package com.aiwrapper.executor;

import com.aiwrapper.provider.AiProviderFactory;
import com.aiwrapper.provider.RateLimitBackoffHandler;
import com.aiwrapper.provider.TranslationProviderConstraints;
import com.aiwrapper.template.PromptTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

@Component
public class TranslationBatchQueue implements InitializingBean {

    public static class QueueItem {
        public final String originalText;
        public final String preservedText;
        public final Map<String, String> glossary;
        public final TranslateExecutor.TagPreserver preserver;
        public final CompletableFuture<String> future;
        public final Map<String, Object> options;

        public QueueItem(String originalText, String preservedText, Map<String, String> glossary,
                TranslateExecutor.TagPreserver preserver, CompletableFuture<String> future,
                Map<String, Object> options) {
            this.originalText = originalText;
            this.preservedText = preservedText;
            this.glossary = glossary;
            this.preserver = preserver;
            this.future = future;
            this.options = options;
        }
    }

    private final LinkedBlockingDeque<QueueItem> queue = new LinkedBlockingDeque<>();
    private final AiProviderFactory aiFactory;
    private final RateLimitBackoffHandler backoffHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranslationBatchQueue(AiProviderFactory aiFactory, RateLimitBackoffHandler backoffHandler) {
        this.aiFactory = aiFactory;
        this.backoffHandler = backoffHandler;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void init() {
        Thread processorThread = new Thread(this::processQueue, "TranslationBatchQueueProcessor");
        processorThread.setDaemon(true);
        processorThread.start();
    }

    public CompletableFuture<String> queue(String originalText, String preservedText,
            Map<String, String> glossary,
            TranslateExecutor.TagPreserver preserver,
            Map<String, Object> options) {
        CompletableFuture<String> future = new CompletableFuture<>();
        queue.add(new QueueItem(originalText, preservedText, glossary, preserver, future, options));
        return future;
    }

    private void processQueue() {
        while (true) {
            try {
                QueueItem first = queue.take(); // block until item is available
                List<QueueItem> batch = new ArrayList<>();
                batch.add(first);

                TranslationProviderConstraints constraints = aiFactory.getConstraints();
                int maxItems = constraints.maxItemsPerBatch();
                int maxChars = constraints.maxCharsPerRequest();
                long maxWaitMs = 60000 / Math.max(1, constraints.requestsPerMinute());
                maxWaitMs = Math.min(2000, Math.max(200, maxWaitMs)); // range [200ms, 2000ms]

                long startTime = System.currentTimeMillis();
                int totalChars = first.preservedText.length();

                while (batch.size() < maxItems) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    long remaining = maxWaitMs - elapsed;
                    if (remaining <= 0)
                        break;

                    QueueItem next = queue.peek();
                    if (next == null) {
                        next = queue.poll(remaining, TimeUnit.MILLISECONDS);
                        if (next == null) {
                            break; // timed out
                        }
                    } else {
                        next = queue.poll();
                    }

                    if (next == null) {
                        continue;
                    }

                    if (totalChars + next.preservedText.length() > maxChars) {
                        queue.putFirst(next);
                        break;
                    }

                    batch.add(next);
                    totalChars += next.preservedText.length();
                }

                if (!batch.isEmpty()) {
                    executeBatch(batch, constraints);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void executeBatch(List<QueueItem> batch, TranslationProviderConstraints constraints) {
        String time = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(java.time.LocalTime.now());
        System.out.println(time + "  INFO --- [proxy] : Sending translation batch of " + batch.size() + " items using "
                + constraints.getClass().getSimpleName() + "...");
        for (int i = 0; i < batch.size(); i++) {
            System.out.println("                   - [" + (i + 1) + "] " + batch.get(i).preservedText);
        }
        try {
            backoffHandler.executeWithRetry(provider -> {
                if (constraints.supportsBatchNative()) {
                    // Google Translate (Native batching)
                    List<String> textsToTranslate = new ArrayList<>();
                    for (QueueItem item : batch) {
                        textsToTranslate.add(item.preservedText);
                    }
                    String payload = objectMapper.writeValueAsString(textsToTranslate);

                    Map<String, Object> firstOptions = batch.get(0).options != null ? batch.get(0).options : Map.of();
                    String responsePayload = provider.complete(payload, firstOptions);

                    List<String> results = objectMapper.readValue(responsePayload, new TypeReference<List<String>>() {
                    });
                    if (results.size() == batch.size()) {
                        String doneTime = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                                .format(java.time.LocalTime.now());
                        System.out.println(doneTime + "  INFO --- [proxy] : Batch completed successfully ("
                                + batch.size() + " items).");
                        for (int i = 0; i < batch.size(); i++) {
                            batch.get(i).future.complete(results.get(i));
                        }
                    } else {
                        throw new RuntimeException("Native batch response size mismatch. Expected "
                                + batch.size() + " but got " + results.size());
                    }
                    return "SUCCESS";
                } else {
                    // Prompt-based batching (Gemini, Ollama, OpenAI)
                    StringBuilder promptBuilder = new StringBuilder();
                    for (int i = 0; i < batch.size(); i++) {
                        promptBuilder.append(batch.get(i).preservedText).append(" === \n");
                    }

                    Map<String, String> mergedGlossary = new LinkedHashMap<>();
                    for (QueueItem item : batch) {
                        if (item.glossary != null) {
                            mergedGlossary.putAll(item.glossary);
                        }
                    }

                    String promptText = promptBuilder.toString();
                    if (!mergedGlossary.isEmpty()) {
                        StringBuilder glossaryPrompt = new StringBuilder();
                        glossaryPrompt.append(promptText);
                        glossaryPrompt.append("\n\nYêu cầu dịch các thuật ngữ sau chính xác như mô tả:\n");
                        for (Map.Entry<String, String> entry : mergedGlossary.entrySet()) {
                            glossaryPrompt.append("- \"").append(entry.getKey()).append("\" -> \"")
                                    .append(entry.getValue()).append("\"\n");
                        }
                        promptText = glossaryPrompt.toString();
                    }

                    String promptTemplateString = "Translate the following list into Vietnamese. Return ONLY the translations in the exact format: Original Text === Translation. Do not modify the original text on the left hand side of the '==='. Only reply with the translated list:\n\n{text}";
                    PromptTemplate activeTemplate = new PromptTemplate(promptTemplateString);

                    Map<String, Object> firstOptions = batch.get(0).options != null ? batch.get(0).options : Map.of();
                    String prompt = activeTemplate.render(Map.of("text", promptText));

                    String rawResponse = provider.complete(prompt, firstOptions);

                    Map<String, List<String>> resultsMap = new HashMap<>();
                    String[] lines = rawResponse.split("\\r?\\n");
                    for (String line : lines) {
                        if (line.contains("===")) {
                            int idx = line.indexOf("===");
                            String origPart = line.substring(0, idx).trim();
                            String transPart = line.substring(idx + 3).trim();
                            if (origPart.startsWith("\"") && origPart.endsWith("\"") && origPart.length() >= 2) {
                                origPart = origPart.substring(1, origPart.length() - 1);
                            }
                            if (transPart.startsWith("\"") && transPart.endsWith("\"") && transPart.length() >= 2) {
                                transPart = transPart.substring(1, transPart.length() - 1);
                            }
                            String normKey = normalizeKey(origPart);
                            resultsMap.computeIfAbsent(normKey, k -> new ArrayList<>()).add(transPart.trim());
                        }
                    }

                    Map<String, Integer> usageMap = new HashMap<>();
                    List<QueueItem> failedItems = new ArrayList<>();

                    for (QueueItem item : batch) {
                        String normKey = normalizeKey(item.preservedText);
                        List<String> transList = resultsMap.get(normKey);
                        int useIdx = usageMap.getOrDefault(normKey, 0);
                        if (transList != null && useIdx < transList.size()) {
                            String translatedVal = transList.get(useIdx);
                            item.future.complete(translatedVal);
                            usageMap.put(normKey, useIdx + 1);
                        } else {
                            failedItems.add(item);
                        }
                    }

                    if (failedItems.size() < batch.size()) {
                        String doneTime = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                                .format(java.time.LocalTime.now());
                        System.out.println(doneTime + "  INFO --- [proxy] : Batch completed successfully ("
                                + (batch.size() - failedItems.size()) + "/" + batch.size() + " items).");
                    }

                    if (!failedItems.isEmpty()) {
                        String doneTime = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                                .format(java.time.LocalTime.now());
                        System.out.println(doneTime + "  WARN --- [proxy] : " + failedItems.size()
                                + " items in batch failed alignment. Retrying individually...");
                        fallbackTranslateIndividually(failedItems);
                    }
                    return "SUCCESS";
                }
            });
        } catch (Exception e) {
            for (QueueItem item : batch) {
                item.future.completeExceptionally(e);
            }
        }
    }

    private String normalizeKey(String key) {
        if (key == null)
            return "";
        String clean = key.replaceAll("[\\uFEFF\\u200B\\u200C\\u200D\\u200E\\u200F\\u2070-\\u209F]", "");
        return clean.trim().toLowerCase();
    }

    private void fallbackTranslateIndividually(List<QueueItem> batch) {
        for (QueueItem item : batch) {
            try {
                String singlePromptText = item.preservedText;
                if (item.glossary != null && !item.glossary.isEmpty()) {
                    StringBuilder glossaryPrompt = new StringBuilder(item.preservedText);
                    glossaryPrompt.append("\n\nYêu cầu dịch các thuật ngữ sau chính xác như mô tả:\n");
                    for (Map.Entry<String, String> entry : item.glossary.entrySet()) {
                        glossaryPrompt.append("- \"").append(entry.getKey()).append("\" -> \"").append(entry.getValue())
                                .append("\"\n");
                    }
                    singlePromptText = glossaryPrompt.toString();
                }
                PromptTemplate activeTemplate = new PromptTemplate(
                        "Translate this game text into Vietnamese. Only return translation:\n\n{text}");
                String promptText = activeTemplate.render(Map.of("text", singlePromptText));

                backoffHandler.executeWithRetry(provider -> {
                    String raw = provider.complete(promptText, item.options != null ? item.options : Map.of());
                    item.future.complete(raw);
                    return "SUCCESS";
                });
            } catch (Exception ex) {
                item.future.completeExceptionally(ex);
            }
        }
    }
}
