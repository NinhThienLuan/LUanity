package com.aiwrapper.provider;

public interface TranslationProviderConstraints {
    int maxCharsPerRequest();

    int maxItemsPerBatch();

    int requestsPerMinute();

    long minDelayBetweenRequestsMs();

    boolean supportsBatchNative();
}
