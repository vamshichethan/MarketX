package com.marketx.fixgateway.parser;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FixMessageParser {
    public Map<String, String> parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("FIX message is required");
        }

        Map<String, String> tags = new LinkedHashMap<>();
        String[] fields = rawMessage.split("\\|");
        for (String field : fields) {
            if (field.isBlank()) {
                continue;
            }

            int separator = field.indexOf('=');
            if (separator <= 0 || separator == field.length() - 1) {
                throw new IllegalArgumentException("Malformed FIX tag: " + field);
            }

            String tag = field.substring(0, separator);
            String value = field.substring(separator + 1);
            tags.put(tag, value);
        }

        if (tags.isEmpty()) {
            throw new IllegalArgumentException("FIX message does not contain tags");
        }
        return tags;
    }
}
