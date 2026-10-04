package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.AlertEventDto;
import com.elkassimi.monitoring_v2_0.dto.AlertsRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Alert;
import com.elkassimi.monitoring_v2_0.repository.AlertRepository;
import com.elkassimi.monitoring_v2_0.util.TimeUtil;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import com.elkassimi.monitoring_v2_0.websocket.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final AgentService agentService;
    private final RealTimePushService pushService;

    @Transactional
    public List<Alert> save(AlertsRequestDto dto) {
        log.debug("Received alerts batch for agentId={}, timestamp={}, count={}",
                dto.getAgentId(), dto.getTimestamp(),
                dto.getAlerts() == null ? 0 : dto.getAlerts().size());

        if (dto.getAlerts() == null || dto.getAlerts().isEmpty()) {
            log.debug("Empty alerts batch for agentId={}, nothing to save", dto.getAgentId());
            return Collections.emptyList();
        }

        Agent agent = agentService.touch(dto.getAgentId());
        log.trace("Touched agent before alerts save: agentId={}", agent.getAgentId());

        Instant ts = TimeUtil.fromEpochSeconds(dto.getTimestamp());

        List<Alert> alerts = dto.getAlerts().stream()
                .map(event -> toEntity(agent, ts, event))
                .toList();

        List<Alert> saved = alertRepository.saveAll(alerts);
        log.info("Saved {} alerts for agentId={}", saved.size(), dto.getAgentId());

        saved.forEach(a -> {
            log.debug("Pushing alert id={} agentId={} severity={} triggerName='{}'",
                    a.getId(), dto.getAgentId(), a.getSeverity(), a.getTriggerName());
            pushService.pushAlert(a);
            pushService.pushAgentAlert(a, dto.getAgentId());
        });
        return saved;
    }

    private Alert toEntity(Agent agent, Instant timestamp, AlertEventDto event) {
        log.trace("Mapping alert event to entity: triggerName='{}', severity={}, status='{}', key='{}'",
                event.getTriggerName(), event.getSeverity(), event.getStatus(), event.getKey());
        return Alert.builder()
                .agent(agent)
                .triggerName(event.getTriggerName())
                .severity(event.getSeverity())
                .message(event.getMessage())
                .key(event.getKey())
                .value(event.getValue())
                .operator(event.getOperator())
                .threshold(event.getThreshold())
                .heldForSeconds(event.getHeldForSeconds())
                .status(parseStatus(event.getStatus()))
                .timestamp(timestamp)
                .build();
    }

    private Alert.AlertStatus parseStatus(String status) {
        if (status == null) {
            log.trace("parseStatus: null status, defaulting to PROBLEM");
            return Alert.AlertStatus.PROBLEM;
        }
        try {
            return Alert.AlertStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("parseStatus: unrecognized alert status '{}', defaulting to PROBLEM", status);
            return Alert.AlertStatus.PROBLEM;
        }
    }

    public Page<Alert> listForAgent(String agentId, Pageable pageable) {
        log.debug("Listing alerts for agentId={}, pageable={}", agentId, pageable);
        Page<Alert> page = alertRepository.findByAgent_AgentIdOrderByReceivedAtDesc(agentId, pageable);
        log.debug("Listed {} alerts (total={}) for agentId={}",
                page.getNumberOfElements(), page.getTotalElements(), agentId);
        return page;
    }

    public Page<Alert> listAll(Pageable pageable) {
        log.debug("Listing all alerts, pageable={}", pageable);
        Page<Alert> page = alertRepository.findAllByOrderByReceivedAtDesc(pageable);
        log.debug("Listed {} alerts (total={})", page.getNumberOfElements(), page.getTotalElements());
        return page;
    }

    public Page<Alert> listByStatus(Alert.AlertStatus status, Pageable pageable) {
        log.debug("Listing alerts by status={}, pageable={}", status, pageable);
        Page<Alert> page = alertRepository.findByStatusOrderByReceivedAtDesc(status, pageable);
        log.debug("Listed {} alerts (total={}) with status={}",
                page.getNumberOfElements(), page.getTotalElements(), status);
        return page;
    }
}