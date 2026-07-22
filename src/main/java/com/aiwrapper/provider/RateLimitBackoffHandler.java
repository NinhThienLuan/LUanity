package com.aiwrapper.provider;

import com.aiwrapper.config.AiConfig;
import org.springframework.stereotype.Component;

@Component
public class RateLimitBackoffHandler {
    private final AiConfig aiConfig;
    private final AiProviderFactory aiFactory;

    public RateLimitBackoffHandler(AiConfig aiConfig, AiProviderFactory aiFactory) {
        this.aiConfig = aiConfig;
        this.aiFactory = aiFactory;
    }

    public interface BatchTranslationExecutor {
        String execute(AiProvider provider) throws Exception;
    }

    public String executeWithRetry(BatchTranslationExecutor executor) throws Exception {
        int providerSwitches = 0;
        int maxProviderSwitches = 4;

        while (providerSwitches <= maxProviderSwitches) {
            AiProvider provider = aiFactory.get();
            int maxAttempts = 3;
            long baseDelayMs = 2000;
            long maxDelayMs = 15000;

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                try {
                    return executor.execute(provider);
                } catch (Throwable t) {
                    if (isRateLimitError(t)) {
                        if (attempt == maxAttempts - 1) {
                            System.err.println("Rate limit reached for provider " + aiConfig.getProvider()
                                    + " after " + maxAttempts + " attempts. Switching provider...");
                            break;
                        }
                        long delay = baseDelayMs * (long) Math.pow(2, attempt);
                        long jitter = (long) (Math.random() * 1000);
                        long finalDelay = Math.min(maxDelayMs, delay + jitter);
                        System.out.println("Rate limit error for " + aiConfig.getProvider()
                                + ". Retrying attempt " + (attempt + 2) + " after " + finalDelay + "ms. Error: "
                                + t.getMessage());
                        Thread.sleep(finalDelay);
                    } else {
                        throw new Exception(t);
                    }
                }
            }

            // Switch to next fallback provider
            triggerProviderFallback();
            providerSwitches++;
        }
        throw new RuntimeException("All fallback providers rate-limited or exhausted.");
    }

    private void triggerProviderFallback() {
        String current = aiConfig.getProvider();
        if (current == null)
            current = "ollama";
        String next;
        switch (current.toLowerCase()) {
            case "ollama":
                next = "gemini";
                break;
            case "gemini":
                next = "googletranslate";
                break;
            case "googletranslate":
            case "google":
                next = "openapi";
                break;
            case "openapi":
            case "openai":
            default:
                next = "ollama";
                break;
        }
        aiConfig.setProvider(next);
        System.out.println("Cycle provider fallback: " + current + " -> " + next);
    }

    private boolean isRateLimitError(Throwable t) {
        if (t == null)
            return false;
        String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
        if (msg.contains("429") || msg.contains("too many requests") ||
                msg.contains("rate limit") || msg.contains("quota exceeded") ||
                msg.contains("resource exhausted") || msg.contains("exhausted") ||
                (msg.contains("403") && msg.contains("quota"))) {
            return true;
        }
        if (t.getCause() != null) {
            return isRateLimitError(t.getCause());
        }
        return false;
    }
}
