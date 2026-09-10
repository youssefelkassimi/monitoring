package com.elkassimi.monitoring_v2_0.controller;

import com.elkassimi.monitoring_v2_0.dto.ApiResponse;
import com.elkassimi.monitoring_v2_0.dto.CommandCreateRequestDto;
import com.elkassimi.monitoring_v2_0.model.Alert;
import com.elkassimi.monitoring_v2_0.service.AgentService;
import com.elkassimi.monitoring_v2_0.service.AlertService;
import com.elkassimi.monitoring_v2_0.service.DiscoveryService;
import com.elkassimi.monitoring_v2_0.service.InventoryService;
import com.elkassimi.monitoring_v2_0.service.LogService;
import com.elkassimi.monitoring_v2_0.service.MetricsService;
import com.elkassimi.monitoring_v2_0.service.RemoteCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Read-side / dashboard-facing API: everything a frontend uses to browse
 * agents and their history, plus queuing remote commands. Nothing here is
 * called by the Python agent itself.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QueryController {

    private final AgentService agentService;
    private final MetricsService metricsService;
    private final InventoryService inventoryService;
    private final DiscoveryService discoveryService;
    private final LogService logService;
    private final AlertService alertService;
    private final RemoteCommandService remoteCommandService;

    @GetMapping("/agents")
    public ResponseEntity<ApiResponse> listAgents() {
        return ResponseEntity.ok(ApiResponse.ok(agentService.findAll()));
    }

    @GetMapping("/agents/{agentId}")
    public ResponseEntity<ApiResponse> getAgent(@PathVariable String agentId) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.getByAgentId(agentId)));
    }

    @GetMapping("/agents/{agentId}/metrics")
    public ResponseEntity<ApiResponse> listMetrics(
            @PathVariable String agentId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(metricsService.listForAgent(agentId, pageable)));
    }

    @GetMapping("/agents/{agentId}/metrics/latest")
    public ResponseEntity<ApiResponse> latestMetrics(@PathVariable String agentId) {
        Optional<?> latest = metricsService.latestForAgent(agentId);
        return latest.<ResponseEntity<ApiResponse>>map(m -> ResponseEntity.ok(ApiResponse.ok(m)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null)));
    }

    @GetMapping("/agents/{agentId}/inventory/latest")
    public ResponseEntity<ApiResponse> latestInventory(@PathVariable String agentId) {
        Optional<?> latest = inventoryService.latestForAgent(agentId);
        return latest.<ResponseEntity<ApiResponse>>map(i -> ResponseEntity.ok(ApiResponse.ok(i)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null)));
    }

    @GetMapping("/agents/{agentId}/discovery")
    public ResponseEntity<ApiResponse> listDiscovery(
            @PathVariable String agentId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(discoveryService.listForAgent(agentId, pageable)));
    }

    @GetMapping("/agents/{agentId}/logs")
    public ResponseEntity<ApiResponse> listLogs(
            @PathVariable String agentId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(logService.listForAgent(agentId, pageable)));
    }

    @GetMapping("/agents/{agentId}/alerts")
    public ResponseEntity<ApiResponse> listAgentAlerts(
            @PathVariable String agentId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.listForAgent(agentId, pageable)));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse> listAlerts(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 50) Pageable pageable) {
        if (status == null || status.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(alertService.listAll(pageable)));
        }
        Alert.AlertStatus parsed = Alert.AlertStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(ApiResponse.ok(alertService.listByStatus(parsed, pageable)));
    }

    @PostMapping("/agents/{agentId}/commands")
    public ResponseEntity<ApiResponse> queueCommand(
            @PathVariable String agentId,
            @Valid @RequestBody CommandCreateRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(remoteCommandService.queue(agentId, dto)));
    }

    @GetMapping("/agents/{agentId}/commands")
    public ResponseEntity<ApiResponse> listCommands(
            @PathVariable String agentId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(remoteCommandService.listForAgent(agentId, pageable)));
    }
}
