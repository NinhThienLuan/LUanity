package com.aiwrapper.provider;

import com.aiwrapper.config.AiConfig;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

public class OllamaProvider implements AiProvider {
    private final AiConfig.Ollama config;
    private final RestTemplate restTemplate;

    public OllamaProvider(AiConfig.Ollama config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String complete(String prompt, Map<String, Object> options) throws Exception {
        String url = config.getUrl();
        if (url == null || url.isEmpty()) {
            url = "http://localhost:11434";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url = url + "/api/generate";

        Map<String, Object> request = new HashMap<>();
        request.put("model", config.getModel() != null ? config.getModel() : "gemma2:2b");
        request.put("prompt", prompt);
        request.put("stream", false);

        if (options != null && !options.isEmpty()) {
            Map<String, Object> ollamaOptions = new HashMap<>();
            if (options.containsKey("temperature")) {
                ollamaOptions.put("temperature", options.get("temperature"));
            }
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                if (!entry.getKey().equals("options") && !entry.getKey().equals("temperature")) {
                    ollamaOptions.put(entry.getKey(), entry.getValue());
                }
            }
            if (options.containsKey("options")) {
                Object rawOptions = options.get("options");
                if (rawOptions instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawOptionsMap = (Map<String, Object>) rawOptions;
                    ollamaOptions.putAll(rawOptionsMap);
                }
            }
            if (!ollamaOptions.isEmpty()) {
                request.put("options", ollamaOptions);
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
        if (response != null && response.containsKey("response")) {
            return String.valueOf(response.get("response"));
        }
        throw new RuntimeException("Invalid response from Ollama: " + response);
    }
}
