package com.elkassimi.monitoring_v2_0.dto;

import java.time.Instant;
import java.util.Map;

public record AnomalyAlert(
        String agentId,
        String status,
        double score,
        Map<String, Double> topFeatures,
        Instant at
) { }
