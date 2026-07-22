package com.aiwrapper.config.AiEntity;

import com.aiwrapper.provider.TranslationProviderConstraints;

public class GoogleTranslate implements TranslationProviderConstraints {
    private String apiKey;

    private int maxChars = 5000;
    private int maxItems = 25;
    private int rpm = 1000;
    private long minDelayMs = 0;
    private boolean supportsBatchNative = true;

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
}
