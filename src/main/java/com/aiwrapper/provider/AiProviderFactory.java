package com.aiwrapper.provider;

import com.aiwrapper.config.AiConfig;
import org.springframework.stereotype.Component;

@Component
public class AiProviderFactory {
    private final AiProvider ollamaProvider;
    private final AiProvider geminiProvider;
    private final AiProvider googleTranslateProvider;
    private final AiProvider openApiProvider;
    private final AiConfig aiConfig;

    public AiProviderFactory(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.ollamaProvider = new OllamaProvider(aiConfig.getOllama());
        this.geminiProvider = new GeminiProvider(aiConfig.getGemini());
        this.googleTranslateProvider = new GoogleTranslateProvider(aiConfig.getGoogletranslate());
        this.openApiProvider = new OpenApiProvider(aiConfig.getOpenapi());
    }

    public AiProvider get() {
        String providerName = aiConfig.getProvider();
        if (providerName == null) {
            throw new IllegalArgumentException(
                    "AI provider is not configured. Specify 'ai.provider' in configuration.");
        }
        switch (providerName.toLowerCase()) {
            case "ollama":
                return ollamaProvider;
            case "gemini":
                return geminiProvider;
            case "googletranslate":
            case "google":
            case "google translate":
                return googleTranslateProvider;
            case "openapi":
            case "openai":
                return openApiProvider;
            default:
                throw new IllegalArgumentException("Unsupported AI provider: " + providerName);
        }
    }

    public TranslationProviderConstraints getConstraints() {
        String providerName = aiConfig.getProvider();
        if (providerName == null || providerName.trim().isEmpty()) {
            return aiConfig.getOllama();
        }
        switch (providerName.toLowerCase()) {
            case "gemini":
                return aiConfig.getGemini();
            case "googletranslate":
            case "google":
            case "google translate":
                return aiConfig.getGoogletranslate();
            case "openapi":
            case "openai":
                return aiConfig.getOpenapi();
            case "ollama":
            default:
                return aiConfig.getOllama();
        }
    }
}
