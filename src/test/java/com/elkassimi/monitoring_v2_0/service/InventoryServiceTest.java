package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.InventoryRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Inventory;
import com.elkassimi.monitoring_v2_0.repository.InventoryRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private AgentService agentService;
    @Mock
    private RealTimePushService pushService;

    private InventoryService service() {
        return new InventoryService(inventoryRepository, agentService, new ObjectMapper(), pushService);
    }

    @Test
    void save_withInventoryData_storesJsonAndPushes() {
        InventoryService svc = service();
        Agent agent = Agent.builder().agentId("agt-1").build();
        when(agentService.touch("agt-1")).thenReturn(agent);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryRequestDto dto = new InventoryRequestDto();
        dto.setAgentId("agt-1");
        dto.setTimestamp(1_700_000_000.0);
        dto.setInventory(Map.of("os", "Ubuntu 22.04"));

        Inventory saved = svc.save(dto);

        assertThat(saved.getAgent()).isSameAs(agent);
        assertThat(saved.getPayloadJson()).contains("Ubuntu 22.04");
        verify(pushService).pushInventory(saved, "agt-1");
    }

    @Test
    void save_nullInventory_defaultsToEmptyJsonObject() {
        InventoryService svc = service();
        when(agentService.touch("agt-2")).thenReturn(Agent.builder().agentId("agt-2").build());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryRequestDto dto = new InventoryRequestDto();
        dto.setAgentId("agt-2");

        Inventory saved = svc.save(dto);

        assertThat(saved.getPayloadJson()).isEqualTo("{}");
    }

    @Test
    void latestForAgent_delegatesToRepository() {
        InventoryService svc = service();
        Inventory latest = Inventory.builder().build();
        when(inventoryRepository.findFirstByAgent_AgentIdOrderByTimestampDesc("agt-3"))
                .thenReturn(Optional.of(latest));

        assertThat(svc.latestForAgent("agt-3")).contains(latest);
    }
}
