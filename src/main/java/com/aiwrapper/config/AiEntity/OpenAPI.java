package com.aiwrapper.config.AiEntity;

public class OpenAPI {
    private String url = "https://api.openai.com/v1";
    private String apiKey;
    private String model = "gpt-4o-mini";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
