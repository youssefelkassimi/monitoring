package com.elkassimi.monitoring_v2_0.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Thin wrapper so services can store/reload the agent's free-form payload
 * blocks (system/checks/plugins/inventory/discovery/args...) as JSON text
 * columns without repeating try/catch everywhere. */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String toJson(ObjectMapper mapper, Object value) {
        if (value == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payload to JSON", e);
        }
    }

    public static <T> T fromJson(ObjectMapper mapper, String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize stored JSON payload", e);
        }
    }
}
