package com.aiwrapper.provider;

import com.aiwrapper.config.AiEntity.OpenAPI;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenApiProvider implements AiProvider {
    private final OpenAPI config;
    private final RestTemplate restTemplate;

    public OpenApiProvider(OpenAPI config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String complete(String prompt, Map<String, Object> options) throws Exception {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAPI API Key is not configured.");
        }

        String url = config.getUrl();
        if (url == null || url.trim().isEmpty()) {
            url = "https://api.openai.com/v1";
        }
        url = url.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url = url + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey.trim());

        Map<String, Object> messagePart = new HashMap<>();
        messagePart.put("role", "user");
        messagePart.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel() != null && !config.getModel().trim().isEmpty()
                ? config.getModel().trim()
                : "gpt-4o-mini");
        requestBody.put("messages", List.of(messagePart));

        double temperature = 0.2;
        if (options != null && options.containsKey("temperature")) {
            Object rawTemp = options.get("temperature");
            if (rawTemp instanceof Number) {
                temperature = ((Number) rawTemp).doubleValue();
            }
        }
        requestBody.put("temperature", temperature);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        if (response != null && response.containsKey("choices")) {
            List<?> choices = (List<?>) response.get("choices");
            if (!choices.isEmpty()) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                if (message != null && message.containsKey("content")) {
                    return String.valueOf(message.get("content")).trim();
                }
            }
        }
        throw new RuntimeException("Invalid response from OpenAPI: " + response);
    }
}
