package com.elkassimi.monitoring_v2_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** One entry of the "alerts" list in AlertsRequestDto - mirrors a single
 * TriggerEngine event (triggers/engine.py: evaluate()). */
@Data
public class AlertEventDto {

    @JsonProperty("trigger_name")
    private String triggerName;

    private String severity;
    private String message;
    private String key;
    private Double value;
    private String operator;
    private Double threshold;

    /** Only present on "problem" events, not on "recovery" events. */
    @JsonProperty("held_for_seconds")
    private Double heldForSeconds;

    /** "problem" or "recovery". */
    private String status;
}
