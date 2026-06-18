package com.marketx.fixgateway.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class FixMessageValidator {
    public void validate(Map<String, String> tags) {
        String messageType = require(tags, "35");
        if ("D".equals(messageType)) {
            validateNewOrder(tags);
            return;
        }
        if ("F".equals(messageType)) {
            validateCancel(tags);
            return;
        }
        throw new IllegalArgumentException("Unsupported FIX message type 35=" + messageType);
    }

    private void validateNewOrder(Map<String, String> tags) {
        requireAll(tags, List.of("11", "49", "55", "54", "38", "40", "60"));
        validateSide(tags.get("54"));
        validateOrderType(tags.get("40"));
        validatePositiveInteger(tags.get("38"), "38 OrderQty");
        if ("2".equals(tags.get("40"))) {
            validatePositiveDecimal(require(tags, "44"), "44 Price");
        }
    }

    private void validateCancel(Map<String, String> tags) {
        requireAll(tags, List.of("11", "41", "49", "55", "54", "60"));
        validateSide(tags.get("54"));
    }

    private void requireAll(Map<String, String> tags, List<String> requiredTags) {
        for (String tag : requiredTags) {
            require(tags, tag);
        }
    }

    private String require(Map<String, String> tags, String tag) {
        String value = tags.get(tag);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required FIX tag " + tag);
        }
        return value;
    }

    private void validateSide(String side) {
        if (!"1".equals(side) && !"2".equals(side)) {
            throw new IllegalArgumentException("Unsupported FIX side 54=" + side);
        }
    }

    private void validateOrderType(String orderType) {
        if (!"1".equals(orderType) && !"2".equals(orderType)) {
            throw new IllegalArgumentException("Unsupported FIX order type 40=" + orderType);
        }
    }

    private void validatePositiveInteger(String value, String field) {
        try {
            if (Integer.parseInt(value) <= 0) {
                throw new IllegalArgumentException(field + " must be greater than zero");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be a whole number");
        }
    }

    private void validatePositiveDecimal(String value, String field) {
        try {
            if (new BigDecimal(value).compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(field + " must be greater than zero");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be a decimal number");
        }
    }
}
