package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CommandCreateRequestDto {

    @NotBlank
    private String command;

    @NotBlank
    private String userId;

    private List<String> args;

    private Integer timeout;
}
