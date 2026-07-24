package com.aiwrapper.config;

public class AppConfig {
    private String activeGamePath = "";
    private String geminiApiKey = "";
    private String openaiApiKey = "";
    private String languagePair = "EN/VI";
    private String provider = "ollama";

    public String getActiveGamePath() {
        return activeGamePath;
    }

    public void setActiveGamePath(String activeGamePath) {
        this.activeGamePath = activeGamePath;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public String getLanguagePair() {
        return languagePair;
    }

    public void setLanguagePair(String languagePair) {
        this.languagePair = languagePair;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
