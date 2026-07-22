package com.aiwrapper.config.AiEntity;

import com.aiwrapper.provider.TranslationProviderConstraints;

public class Ollama implements TranslationProviderConstraints {
    private String url;
    private String model;
    private Double temperature;

    private int maxChars = 100000;
    private int maxItems = 50;
    private int rpm = 120;
    private long minDelayMs = 100;
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
}
