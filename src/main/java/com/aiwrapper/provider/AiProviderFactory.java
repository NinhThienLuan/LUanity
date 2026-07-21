package com.aiwrapper.provider;

import com.aiwrapper.config.AiConfig;
import org.springframework.stereotype.Component;

@Component
public class AiProviderFactory {
    private final AiProvider ollamaProvider;
    private final AiProvider openAiProvider;
    private final AiProvider geminiProvider;
    private final AiProvider googleTranslateProvider;
    private final AiConfig aiConfig;

    public AiProviderFactory(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.ollamaProvider = new OllamaProvider(aiConfig.getOllama());
        this.openAiProvider = new OpenAiProvider(aiConfig.getOpenai());
        this.geminiProvider = new GeminiProvider(aiConfig.getGemini());
        this.googleTranslateProvider = new GoogleTranslateProvider(aiConfig.getGoogletranslate());
    }

    public AiProvider get() {
        String providerName = aiConfig.getProvider();
        if (providerName == null) {
            throw new IllegalArgumentException("AI provider is not configured. Specify 'ai.provider' in configuration.");
        }
        switch (providerName.toLowerCase()) {
            case "ollama":
                return ollamaProvider;
            case "openai":
                return openAiProvider;
            case "gemini":
                return geminiProvider;
            case "googletranslate":
            case "google":
                return googleTranslateProvider;
            default:
                throw new IllegalArgumentException("Unsupported AI provider: " + providerName);
        }
    }
}
