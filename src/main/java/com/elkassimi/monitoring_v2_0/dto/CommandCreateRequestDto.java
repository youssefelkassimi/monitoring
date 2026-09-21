package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** Body for the dashboard-facing "queue a remote command for this agent"
 * endpoint. Not part of the Python agent's own outbound contract. */
@Data
public class CommandCreateRequestDto {

    @NotBlank
    private String command;

    private String userId;

    private List<String> args;

    private Integer timeout;
}
