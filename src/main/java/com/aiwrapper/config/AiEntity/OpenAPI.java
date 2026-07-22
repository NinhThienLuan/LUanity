package com.aiwrapper.config.AiEntity;

import com.aiwrapper.provider.TranslationProviderConstraints;

public class OpenAPI implements TranslationProviderConstraints {
    private String url = "https://api.openai.com/v1";
    private String apiKey;
    private String model = "gpt-4o-mini";

    private int maxChars = 20000;
    private int maxItems = 30;
    private int rpm = 60;
    private long minDelayMs = 500;
    private boolean supportsBatchNative = false;

    @Override
    public int maxCharsPerRequest() {
        return maxChars;
    }

    @Override
    public int maxItemsPerBatch() {
        return maxItems;
    }

    @Override
    public int requestsPerMinute() {
        return rpm;
    }

    @Override
    public long minDelayBetweenRequestsMs() {
        return minDelayMs;
    }

    @Override
    public boolean supportsBatchNative() {
        return supportsBatchNative;
    }

    public int getMaxChars() {
        return maxChars;
    }

    public void setMaxChars(int maxChars) {
        this.maxChars = maxChars;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public int getRpm() {
        return rpm;
    }

    public void setRpm(int rpm) {
        this.rpm = rpm;
    }

    public long getMinDelayMs() {
        return minDelayMs;
    }

    public void setMinDelayMs(long minDelayMs) {
        this.minDelayMs = minDelayMs;
    }

    public boolean isSupportsBatchNative() {
        return supportsBatchNative;
    }

    public void setSupportsBatchNative(boolean supportsBatchNative) {
        this.supportsBatchNative = supportsBatchNative;
    }

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
