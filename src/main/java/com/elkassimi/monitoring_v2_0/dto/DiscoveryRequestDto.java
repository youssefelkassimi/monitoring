package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class DiscoveryRequestDto {

    @NotBlank
    private String agentId;

    private Double timestamp;

    private Map<String, Object> discovery;
}
