package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RemoteCommandResultDto {

    @NotBlank
    private String agentId;

    private Double timestamp;

    private RemoteCommandResultInnerDto result;
}
