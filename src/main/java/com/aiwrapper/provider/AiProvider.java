package com.aiwrapper.provider;

import java.util.Map;

public interface AiProvider {
    String complete(String prompt, Map<String, Object> options) throws Exception;
}
