package com.aiwrapper.config.AiEntity;

import com.aiwrapper.provider.TranslationProviderConstraints;

public class Gemini implements TranslationProviderConstraints {
    private String apiKey;
    private String model;

    private int maxChars = 5000;
    private int maxItems = 25;
    private int rpm = 15;
    private long minDelayMs = 4000;
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
