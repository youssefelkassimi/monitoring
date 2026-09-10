package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.InventoryRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Inventory;
import com.elkassimi.monitoring_v2_0.repository.InventoryRepository;
import com.elkassimi.monitoring_v2_0.util.JsonUtil;
import com.elkassimi.monitoring_v2_0.util.TimeUtil;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;
    private final RealTimePushService pushService;

    @Transactional
    public void save(InventoryRequestDto dto) {
        log.debug("Saving inventory for agentId={}, timestamp={}", dto.getAgentId(), dto.getTimestamp());

        Agent agent = agentService.touch(dto.getAgentId());
        log.trace("Touched agent before inventory save: agentId={}", agent.getAgentId());

        String payload = JsonUtil.toJson(objectMapper, dto.getInventory());
        if (payload == null) {
            log.warn("Inventory payload serialized to null for agentId={}, defaulting to empty object", dto.getAgentId());
        }

        Inventory inventory = Inventory.builder()
                .agent(agent)
                .timestamp(TimeUtil.fromEpochSeconds(dto.getTimestamp()))
                .payloadJson(payload != null ? payload : "{}")
                .build();

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory saved: id={}, agentId={}, payloadLength={}",
                saved.getId(), dto.getAgentId(), saved.getPayloadJson() != null ? saved.getPayloadJson().length() : 0);

        pushService.pushInventory(saved, dto.getAgentId());
        log.debug("Pushed inventory update for agentId={}", dto.getAgentId());
    }

    public Optional<Inventory> latestForAgent(String agentId) {
        log.debug("Fetching latest inventory for agentId={}", agentId);
        Optional<Inventory> latest = inventoryRepository.findFirstByAgent_AgentIdOrderByTimestampDesc(agentId);
        if (latest.isEmpty()) {
            log.debug("No inventory found for agentId={}", agentId);
        } else {
            log.trace("Latest inventory for agentId={}: id={}, timestamp={}",
                    agentId, latest.get().getId(), latest.get().getTimestamp());
        }
        return latest;
    }
}