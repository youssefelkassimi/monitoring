package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** sender.py: send_command_result() -> POST /api/commands/{commandId}/result. */
@Data
public class RemoteCommandResultDto {

    @NotBlank
    private String agentId;

    private Double timestamp;

    private RemoteCommandResultInnerDto result;
}
