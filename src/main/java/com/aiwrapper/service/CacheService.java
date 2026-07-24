package com.aiwrapper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CacheService {

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, String> loadCacheMap(File cacheFile) throws Exception {
        if (cacheFile == null || !cacheFile.exists()) {
            return new HashMap<>();
        }
        return mapper.readValue(cacheFile, new TypeReference<Map<String, String>>() {
        });
    }

    public void saveCacheMap(File cacheFile, Map<String, String> cache) throws Exception {
        if (cacheFile == null) {
            return;
        }
        File parent = cacheFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile, cache);
    }
}
