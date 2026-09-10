package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.LogsRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Logs;
import com.elkassimi.monitoring_v2_0.repository.LogRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private final LogRepository logRepository;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;
    private final RealTimePushService pushService;

    @Transactional
    public Logs save(LogsRequestDto dto) {
        log.debug("Saving logs for agentId={}, timestamp={}", dto.getAgentId(), dto.getTimestamp());

        Agent agent = agentService.touch(dto.getAgentId());
        log.trace("Touched agent before logs save: agentId={}", agent.getAgentId());

        String payload = JsonUtil.toJson(objectMapper, dto.getLogs());
        if (payload == null) {
            log.warn("Logs payload serialized to null for agentId={}, defaulting to empty array", dto.getAgentId());
        }

        Logs logs = Logs.builder()
                .agent(agent)
                .timestamp(TimeUtil.fromEpochSeconds(dto.getTimestamp()))
                .payloadJson(payload != null ? payload : "[]")
                .build();

        Logs saved = logRepository.save(logs);
        log.info("Logs saved: id={}, agentId={}, payloadLength={}",
                saved.getId(), dto.getAgentId(),
                saved.getPayloadJson() != null ? saved.getPayloadJson().length() : 0);

        pushService.pushLogs(saved, dto.getAgentId());
        log.debug("Pushed logs update for agentId={}", dto.getAgentId());

        return saved;
    }

    public Page<Logs> listForAgent(String agentId, Pageable pageable) {
        log.debug("Listing logs for agentId={}, pageable={}", agentId, pageable);
        Page<Logs> page = logRepository.findByAgent_AgentIdOrderByTimestampDesc(agentId, pageable);
        log.debug("Listed {} log records (total={}) for agentId={}",
                page.getNumberOfElements(), page.getTotalElements(), agentId);
        return page;
    }
}