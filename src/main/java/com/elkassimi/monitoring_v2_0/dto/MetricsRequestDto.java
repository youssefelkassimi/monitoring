package com.elkassimi.monitoring_v2_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * The payload envelope sent by the agent for metric batches (sender.py:
 * send_metrics()). Fields beyond agentId/sentAt are captured generically as
 * Map<String,Object> so the backend tolerates collector changes without a
 * schema migration.
 */
@Data
public class MetricsRequestDto {

    @NotBlank
    private String agentId;

    @JsonProperty("sent_at")
    private Double sentAt;

    private Map<String, Object> system;
    private Map<String, Object> checks;
    private Map<String, Object> plugins;

    @JsonProperty("alerts_summary")
    private Map<String, Object> alertsSummary;

}
