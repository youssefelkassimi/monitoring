package com.elkassimi.monitoring_v2_0.controller;

import com.elkassimi.monitoring_v2_0.dto.AlertsRequestDto;
import com.elkassimi.monitoring_v2_0.dto.ApiResponse;
import com.elkassimi.monitoring_v2_0.dto.DiscoveryRequestDto;
import com.elkassimi.monitoring_v2_0.dto.HeartbeatRequestDto;
import com.elkassimi.monitoring_v2_0.dto.InventoryRequestDto;
import com.elkassimi.monitoring_v2_0.dto.LogsRequestDto;
import com.elkassimi.monitoring_v2_0.dto.MetricsRequestDto;
import com.elkassimi.monitoring_v2_0.dto.PendingCommandDto;
import com.elkassimi.monitoring_v2_0.dto.RegistrationRequestDto;
import com.elkassimi.monitoring_v2_0.dto.RemoteCommandResultDto;
import com.elkassimi.monitoring_v2_0.model.Metrics;
import com.elkassimi.monitoring_v2_0.service.AgentService;
import com.elkassimi.monitoring_v2_0.service.AlertService;
import com.elkassimi.monitoring_v2_0.service.DiscoveryService;
import com.elkassimi.monitoring_v2_0.service.InventoryService;
import com.elkassimi.monitoring_v2_0.service.LogService;
import com.elkassimi.monitoring_v2_0.service.MetricsService;
import com.elkassimi.monitoring_v2_0.service.RemoteCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Ingest-side API: everything the Python agent itself calls (see sender.py).
 * Endpoints and payload shapes here are dictated by that client, not chosen
 * freely - keep this file and sender.py in sync.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AgentIngestController {

    private final AgentService agentService;
    private final MetricsService metricsService;
    private final InventoryService inventoryService;
    private final DiscoveryService discoveryService;
    private final LogService logService;
    private final AlertService alertService;
    private final RemoteCommandService remoteCommandService;

    @PostMapping("/agent-register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegistrationRequestDto dto) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok(agentService.register(dto)));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse> heartbeat(@Valid @RequestBody HeartbeatRequestDto dto) {
        agentService.recordHeartbeat(dto);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/metrics")
    public ResponseEntity<ApiResponse> metrics(@Valid @RequestBody MetricsRequestDto dto) {
        metricsService.save(dto);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/inventory")
    public ResponseEntity<ApiResponse> inventory(@Valid @RequestBody InventoryRequestDto dto) {
        inventoryService.save(dto);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/discovery")
    public ResponseEntity<ApiResponse> discovery(@Valid @RequestBody DiscoveryRequestDto dto) {
        discoveryService.save(dto);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/logs")
    public ResponseEntity<ApiResponse> logs(@Valid @RequestBody LogsRequestDto dto) {
        logService.save(dto);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/alerts")
    public ResponseEntity<ApiResponse> alerts(@Valid @RequestBody AlertsRequestDto dto) {
        alertService.save(dto);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * Polled by main.py's _remote_command_loop every remote_commands.poll_interval
     * seconds. sender.fetch_commands() reads resp.json() directly and expects a
     * top-level "commands" key - it is NOT wrapped in the {status,data,message}
     * ApiResponse envelope like every other endpoint here.
     */
    @GetMapping("/commands/{agentId}")
    public ResponseEntity<Map<String, List<PendingCommandDto>>> pendingCommands(@PathVariable String agentId) {
        return ResponseEntity.ok(Map.of("commands", remoteCommandService.fetchPending(agentId)));
    }

    @PostMapping("/commands/{commandId}/result")
    public ResponseEntity<ApiResponse> commandResult(
            @PathVariable String commandId,
            @Valid @RequestBody RemoteCommandResultDto dto) {
        remoteCommandService.recordResult(commandId, dto);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * sender.fetch_config() is defined on the agent but not yet called from
     * the collection loop - placeholder so the endpoint exists once that
     * wiring lands. Returns an empty config; extend as agent-side dynamic
     * config support grows.
     */
    @GetMapping("/agent-config/{agentId}")
    public ResponseEntity<ApiResponse> agentConfig(@PathVariable String agentId) {
        agentService.getByAgentId(agentId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of()));
    }
}
