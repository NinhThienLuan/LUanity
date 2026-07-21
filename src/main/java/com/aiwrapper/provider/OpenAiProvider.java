package com.aiwrapper.provider;

import com.aiwrapper.config.AiConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAiProvider implements AiProvider {
    private final AiConfig.OpenAi config;
    private final RestTemplate restTemplate;

    public OpenAiProvider(AiConfig.OpenAi config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String complete(String prompt, Map<String, Object> options) throws Exception {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API Key is not configured.");
        }

        String url = "https://api.openai.com/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel() != null ? config.getModel() : "gpt-4o-mini");

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", List.of(message));

        if (options != null) {
            requestBody.putAll(options);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        if (response != null && response.containsKey("choices")) {
            List<?> choices = (List<?>) response.get("choices");
            if (!choices.isEmpty()) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> messageObj = (Map<?, ?>) firstChoice.get("message");
                if (messageObj != null && messageObj.containsKey("content")) {
                    return String.valueOf(messageObj.get("content"));
                }
            }
        }
        throw new RuntimeException("Invalid response from OpenAI: " + response);
    }
}
