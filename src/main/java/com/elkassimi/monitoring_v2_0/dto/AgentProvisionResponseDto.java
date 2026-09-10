package com.elkassimi.monitoring_v2_0.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentProvisionResponseDto {
    private String id;
    private String label;
    private String token;
    private Instant tokenExpiresAt;
}
