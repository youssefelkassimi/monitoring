package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.CommandCreateRequestDto;
import com.elkassimi.monitoring_v2_0.dto.PendingCommandDto;
import com.elkassimi.monitoring_v2_0.dto.RemoteCommandResultDto;
import com.elkassimi.monitoring_v2_0.dto.RemoteCommandResultInnerDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.RemoteCommand;
import com.elkassimi.monitoring_v2_0.repository.RemoteCommandRepository;
import com.elkassimi.monitoring_v2_0.util.JsonUtil;
import com.elkassimi.monitoring_v2_0.util.TimeUtil;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Queues and tracks commands executed by remote/commands.py's
 * RemoteCommandHandler. The agent only runs a command if it's also on its
 * own local config.yaml allow list - this service doesn't know or enforce
 * that list, it just relays what the operator queues. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RemoteCommandService {

    private final RemoteCommandRepository remoteCommandRepository;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;
    private  final UserService userService;
    private final RealTimePushService pushService;

    @Transactional
    public RemoteCommand queue(String agentId, CommandCreateRequestDto dto) throws Exception {
        log.info("Queueing remote command for agentId={}, command='{}', timeout={}",
                agentId, dto.getCommand(), dto.getTimeout());

        Agent agent = agentService.getByAgentId(agentId);

        RemoteCommand command = RemoteCommand.builder()
                .agent(agent)
                .command(dto.getCommand())
                .user(userService.findOrThrow(dto.getUserId()))
                .argsJson(JsonUtil.toJson(objectMapper, dto.getArgs()))
                .timeout(dto.getTimeout() != null ? dto.getTimeout() : 30)
                .status(RemoteCommand.CommandStatus.PENDING)
                .build();


        RemoteCommand saved = remoteCommandRepository.save(command);
        log.info("Remote command queued: id={}, agentId={}, command='{}', status={}",
                saved.getId(), agentId, saved.getCommand(), saved.getStatus());

        pushService.pushCommand(saved, agentId);
        log.debug("Pushed queued command to real-time clients: id={}, agentId={}", saved.getId(), agentId);

        return saved;
    }

    /** Called by the agent's poll loop (sender.py: fetch_commands()). Every
     * PENDING command for this agent is handed over and flipped to SENT so
     * the next poll doesn't re-deliver it. */
    @Transactional
    public List<PendingCommandDto> fetchPending(String agentId) {
        log.debug("Fetching pending commands for agentId={}", agentId);

        List<RemoteCommand> pending = remoteCommandRepository
                .findByAgent_AgentIdAndStatus(agentId, RemoteCommand.CommandStatus.PENDING);

        log.debug("Found {} pending command(s) for agentId={}", pending.size(), agentId);

        Instant now = Instant.now();
        List<PendingCommandDto> result = pending.stream()
                .map(cmd -> {
                    cmd.setStatus(RemoteCommand.CommandStatus.SENT);
                    cmd.setSentAt(now);
                    log.debug("Dispatching command id={} to agentId={}, command='{}', timeout={}",
                            cmd.getId(), agentId, cmd.getCommand(), cmd.getTimeout());
                    return PendingCommandDto.builder()
                            .id(cmd.getId().toString())
                            .command(cmd.getCommand())
                            .args(JsonUtil.fromJson(objectMapper, cmd.getArgsJson(), Object.class))
                            .timeout(cmd.getTimeout())
                            .build();
                })
                .toList();

        remoteCommandRepository.saveAll(pending);
        log.info("Dispatched {} command(s) to agentId={}, marked as SENT", result.size(), agentId);

        return result;
    }

    /** Called when the agent reports back (sender.py: send_command_result()). */
    @Transactional
    public RemoteCommand recordResult(String commandId, RemoteCommandResultDto dto) {
        log.debug("Recording result for commandId={}", commandId);

        RemoteCommand command = remoteCommandRepository.findById(UUID.fromString(commandId))
                .orElseThrow(() -> {
                    log.warn("Command result rejected: unknown command id={}", commandId);
                    return new NoSuchElementException("Unknown command id: " + commandId);
                });

        RemoteCommandResultInnerDto result = dto.getResult();
        if (result != null) {
            command.setStatus(parseStatus(result.getStatus()));
            command.setStdout(result.getStdout());
            command.setStderr(result.getStderr());
            command.setExitCode(result.getExitCode());
            command.setExecutedAt(TimeUtil.fromEpochSeconds(result.getExecutedAt()));
            log.debug("Command result details: id={}, reportedStatus='{}', exitCode={}",
                    commandId, result.getStatus(), result.getExitCode());
        } else {
            log.warn("Command result payload was null for commandId={}, leaving command unchanged", commandId);
        }

        RemoteCommand saved = remoteCommandRepository.save(command);
        log.info("Command result recorded: id={}, agentId={}, command='{}', status={}, exitCode={}",
                saved.getId(), command.getAgent().getAgentId(), saved.getCommand(),
                saved.getStatus(), saved.getExitCode());

        pushService.pushCommandResultToUser(saved, command.getAgent().getAgentId(), saved.getUser().getId());
        pushService.pushCommandResult(saved, saved.getAgent().getAgentId());
        log.debug("Pushed command result to real-time clients: id={}, agentId={}",
                saved.getId(), command.getAgent().getAgentId());

        return saved;
    }

    public Page<RemoteCommand> listForAgent(String agentId, Pageable pageable) {
        log.debug("Listing remote commands for agentId={}, pageable={}", agentId, pageable);
        Page<RemoteCommand> page = remoteCommandRepository.findByAgent_AgentIdOrderByCreatedAtDesc(agentId, pageable);
        log.debug("Listed {} remote commands (total={}) for agentId={}",
                page.getNumberOfElements(), page.getTotalElements(), agentId);
        return page;
    }

    private RemoteCommand.CommandStatus parseStatus(String status) {
        if (status == null) {
            log.trace("parseStatus: null command status, defaulting to ERROR");
            return RemoteCommand.CommandStatus.ERROR;
        }
        try {
            return RemoteCommand.CommandStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("parseStatus: unrecognized command status '{}', defaulting to ERROR", status);
            return RemoteCommand.CommandStatus.ERROR;
        }
    }
}