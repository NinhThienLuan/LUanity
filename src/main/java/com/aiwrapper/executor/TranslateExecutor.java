package com.aiwrapper.executor;

import com.aiwrapper.file.FileHandler;
import com.aiwrapper.file.FileHandlerFactory;
import com.aiwrapper.provider.AiProvider;
import com.aiwrapper.provider.AiProviderFactory;
import com.aiwrapper.template.PromptTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TranslateExecutor implements BaseExecutor {

    public interface TranslationListener {
        void onTranslation(String original, String translated, String type, int characterCount);
    }

    private TranslationListener listener;
    private File activeCacheFile = null;

    public void setTranslationListener(TranslationListener listener) {
        this.listener = listener;
    }

    public File getActiveCacheFile() {
        return activeCacheFile;
    }

    public void setActiveCacheFile(File activeCacheFile) {
        this.activeCacheFile = activeCacheFile;
    }

    private final AiProviderFactory aiFactory;

    @org.springframework.beans.factory.annotation.Autowired
    private TranslationBatchQueue batchQueue;
    private String promptTemplateString = "Translate the following English game text into Vietnamese according to these rules:\n"
            +
            "1. For single words and phrases: translate literally and precisely (dịch sát nghĩa).\n" +
            "2. For full sentences: translate naturally to suit the game context (dịch phù hợp với ngữ cảnh).\n" +
            "3. Do not explain, do not write anything else, only return the Vietnamese translation.\n" +
            "4. Preserve placeholders like [[TAG_N]] exactly.\n" +
            "5. Absolutely do not output any conversational filler or unrelated details.\n\n" +
            "English: \"Continue\"\n" +
            "Vietnamese: \"Tiếp tục\"\n\n" +
            "English: \"Settings\"\n" +
            "Vietnamese: \"Cài đặt\"\n\n" +
            "English: \"{text}\"\n" +
            "Vietnamese: \"";

    public TranslateExecutor(AiProviderFactory aiFactory) {
        this.aiFactory = aiFactory;
        loadPromptTemplate();
    }

    public java.util.concurrent.CompletableFuture<String> translateSingleAsync(String text,
            Map<String, Object> options) {
        if (!proxyActive) {
            return java.util.concurrent.CompletableFuture.completedFuture(text);
        }
        if (text == null || text.trim().isEmpty()) {
            return java.util.concurrent.CompletableFuture.completedFuture(text);
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> cacheMap = new java.util.HashMap<>();
        File activeCache = activeCacheFile;
        File gameCacheFile = null;
        if (activeCache != null && activeCache.exists()) {
            try {
                Map<String, String> loadedActiveCache = mapper.readValue(activeCache,
                        new TypeReference<Map<String, String>>() {
                        });
                cacheMap.putAll(loadedActiveCache);
                gameCacheFile = activeCache;
            } catch (Exception e) {
                // Ignore
            }
        } else {
            String gameName = getGameName();
            if (gameName != null && !gameName.isEmpty()) {
                gameCacheFile = new File("data/cache_" + gameName + ".json");
                if (gameCacheFile.exists()) {
                    try {
                        Map<String, String> gameCache = mapper.readValue(gameCacheFile,
                                new TypeReference<Map<String, String>>() {
                                });
                        cacheMap.putAll(gameCache);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            } else {
                gameCacheFile = new File("data/cache.json");
            }
        }

        if (gameCacheFile == null) {
            String gameName = getGameName();
            if (gameName != null && !gameName.isEmpty()) {
                gameCacheFile = new File("data/cache_" + gameName + ".json");
            } else {
                gameCacheFile = new File("data/cache.json");
            }
        }

        cacheMap.entrySet().removeIf(entry -> isRefusalOrJunk(entry.getValue()));

        if (cacheMap.containsKey(text)) {
            String cached = cacheMap.get(text);
            if (listener != null) {
                listener.onTranslation(text, cached, "Cache", text.length());
            }
            return java.util.concurrent.CompletableFuture.completedFuture(cached);
        }

        File glossaryFile = new File("data/glossary.json");
        Map<String, String> glossaryMap = new java.util.HashMap<>();
        if (glossaryFile.exists()) {
            try {
                glossaryMap = mapper.readValue(glossaryFile, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                // Ignore
            }
        }

        Map<String, String> matchedGlossary = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : glossaryMap.entrySet()) {
            String key = entry.getKey();
            if (text.toLowerCase().contains(key.toLowerCase())) {
                matchedGlossary.put(key, entry.getValue());
            }
        }

        TagPreserver preserver = new TagPreserver();
        String preservedText = preserver.preserve(text);

        final File finalGameCacheFile = gameCacheFile;
        final Map<String, String> finalCacheMap = cacheMap;

        return batchQueue.queue(text, preservedText, matchedGlossary, preserver, options)
                .thenApply(translatedValue -> {
                    String cleaned = cleanRawTranslation(text, translatedValue);
                    if (isRefusalOrJunk(cleaned)) {
                        if (listener != null) {
                            listener.onTranslation(text, text, "AI_Refused", text.length());
                        }
                        return text;
                    }
                    String restored = preserver.restore(cleaned);

                    finalCacheMap.put(text, restored);
                    if (finalGameCacheFile != null) {
                        try {
                            File parent = finalGameCacheFile.getParentFile();
                            if (parent != null && !parent.exists()) {
                                parent.mkdirs();
                            }
                            mapper.writerWithDefaultPrettyPrinter().writeValue(finalGameCacheFile, finalCacheMap);
                        } catch (Exception ex) {
                            // Ignore
                        }
                    }

                    if (listener != null) {
                        listener.onTranslation(text, restored, "AI", text.length());
                    }
                    return restored;
                }).exceptionally(ex -> {
                    if (listener != null) {
                        listener.onTranslation(text, "[ERROR] " + ex.getMessage(), "AI_Error", text.length());
                    }
                    return "[ERROR] " + ex.getMessage();
                });
    }

    private void loadPromptTemplate() {
        java.io.File file = new java.io.File("data/prompt_template.txt");
        if (file.exists()) {
            try {
                this.promptTemplateString = new String(java.nio.file.Files.readAllBytes(file.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public String getPromptTemplate() {
        return promptTemplateString;
    }

    public void setPromptTemplate(String promptTemplateString) {
        this.promptTemplateString = promptTemplateString;
        try {
            java.io.File file = new java.io.File("data/prompt_template.txt");
            java.io.File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            java.nio.file.Files.write(file.toPath(),
                    promptTemplateString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Ignore
        }
    }

    private boolean proxyActive = true;

    public boolean isProxyActive() {
        return proxyActive;
    }

    public void setProxyActive(boolean proxyActive) {
        this.proxyActive = proxyActive;
    }

    public static class TagPreserver {
        private final List<String> originalTags = new ArrayList<>();

        public String preserve(String text) {
            if (text == null)
                return null;
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(<[^>]+>|\\{[^}]+\\}|\\b\\d+(?:\\.\\d+)?\\s*(?i:km/h|km|kg|lbs|ghz|mhz|hz|mph|fps|sec|ml|oz|px|pt|in|ft|yd|g|l|s|m)\\b|(?i)\\b[xX]\\s*\\d+\\b|\\b\\d+\\s*[xX]\\b)");

            // Bypass preservation if the entire text is just a placeholder/unit itself
            if (pattern.matcher(text).matches()) {
                return text;
            }

            java.util.regex.Matcher matcher = pattern.matcher(text);
            StringBuilder sb = new StringBuilder();
            int index = 0;
            while (matcher.find()) {
                originalTags.add(matcher.group());
                matcher.appendReplacement(sb, "[[TAG_" + index + "]]");
                index++;
            }
            matcher.appendTail(sb);
            return sb.toString();
        }

        public String restore(String translated) {
            if (translated == null)
                return null;
            // Match tag sequence with flexible brackets, asterisks, spacing, and
            // underscores
            java.util.regex.Pattern pattern = java.util.regex.Pattern
                    .compile("([\\[\\]\\*_]*\\s*[\\[\\]\\*_]*[tT][aA][gG]\\s*_?\\s*(\\d+)[\\s\\[\\]\\*]*)");
            java.util.regex.Matcher matcher = pattern.matcher(translated);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                try {
                    int index = Integer.parseInt(matcher.group(2));
                    if (index >= 0 && index < originalTags.size()) {
                        matcher.appendReplacement(sb,
                                java.util.regex.Matcher.quoteReplacement(originalTags.get(index)));
                    } else {
                        matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(matcher.group()));
                    }
                } catch (NumberFormatException e) {
                    matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(matcher.group()));
                }
            }
            matcher.appendTail(sb);
            return sb.toString();
        }
    }

    @Override
    public void execute(String inputPath, String outputPath, Map<String, Object> options) {
        FileHandler inHandler = FileHandlerFactory.get(inputPath);
        FileHandler outHandler = FileHandlerFactory.get(outputPath);
        AiProvider ai = aiFactory.get();

        List<Map<String, Object>> rows = inHandler.read(inputPath);
        List<Map<String, Object>> results = new ArrayList<>();

        // Load Hybrid Cache (global cache fallback + game-specific override)
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> cacheMap = new java.util.HashMap<>();

        File activeCache = activeCacheFile;
        File gameCacheFile = null;
        if (activeCache != null && activeCache.exists()) {
            try {
                Map<String, String> loadedActiveCache = mapper.readValue(activeCache,
                        new TypeReference<Map<String, String>>() {
                        });
                cacheMap.putAll(loadedActiveCache);
                gameCacheFile = activeCache;
            } catch (Exception e) {
                System.err.println("Warning: Could not read active cache: " + e.getMessage());
            }
        } else {
            String gameName = getGameName();
            if (gameName != null && !gameName.isEmpty()) {
                gameCacheFile = new File("data/cache_" + gameName + ".json");
                if (gameCacheFile.exists()) {
                    try {
                        Map<String, String> gameCache = mapper.readValue(gameCacheFile,
                                new TypeReference<Map<String, String>>() {
                                });
                        cacheMap.putAll(gameCache);
                    } catch (Exception e) {
                        System.err.println("Warning: Could not read game translation cache: " + e.getMessage());
                    }
                }
            }
        }

        // Load Glossary
        File glossaryFile = new File("data/glossary.json");
        Map<String, String> glossaryMap = new java.util.HashMap<>();
        if (glossaryFile.exists()) {
            try {
                glossaryMap = mapper.readValue(glossaryFile, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                System.err.println("Warning: Could not read glossary: " + e.getMessage());
            }
        }

        // Get template options override
        PromptTemplate activeTemplate = new PromptTemplate(
                promptTemplateString);
        if (options != null && options.containsKey("promptTemplate")) {
            String customT = String.valueOf(options.get("promptTemplate"));
            if (customT != null && !customT.trim().isEmpty()) {
                activeTemplate = new PromptTemplate(customT);
            }
        }

        for (Map<String, Object> row : rows) {
            Object textObj = row.get("text");
            if (textObj == null && !row.isEmpty()) {
                // Defensive fallback to first value if "text" key is absent
                textObj = row.values().iterator().next();
            }
            String text = textObj != null ? String.valueOf(textObj) : "";

            String translated;
            if (cacheMap.containsKey(text)) {
                translated = cacheMap.get(text);
                System.out.println("Cache hit for: " + text + " -> " + translated);
                if (listener != null) {
                    listener.onTranslation(text, translated, "Cache", text.length());
                }
            } else {
                TagPreserver preserver = new TagPreserver();
                String preservedText = preserver.preserve(text);

                // Find matched glossary entries
                Map<String, String> matchedGlossary = new java.util.HashMap<>();
                if (text != null) {
                    for (Map.Entry<String, String> entry : glossaryMap.entrySet()) {
                        String key = entry.getKey();
                        if (text.toLowerCase().contains(key.toLowerCase())) {
                            matchedGlossary.put(key, entry.getValue());
                        }
                    }
                }

                String promptText = preservedText;
                if (!matchedGlossary.isEmpty()) {
                    StringBuilder glossaryPrompt = new StringBuilder(preservedText);
                    glossaryPrompt.append("\n\nYêu cầu dịch các thuật ngữ sau chính xác như mô tả:\n");
                    for (Map.Entry<String, String> entry : matchedGlossary.entrySet()) {
                        glossaryPrompt.append("- \"").append(entry.getKey()).append("\" -> \"").append(entry.getValue())
                                .append("\"\n");
                    }
                    promptText = glossaryPrompt.toString();
                }

                String prompt = activeTemplate.render(Map.of("text", promptText));

                try {
                    String rawTranslation = ai.complete(prompt, options);
                    String cleanedRaw = cleanRawTranslation(text, rawTranslation);

                    if (isRefusalOrJunk(cleanedRaw)) {
                        String simplePrompt = "Translate this English text to Vietnamese (only return the translation, no explanation): "
                                + text;
                        try {
                            String retryRaw = ai.complete(simplePrompt, options);
                            String retryCleaned = cleanRawTranslation(text, retryRaw);
                            if (!isRefusalOrJunk(retryCleaned)) {
                                cleanedRaw = retryCleaned;
                            } else {
                                cleanedRaw = text; // fallback to original
                            }
                        } catch (Exception ex) {
                            cleanedRaw = text;
                        }
                    }

                    translated = preserver.restore(cleanedRaw);

                    // Apply glossary fallback replacements
                    for (Map.Entry<String, String> entry : matchedGlossary.entrySet()) {
                        String key = entry.getKey();
                        String val = entry.getValue();
                        translated = translated.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(key) + "\\b", val);
                    }

                    cacheMap.put(text, translated);
                    if (listener != null) {
                        String type = matchedGlossary.isEmpty() ? "AI" : "Glossary";
                        listener.onTranslation(text, translated, type, text.length());
                    }
                } catch (Exception e) {
                    translated = "[ERROR] " + e.getMessage();
                }
            }

            Map<String, Object> resultRow = new LinkedHashMap<>(row);
            resultRow.put("translated", translated);
            results.add(resultRow);
        }

        // Save Hybrid Cache
        try {
            if (gameCacheFile != null) {
                Map<String, String> gameCacheMap = new java.util.HashMap<>();
                if (gameCacheFile.exists()) {
                    try {
                        gameCacheMap = mapper.readValue(gameCacheFile, new TypeReference<Map<String, String>>() {
                        });
                    } catch (Exception e) {
                    }
                }
                for (Map<String, Object> resultRow : results) {
                    Object textObj = resultRow.get("text");
                    String text = textObj != null ? String.valueOf(textObj) : "";
                    String translated = String.valueOf(resultRow.get("translated"));
                    if (!text.isEmpty()) {
                        gameCacheMap.put(text, translated);
                    }
                }
                File parentDir = gameCacheFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                mapper.writerWithDefaultPrettyPrinter().writeValue(gameCacheFile, gameCacheMap);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not write translation cache: " + e.getMessage());
        }

        outHandler.write(outputPath, results);
    }

    public String translateSingle(String text, Map<String, Object> options) {
        if (!proxyActive) {
            return text;
        }
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        if (shouldBypassTranslation(text)) {
            return text;
        }

        // Load Hybrid Cache (global cache fallback + game-specific override)
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> cacheMap = new java.util.HashMap<>();

        File activeCache = activeCacheFile;
        File gameCacheFile = null;
        if (activeCache != null && activeCache.exists()) {
            try {
                Map<String, String> loadedActiveCache = mapper.readValue(activeCache,
                        new TypeReference<Map<String, String>>() {
                        });
                cacheMap.putAll(loadedActiveCache);
                gameCacheFile = activeCache;
            } catch (Exception e) {
                // Ignore
            }
        } else {
            String gameName = getGameName();
            if (gameName != null && !gameName.isEmpty()) {
                gameCacheFile = new File("data/cache_" + gameName + ".json");
                if (gameCacheFile.exists()) {
                    try {
                        Map<String, String> gameCache = mapper.readValue(gameCacheFile,
                                new TypeReference<Map<String, String>>() {
                                });
                        cacheMap.putAll(gameCache);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }

        // Clean loaded caches: remove any junk entries
        cacheMap.entrySet().removeIf(entry ->

        isRefusalOrJunk(entry.getValue()));

        if (cacheMap.containsKey(text)) {
            String cached = cacheMap.get(text);
            boolean skipCacheWrite = (options != null && options.containsKey("skipCacheWrite")
                    && (boolean) options.get("skipCacheWrite"));
            if (listener != null && !skipCacheWrite) {
                listener.onTranslation(text, cached, "Cache", text.length());
            }
            return cached;
        }

        // Load Glossary
        File glossaryFile = new File("data/glossary.json");
        Map<String, String> glossaryMap = new java.util.HashMap<>();
        if (glossaryFile.exists()) {
            try {
                glossaryMap = mapper.readValue(glossaryFile, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                // Ignore
            }
        }

        // Find matched glossary entries
        Map<String, String> matchedGlossary = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : glossaryMap.entrySet()) {
            String key = entry.getKey();
            if (text.toLowerCase().contains(key.toLowerCase())) {
                matchedGlossary.put(key, entry.getValue());
            }
        }

        TagPreserver preserver = new TagPreserver();
        String preservedText = preserver.preserve(text);

        String promptText = preservedText;
        if (!matchedGlossary.isEmpty()) {
            StringBuilder glossaryPrompt = new StringBuilder(preservedText);
            glossaryPrompt.append("\n\nYêu cầu dịch các thuật ngữ sau chính xác như mô tả:\n");
            for (Map.Entry<String, String> entry : matchedGlossary.entrySet()) {
                glossaryPrompt.append("- \"").append(entry.getKey()).append("\" -> \"").append(entry.getValue())
                        .append("\"\n");
            }
            promptText = glossaryPrompt.toString();
        }

        PromptTemplate activeTemplate = new PromptTemplate(promptTemplateString);
        if (options != null && options.containsKey("promptTemplate")) {
            String customT = String.valueOf(options.get("promptTemplate"));
            if (customT != null && !customT.trim().isEmpty()) {
                activeTemplate = new PromptTemplate(customT);
            }
        }

        String prompt = activeTemplate.render(Map.of("text", promptText));
        AiProvider ai = aiFactory.get();

        String translated;
        try {
            String rawTranslation = ai.complete(prompt, options != null ? options : Map.of());
            String cleanedRaw = cleanRawTranslation(text, rawTranslation);

            if (isRefusalOrJunk(cleanedRaw)) {
                String simplePrompt = "Translate this English text to Vietnamese (only return the translation, no explanation): "
                        + text;
                try {
                    String retryRaw = ai.complete(simplePrompt, options != null ? options : Map.of());
                    String retryCleaned = cleanRawTranslation(text, retryRaw);
                    if (!isRefusalOrJunk(retryCleaned)) {
                        cleanedRaw = retryCleaned;
                    } else {
                        if (listener != null) {
                            listener.onTranslation(text, text, "AI_Refused", text.length());
                        }
                        return text; // Do not save or cache refusal
                    }
                } catch (Exception ex) {
                    if (listener != null) {
                        listener.onTranslation(text, text, "AI_Refused", text.length());
                    }
                    return text;
                }
            }

            translated = preserver.restore(cleanedRaw);

            // Apply glossary fallback replacements
            for (Map.Entry<String, String> entry : matchedGlossary.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                translated = translated.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(key) + "\\b", val);
            }

            boolean skipCacheWrite = (options != null && options.containsKey("skipCacheWrite")
                    && (boolean) options.get("skipCacheWrite"));

            // Save Hybrid Cache
            try {
                if (gameCacheFile != null && !skipCacheWrite) {
                    Map<String, String> gameCacheMap = new java.util.HashMap<>();
                    if (gameCacheFile.exists()) {
                        try {
                            gameCacheMap = mapper.readValue(gameCacheFile, new TypeReference<Map<String, String>>() {
                            });
                        } catch (Exception e) {
                        }
                    }
                    gameCacheMap.put(text, translated);
                    File parentDir = gameCacheFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    mapper.writerWithDefaultPrettyPrinter().writeValue(gameCacheFile, gameCacheMap);
                }
            } catch (Exception e) {
                // Ignore
            }

            if (listener != null && !skipCacheWrite) {
                boolean exactGlossaryMatch = false;
                for (String key : matchedGlossary.keySet()) {
                    if (key.equalsIgnoreCase(text)) {
                        exactGlossaryMatch = true;
                        break;
                    }
                }
                String type = exactGlossaryMatch ? "Glossary" : "AI";
                listener.onTranslation(text, translated, type, text.length());
            }

        } catch (Exception e) {
            translated = "[ERROR] " + e.getMessage();
        }

        return translated;
    }

    private boolean shouldBypassTranslation(String text) {
        if (text == null)
            return false;
        String trimmed = text.trim();
        return trimmed.matches(
                "(?i)^[0-9\\s.,/:\\-+%*#()\\[\\]_xX=\"'<>!?;]*((kg|g|l|s|m|h|M|xp|exp|lv|lvl|v|hz|fps|ping|ms|am|pm|sec|min|hr|d|km|km/h|lbs|mph|ml|oz|px|pt|in|ft|yd)\\b[0-9\\s.,/:\\-+%*#()\\[\\]_xX=\"'<>!?;]*)*$");
    }

    private String getGameName() {
        File pathFile = new File("data/game_path.txt");
        if (pathFile.exists()) {
            try {
                String gamePath = new String(java.nio.file.Files.readAllBytes(pathFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!gamePath.isEmpty()) {
                    File exeFile = new File(gamePath);
                    String gameName = exeFile.getName();
                    if (gameName.endsWith(".exe")) {
                        gameName = gameName.substring(0, gameName.length() - 4);
                    }
                    return gameName;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return null;
    }

    private String cleanRawTranslation(String original, String raw) {
        if (raw == null)
            return null;
        String clean = raw.trim();

        String[] prefixes = {
                "vietnamese:", "tiếng việt:", "tieng viet:", "dịch:", "dich:",
                "translation:", "vietnamese translation:", "bản dịch:"
        };
        for (String prefix : prefixes) {
            if (clean.toLowerCase().startsWith(prefix)) {
                clean = clean.substring(prefix.length()).trim();
            }
        }

        if (original != null && !original.contains("\n")) {
            String[] lines = clean.split("\\r?\\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty())
                    continue;

                boolean isJunk = false;
                for (String junkWord : new String[] { "note:", "please note:", "explanation:", "english:",
                        "vietnamese:", "tiếng việt:", "tiếng anh:", "tieng anh:" }) {
                    if (trimmedLine.toLowerCase().startsWith(junkWord)) {
                        isJunk = true;
                        break;
                    }
                }
                if (trimmedLine.startsWith("**")
                        && (trimmedLine.toLowerCase().contains("note") || trimmedLine.toLowerCase().contains("lưu ý")
                                || trimmedLine.toLowerCase().contains("chú ý"))) {
                    isJunk = true;
                }

                if (!isJunk) {
                    for (String prefix : prefixes) {
                        if (trimmedLine.toLowerCase().startsWith(prefix)) {
                            trimmedLine = trimmedLine.substring(prefix.length()).trim();
                        }
                    }
                    clean = trimmedLine;
                    break;
                }
            }
        }

        if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() > 1) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        if (clean.startsWith("'") && clean.endsWith("'") && clean.length() > 1) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }

        return clean;
    }

    boolean isRefusalOrJunk(String raw) {
        if (raw == null)
            return false;
        String lower = raw.toLowerCase();
        return lower.contains("please provide")
                || lower.contains("english game text")
                || lower.contains("unable to translate")
                || lower.contains("translate into vietnamese")
                || lower.contains("cannot translate")
                || lower.contains("i am unable")
                || lower.contains("could you please")
                || lower.contains("sorry, but")
                || lower.contains("unrelated details");
    }

    public void updateCacheValue(String original, String translated) {
        if (original == null || translated == null)
            return;
        ObjectMapper mapper = new ObjectMapper();
        File targetCacheFile = activeCacheFile;
        if (targetCacheFile == null) {
            File globalCacheFile = new File("data/cache.json");
            String gameName = getGameName();
            targetCacheFile = (gameName != null && !gameName.isEmpty())
                    ? new File("data/cache_" + gameName + ".json")
                    : globalCacheFile;
        }
        try {
            Map<String, String> cacheMap = new java.util.HashMap<>();
            if (targetCacheFile.exists()) {
                cacheMap = mapper.readValue(targetCacheFile, new TypeReference<Map<String, String>>() {
                });
            }
            cacheMap.put(original, translated);
            File parentDir = targetCacheFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(targetCacheFile, cacheMap);
            System.out.println("Cache entry updated manually: " + original + " -> " + translated);
        } catch (Exception e) {
            System.err.println("Failed to update manual translation in cache: " + e.getMessage());
        }
    }

    public void updateGlossaryValue(String original, String translated) {
        if (original == null || translated == null)
            return;
        ObjectMapper mapper = new ObjectMapper();
        File glossaryFile = new File("data/glossary.json");
        try {
            Map<String, String> glossaryMap = new java.util.HashMap<>();
            if (glossaryFile.exists()) {
                glossaryMap = mapper.readValue(glossaryFile, new TypeReference<Map<String, String>>() {
                });
            }
            glossaryMap.put(original, translated);
            File parentDir = glossaryFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(glossaryFile, glossaryMap);
            System.out.println("Glossary entry updated: " + original + " -> " + translated);
        } catch (Exception e) {
            System.err.println("Failed to update glossary: " + e.getMessage());
        }
    }

    public void deleteCacheValue(String original) {
        if (original == null)
            return;
        ObjectMapper mapper = new ObjectMapper();
        File targetCacheFile = activeCacheFile;
        if (targetCacheFile == null) {
            String gameName = getGameName();
            targetCacheFile = (gameName != null && !gameName.isEmpty())
                    ? new File("data/cache_" + gameName + ".json")
                    : new File("data/cache_default.json");
        }
        if (!targetCacheFile.exists()) {
            return;
        }
        try {
            Map<String, String> cacheMap = mapper.readValue(targetCacheFile, new TypeReference<Map<String, String>>() {
            });
            if (cacheMap.remove(original) != null) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(targetCacheFile, cacheMap);
                System.out.println("Cache entry deleted: " + original);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete cache entry: " + e.getMessage());
        }
    }

    public void importTranslations(Map<String, String> newTranslations) {
        if (newTranslations == null || newTranslations.isEmpty())
            return;
        ObjectMapper mapper = new ObjectMapper();
        File targetCacheFile = activeCacheFile;
        if (targetCacheFile == null) {
            String gameName = getGameName();
            targetCacheFile = (gameName != null && !gameName.isEmpty())
                    ? new File("data/cache_" + gameName + ".json")
                    : new File("data/cache_default.json");
        }
        try {
            Map<String, String> cacheMap = new java.util.HashMap<>();
            if (targetCacheFile.exists() && targetCacheFile.length() > 0) {
                cacheMap = mapper.readValue(targetCacheFile, new TypeReference<Map<String, String>>() {
                });
            }
            cacheMap.putAll(newTranslations);
            File parentDir = targetCacheFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(targetCacheFile, cacheMap);
            System.out.println("Imported " + newTranslations.size() + " translations to " + targetCacheFile.getPath());
        } catch (Exception e) {
            System.err.println("Failed to import translations: " + e.getMessage());
        }
    }
}
