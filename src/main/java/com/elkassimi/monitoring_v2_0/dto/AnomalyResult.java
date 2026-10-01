package com.elkassimi.monitoring_v2_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record AnomalyResult(
        @JsonProperty("host_id") String agentId,
        @JsonProperty("is_anomaly") boolean anomaly,
        double score,
        @JsonProperty("top_features")Map<String,Double> topFutures

        ) {}
