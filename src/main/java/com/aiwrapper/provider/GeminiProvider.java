package com.aiwrapper.provider;

import com.aiwrapper.config.AiConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeminiProvider implements AiProvider {
    private final AiConfig.Gemini config;
    private final RestTemplate restTemplate;

    public GeminiProvider(AiConfig.Gemini config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String complete(String prompt, Map<String, Object> options) throws Exception {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini API Key is not configured.");
        }

        String model = config.getModel() != null ? config.getModel() : "gemini-1.5-flash";
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> contentObj = new HashMap<>();
        contentObj.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(contentObj));

        if (options != null && !options.isEmpty()) {
            Map<String, Object> generationConfig = new HashMap<>();
            if (options.containsKey("temperature")) {
                generationConfig.put("temperature", options.get("temperature"));
            }
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                if (!entry.getKey().equals("generationConfig") && !entry.getKey().equals("temperature")) {
                    generationConfig.put(entry.getKey(), entry.getValue());
                }
            }
            if (options.containsKey("generationConfig")) {
                Object rawConfig = options.get("generationConfig");
                if (rawConfig instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawConfigMap = (Map<String, Object>) rawConfig;
                    generationConfig.putAll(rawConfigMap);
                }
            }
            if (!generationConfig.isEmpty()) {
                requestBody.put("generationConfig", generationConfig);
            }
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        if (response != null && response.containsKey("candidates")) {
            List<?> candidates = (List<?>) response.get("candidates");
            if (!candidates.isEmpty()) {
                Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                if (content != null && content.containsKey("parts")) {
                    List<?> parts = (List<?>) content.get("parts");
                    if (!parts.isEmpty()) {
                        Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
                        if (firstPart.containsKey("text")) {
                            return String.valueOf(firstPart.get("text"));
                        }
                    }
                }
            }
        }
        throw new RuntimeException("Invalid response from Gemini: " + response);
    }
}
