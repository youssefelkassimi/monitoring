package com.elkassimi.monitoring_v2_0.dto;

import java.util.Map;

public record AnomalyEvent(
        String agentId,
        Map<String, Object> metrics
) {}
