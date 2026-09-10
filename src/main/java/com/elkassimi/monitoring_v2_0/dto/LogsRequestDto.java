package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** sender.py: send_log_events(). Each entry in "logs" mirrors LogMonitor's
 * per-watched-file result (path/status/new_lines/matched_lines/...). */
@Data
public class LogsRequestDto {

    @NotBlank
    private String agentId;

    private Double timestamp;

    private List<Map<String, Object>> logs;
}
