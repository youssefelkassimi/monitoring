package com.elkassimi.monitoring_v2_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RemoteCommandResultInnerDto {

    @JsonProperty("command_id")
    private String commandId;

    private String command;
    private Object args;

    /** "success" | "error" | "timeout" | "rejected". */
    private String status;

    private String stdout;
    private String stderr;

    @JsonProperty("exit_code")
    private Integer exitCode;

    @JsonProperty("executed_at")
    private Double executedAt;
}
