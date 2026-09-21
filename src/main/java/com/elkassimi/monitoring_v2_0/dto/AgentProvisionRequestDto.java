package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


public record AgentProvisionRequestDto(

        @NotBlank
         String label,

        @NotNull @Positive Integer validityMinutes

) {}
