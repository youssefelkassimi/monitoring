package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class HeartbeatRequestDto {

    @NotBlank
    private String agentId;

    private Double timestamp;
    private String hostname;
    private String status;

}
