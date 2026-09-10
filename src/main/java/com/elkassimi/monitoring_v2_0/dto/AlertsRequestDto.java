package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** sender.py: send_alerts(). */
@Data
public class AlertsRequestDto {

    @NotBlank
    private String agentId;

    private Double timestamp;

    private List<AlertEventDto> alerts;
}
