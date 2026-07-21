package com.aiwrapper.template;

import java.util.Map;

public class PromptTemplate {
    private final String template;

    public PromptTemplate(String template) {
        this.template = template;
    }

    public String render(Map<String, Object> params) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
