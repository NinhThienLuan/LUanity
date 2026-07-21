package com.aiwrapper.executor;

import java.util.Map;

public interface BaseExecutor {
    void execute(String inputPath, String outputPath, Map<String, Object> options);
}
