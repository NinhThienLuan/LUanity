package com.aiwrapper.config;

import com.aiwrapper.config.AiEntity.Gemini;
import com.aiwrapper.config.AiEntity.GoogleTranslate;
import com.aiwrapper.config.AiEntity.Ollama;
import com.aiwrapper.config.AiEntity.OpenAPI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public class AiConfig {
    private String provider;
    private Ollama ollama = new Ollama();
    private Gemini gemini = new Gemini();
    private GoogleTranslate googletranslate = new GoogleTranslate();
    private OpenAPI openapi = new OpenAPI();

    public Gemini getGemini() {
        return gemini;
    }

    public void setGemini(Gemini gemini) {
        this.gemini = gemini;
    }

    public GoogleTranslate getGoogletranslate() {
        return googletranslate;
    }

    public void setGoogletranslate(GoogleTranslate googletranslate) {
        this.googletranslate = googletranslate;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public void setOllama(Ollama ollama) {
        this.ollama = ollama;
    }

    public OpenAPI getOpenapi() {
        return openapi;
    }

    public void setOpenapi(OpenAPI openapi) {
        this.openapi = openapi;
    }
}
