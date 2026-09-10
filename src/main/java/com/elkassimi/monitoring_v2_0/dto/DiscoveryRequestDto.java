package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/** sender.py: send_discovery(); covers both network-sweep and open-service
 * discovery results, which share the same envelope on the agent side. */
@Data
public class DiscoveryRequestDto {

    @NotBlank
    private String agentId;

    private Double timestamp;

    private Map<String, Object> discovery;
}
