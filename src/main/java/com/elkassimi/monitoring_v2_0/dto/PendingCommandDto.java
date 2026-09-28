package com.elkassimi.monitoring_v2_0.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingCommandDto {
    private String id;
    private String command;
    private Object args;
    private Integer timeout;
}
