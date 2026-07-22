package com.aiwrapper.provider;

import com.aiwrapper.config.AiEntity.GoogleTranslate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleTranslateProvider implements AiProvider {
    private final GoogleTranslate config;
    private final RestTemplate restTemplate;

    public GoogleTranslateProvider(GoogleTranslate config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String complete(String prompt, Map<String, Object> options) throws Exception {
        String textToTranslate = prompt;
        if (prompt != null && prompt.contains(": ")) {
            textToTranslate = prompt.substring(prompt.indexOf(": ") + 2).trim();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> queryList;
        boolean isBatchInput = false;
        try {
            if (prompt != null && prompt.trim().startsWith("[")) {
                queryList = objectMapper.readValue(prompt,
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                        });
                isBatchInput = true;
            } else {
                queryList = List.of(textToTranslate);
            }
        } catch (Exception e) {
            queryList = List.of(textToTranslate);
        }

        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            String url = "https://translation.googleapis.com/language/translate/v2?key=" + apiKey.trim();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("q", queryList);
            String target = "vi";
            if (options != null && options.containsKey("targetLanguage")) {
                target = String.valueOf(options.get("targetLanguage"));
            }
            requestBody.put("target", target);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("data")) {
                Map<?, ?> data = (Map<?, ?>) response.get("data");
                if (data != null && data.containsKey("translations")) {
                    List<?> translations = (List<?>) data.get("translations");
                    if (!translations.isEmpty()) {
                        if (isBatchInput) {
                            List<String> results = new ArrayList<>();
                            for (Object transObj : translations) {
                                Map<?, ?> transMap = (Map<?, ?>) transObj;
                                results.add(String.valueOf(transMap.get("translatedText")));
                            }
                            return objectMapper.writeValueAsString(results);
                        } else {
                            Map<?, ?> translation = (Map<?, ?>) translations.get(0);
                            if (translation.containsKey("translatedText")) {
                                return String.valueOf(translation.get("translatedText"));
                            }
                        }
                    }
                }
            }
            throw new RuntimeException("Invalid response from official Google Translate API: " + response);
        } else {
            String target = "vi";
            if (options != null && options.containsKey("targetLanguage")) {
                target = String.valueOf(options.get("targetLanguage"));
            }

            if (isBatchInput) {
                List<String> results = new ArrayList<>();
                for (String qText : queryList) {
                    String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl={tl}&dt=t&q={q}";
                    try {
                        List<?> response = restTemplate.getForObject(url, List.class, target, qText);
                        if (response != null && !response.isEmpty()) {
                            List<?> outerList = (List<?>) response.get(0);
                            if (outerList != null && !outerList.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (Object sentenceObj : outerList) {
                                    if (sentenceObj instanceof List) {
                                        List<?> sentenceList = (List<?>) sentenceObj;
                                        if (!sentenceList.isEmpty()) {
                                            sb.append(sentenceList.get(0));
                                        }
                                    }
                                }
                                results.add(sb.toString());
                            } else {
                                results.add(qText);
                            }
                        } else {
                            results.add(qText);
                        }
                    } catch (Exception ex) {
                        results.add(qText);
                    }
                }
                return objectMapper.writeValueAsString(results);
            } else {
                String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl={tl}&dt=t&q={q}";
                List<?> response = restTemplate.getForObject(url, List.class, target, textToTranslate);
                if (response != null && !response.isEmpty()) {
                    List<?> outerList = (List<?>) response.get(0);
                    if (outerList != null && !outerList.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (Object sentenceObj : outerList) {
                            if (sentenceObj instanceof List) {
                                List<?> sentenceList = (List<?>) sentenceObj;
                                if (!sentenceList.isEmpty()) {
                                    sb.append(sentenceList.get(0));
                                }
                            }
                        }
                        return sb.toString();
                    }
                }
                throw new RuntimeException("Invalid response from free Google Translate API");
            }
        }
    }
}
