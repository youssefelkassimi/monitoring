package com.elkassimi.monitoring_v2_0.controller;

import com.elkassimi.monitoring_v2_0.config.GlobalExceptionHandler;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Alert;
import com.elkassimi.monitoring_v2_0.model.RemoteCommand;
import com.elkassimi.monitoring_v2_0.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QueryControllerTest {

    @Mock private AgentService agentService;
    @Mock private MetricsService metricsService;
    @Mock private InventoryService inventoryService;
    @Mock private DiscoveryService discoveryService;
    @Mock private LogService logService;
    @Mock private AlertService alertService;
    @Mock private RemoteCommandService remoteCommandService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        QueryController controller = new QueryController(
                agentService, metricsService, inventoryService, discoveryService,
                logService, alertService, remoteCommandService);
        mockMvc = MockMvcBuilders.
                standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listAgents_returnsAllAgents() throws Exception {
        when(agentService.findAll()).thenReturn(List.of(Agent.builder().agentId("agt-1").build()));

        mockMvc.perform(get("/api/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].agentId").value("agt-1"));
    }

    @Test
    void getAgent_found_returnsIt() throws Exception {
        when(agentService.getByAgentId("agt-1")).thenReturn(Agent.builder().agentId("agt-1").build());

        mockMvc.perform(get("/api/agents/agt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentId").value("agt-1"));
    }

    @Test
    void getAgent_unknown_returns404() throws Exception {
        when(agentService.getByAgentId("missing")).thenThrow(new NoSuchElementException("Unknown agent: missing"));

        mockMvc.perform(get("/api/agents/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void listMetrics_returnsPagedMetrics() throws Exception {
        when(metricsService.listForAgent(eq("agt-1"), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api/agents/agt-1/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void latestMetrics_present_returnsIt() throws Exception {
        when(metricsService.latestForAgent("agt-1"))
                .thenReturn(Optional.of(com.elkassimi.monitoring_v2_0.model.Metrics.builder().build()));

        mockMvc.perform(get("/api/agents/agt-1/metrics/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void latestMetrics_absent_returnsOkWithNullData() throws Exception {
        when(metricsService.latestForAgent("agt-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/agents/agt-1/metrics/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void latestInventory_present_returnsIt() throws Exception {
        when(inventoryService.latestForAgent("agt-1"))
                .thenReturn(Optional.of(com.elkassimi.monitoring_v2_0.model.Inventory.builder().build()));

        mockMvc.perform(get("/api/agents/agt-1/inventory/latest"))
                .andExpect(status().isOk());
    }

    @Test
    void listDiscovery_returnsPagedResults() throws Exception {
        when(discoveryService.listForAgent(eq("agt-1"), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/agents/agt-1/discovery"))
                .andExpect(status().isOk());
    }

    @Test
    void listLogs_returnsPagedResults() throws Exception {
        when(logService.listForAgent(eq("agt-1"), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/agents/agt-1/logs"))
                .andExpect(status().isOk());
    }

    @Test
    void listAgentAlerts_returnsPagedResults() throws Exception {
        when(alertService.listForAgent(eq("agt-1"), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api/agents/agt-1/alerts"))
                .andExpect(status().isOk());
    }

    @Test
    void listAlerts_noStatusFilter_listsAll() throws Exception {
        when(alertService.listAll(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk());
    }

    @Test
    void listAlerts_withStatusFilter_listsByStatus() throws Exception {
        when(alertService.listByStatus(eq(Alert.AlertStatus.PROBLEM), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api/alerts").param("status", "problem"))
                .andExpect(status().isOk());
    }

    @Test
    void queueCommand_returnsQueuedCommand() throws Exception {
        when(remoteCommandService.queue(eq("agt-1"), any()))
                .thenReturn(RemoteCommand.builder().command("uptime").build());

        String body = """
                {"command":"uptime"}
                """;

        mockMvc.perform(post("/api/agents/agt-1/commands")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.command").value("uptime"));
    }

    @Test
    void queueCommand_missingCommand_returns400() throws Exception {
        String body = """
                {}
                """;

        mockMvc.perform(post("/api/agents/agt-1/commands")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listCommands_returnsPagedResults() throws Exception {
        when(remoteCommandService.listForAgent(eq("agt-1"), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/agents/agt-1/commands"))
                .andExpect(status().isOk());
    }
}
