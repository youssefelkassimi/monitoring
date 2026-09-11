package com.elkassimi.monitoring_v2_0.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonUtilTest {

    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    void toJson_nullValue_returnsNull() {
        assertThat(JsonUtil.toJson(realMapper, null)).isNull();
    }

    @Test
    void toJson_map_returnsJsonString() throws Exception {
        String json = JsonUtil.toJson(realMapper, Map.of("cpu_percent", 42.0));
        assertThat(json).isEqualTo(realMapper.writeValueAsString(Map.of("cpu_percent", 42.0)));
    }

    @Test
    void toJson_serializationFailure_wrapsInIllegalStateException() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> JsonUtil.toJson(failingMapper, "anything"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize");
    }

    @Test
    void fromJson_nullSource_returnsNull() {
        assertThat(JsonUtil.fromJson(realMapper, null, Map.class)).isNull();
    }

    @Test
    void fromJson_blankSource_returnsNull() {
        assertThat(JsonUtil.fromJson(realMapper, "   ", Map.class)).isNull();
    }

    @Test
    void fromJson_validJson_deserializesToRequestedType() {
        Map<String, Object> result = JsonUtil.fromJson(realMapper, "{\"a\":1}", Map.class);
        assertThat(result).containsEntry("a", 1);
    }

    @Test
    void fromJson_deserializationFailure_wrapsInIllegalStateException() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.readValue(eq("{bad}"), eq(Map.class)))
                .thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> JsonUtil.fromJson(failingMapper, "{bad}", Map.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to deserialize");
    }
}
