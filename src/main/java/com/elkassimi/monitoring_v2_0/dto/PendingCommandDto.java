package com.elkassimi.monitoring_v2_0.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One entry returned to the agent by GET /api/commands/{agentId} - shape
 * expected by main.py's _remote_command_loop (cmd.get("id"/"command"/"args"/"timeout")). */
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
