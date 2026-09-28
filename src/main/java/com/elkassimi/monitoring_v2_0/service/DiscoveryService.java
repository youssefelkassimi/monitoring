package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.DiscoveryRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Discovery;
import com.elkassimi.monitoring_v2_0.model.Inventory;
import com.elkassimi.monitoring_v2_0.repository.DiscoveryRepository;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscoveryService {

    private final DiscoveryRepository discoveryRepository;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;
    private final RealTimePushService pushService;

    @Transactional
    public Discovery save(DiscoveryRequestDto dto) {
        log.debug("Saving discovery for agentId={}, timestamp={}", dto.getAgentId(), dto.getTimestamp());

        Agent agent = agentService.touch(dto.getAgentId());
        log.trace("Touched agent before discovery save: agentId={}", agent.getAgentId());

        String payload = JsonUtil.toJson(objectMapper, dto.getDiscovery());
        if (payload == null) {
            log.warn("Discovery payload serialized to null for agentId={}, defaulting to empty object", dto.getAgentId());
        }

        Discovery discovery = Discovery.builder()
                .agent(agent)
                .timestamp(TimeUtil.fromEpochSeconds(dto.getTimestamp()))
                .payloadJson(payload != null ? payload : "{}")
                .build();

        Discovery saved = discoveryRepository.save(discovery);
        log.info("Discovery saved: id={}, agentId={}, payloadLength={}",
                saved.getId(), dto.getAgentId(),
                saved.getPayloadJson() != null ? saved.getPayloadJson().length() : 0);

        pushService.pushDiscovery(saved, dto.getAgentId());
        log.debug("Pushed discovery update for agentId={}", dto.getAgentId());

        return saved;
    }

    public Page<Discovery> listForAgent(String agentId, Pageable pageable) {
        log.debug("Listing discovery records for agentId={}, pageable={}", agentId, pageable);
        Page<Discovery> page = discoveryRepository.findByAgent_AgentIdOrderByTimestampDesc(agentId, pageable);
        log.debug("Listed {} discovery records (total={}) for agentId={}",
                page.getNumberOfElements(), page.getTotalElements(), agentId);
        return page;
    }

    public Optional<Discovery> latestForAgent(String agentId) {
        log.debug("Fetching latest discovery for agentId={}", agentId);
        Optional<Discovery> latest = discoveryRepository.findFirstByAgent_AgentIdOrderByTimestampDesc(agentId);
        if (latest.isEmpty()) {
            log.debug("No discovery found for agentId={}", agentId);
        } else {
            log.trace("Latest discovery for agentId={}: id={}, timestamp={}",
                    agentId, latest.get().getId(), latest.get().getTimestamp());
        }
        return latest;
    }
}