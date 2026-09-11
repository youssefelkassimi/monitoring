package com.elkassimi.monitoring_v2_0.controller;

import com.elkassimi.monitoring_v2_0.config.GlobalExceptionHandler;
import com.elkassimi.monitoring_v2_0.dto.*;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.RemoteCommand;
import com.elkassimi.monitoring_v2_0.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AgentIngestControllerTest {

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
        AgentIngestController controller = new AgentIngestController(
                agentService, metricsService, inventoryService, discoveryService,
                logService, alertService, remoteCommandService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_returnsOkWithSavedAgent() throws Exception {
        Agent agent = Agent.builder().agentId("agt-1").hostname("host-1").build();
        when(agentService.register(any())).thenReturn(agent);

        String body = """
                {"agentId":"agt-1","hostname":"host-1","os":"Ubuntu"}
                """;

        mockMvc.perform(post("/api/agent-register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.data.agentId").value("agt-1"));
    }

    @Test
    void register_missingAgentId_returns400() throws Exception {
        String body = """
                {"hostname":"host-1"}
                """;

        mockMvc.perform(post("/api/agent-register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void heartbeat_returnsOk_andInvokesService() throws Exception {
        String body = """
                {"agentId":"agt-1","hostname":"host-1","status":"alive"}
                """;

        mockMvc.perform(post("/api/heartbeat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verify(agentService).recordHeartbeat(any());
    }

    @Test
    void metrics_returnsOkWithSavedMetrics() throws Exception {
        when(metricsService.save(any())).thenReturn(com.elkassimi.monitoring_v2_0.model.Metrics.builder().build());

        String body = """
                {"agentId":"agt-1","system":{},"checks":{}}
                """;

        mockMvc.perform(post("/api/metrics").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void inventory_returnsOk() throws Exception {
        when(inventoryService.save(any())).thenReturn(com.elkassimi.monitoring_v2_0.model.Inventory.builder().build());

        String body = """
                {"agentId":"agt-1","inventory":{}}
                """;

        mockMvc.perform(post("/api/inventory").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void discovery_returnsOk() throws Exception {
        when(discoveryService.save(any())).thenReturn(com.elkassimi.monitoring_v2_0.model.Discovery.builder().build());

        String body = """
                {"agentId":"agt-1","discovery":{}}
                """;

        mockMvc.perform(post("/api/discovery").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void logs_returnsOk() throws Exception {
        when(logService.save(any())).thenReturn(com.elkassimi.monitoring_v2_0.model.Logs.builder().build());

        String body = """
                {"agentId":"agt-1","logs":[]}
                """;

        mockMvc.perform(post("/api/logs").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void alerts_returnsOk() throws Exception {
        when(alertService.save(any())).thenReturn(List.of());

        String body = """
                {"agentId":"agt-1","alerts":[]}
                """;

        mockMvc.perform(post("/api/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void pendingCommands_returnsRawUnwrappedCommandsList() throws Exception {
        PendingCommandDto cmd = PendingCommandDto.builder().id("cmd-1").command("uptime").timeout(30).build();
        when(remoteCommandService.fetchPending("agt-1")).thenReturn(List.of(cmd));

        mockMvc.perform(get("/api/commands/agt-1"))
                .andExpect(status().isOk())
                // NOT wrapped in the {status,data,message} envelope - see controller javadoc
                .andExpect(jsonPath("$.commands[0].id").value("cmd-1"))
                .andExpect(jsonPath("$.commands[0].command").value("uptime"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void pendingCommands_noneQueued_returnsEmptyCommandsArray() throws Exception {
        when(remoteCommandService.fetchPending("agt-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/commands/agt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commands").isArray())
                .andExpect(jsonPath("$.commands").isEmpty());
    }

    @Test
    void commandResult_returnsOk() throws Exception {
        when(remoteCommandService.recordResult(eq("cmd-1"), any()))
                .thenReturn(RemoteCommand.builder().command("uptime").build());

        String body = """
                {"agentId":"agt-1","result":{"command_id":"cmd-1","status":"success"}}
                """;

        mockMvc.perform(post("/api/commands/cmd-1/result").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void commandResult_unknownCommandId_returns404() throws Exception {
        when(remoteCommandService.recordResult(eq("missing"), any()))
                .thenThrow(new NoSuchElementException("Unknown command id: missing"));

        String body = """
                {"agentId":"agt-1","result":{"command_id":"missing","status":"success"}}
                """;

        mockMvc.perform(post("/api/commands/missing/result").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void agentConfig_knownAgent_returnsEmptyConfig() throws Exception {
        when(agentService.getByAgentId("agt-1")).thenReturn(Agent.builder().agentId("agt-1").build());

        mockMvc.perform(get("/api/agent-config/agt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void agentConfig_unknownAgent_returns404() throws Exception {
        when(agentService.getByAgentId("missing")).thenThrow(new NoSuchElementException("Unknown agent: missing"));

        mockMvc.perform(get("/api/agent-config/missing"))
                .andExpect(status().isNotFound());
    }
}
