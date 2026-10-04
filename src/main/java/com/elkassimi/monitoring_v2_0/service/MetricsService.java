package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.AnomalyEvent;
import com.elkassimi.monitoring_v2_0.dto.MetricsRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Metrics;
import com.elkassimi.monitoring_v2_0.repository.MetricsRepository;
import com.elkassimi.monitoring_v2_0.util.JsonUtil;
import com.elkassimi.monitoring_v2_0.util.TimeUtil;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MetricsRepository metricsRepository;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;
    private  final RealTimePushService pushService;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public Metrics save(MetricsRequestDto dto) {
        log.debug("Saving metrics for agentId={}, sentAt={}", dto.getAgentId(), dto.getSentAt());

        Agent agent = agentService.touch(dto.getAgentId());
        log.trace("Touched agent before metrics save: agentId={}", agent.getAgentId());

        Metrics metrics = Metrics.builder()
                .agent(agent)
                .sentAt(TimeUtil.fromEpochSeconds(dto.getSentAt()))
                .cpuPercent(nestedDouble(dto.getSystem(), "cpu", "cpu_percent"))
                .memoryPercent(nestedDouble(dto.getSystem(), "memory", "percent"))
                .loadAvg(firstLoadAvg(dto.getSystem()))
                .processCount(nestedInteger(dto.getSystem(), "processes", "total_count"))
                .system(JsonUtil.toJson(objectMapper, dto.getSystem()))
                .checks(JsonUtil.toJson(objectMapper, dto.getChecks()))
                .plugins(JsonUtil.toJson(objectMapper, dto.getPlugins()))
                .alertsSummary(JsonUtil.toJson(objectMapper, dto.getAlertsSummary()))
                .build();

        log.debug("Parsed metrics for agentId={}: cpuPercent={}, memoryPercent={}, loadAvg={}, processCount={}",
                dto.getAgentId(), metrics.getCpuPercent(), metrics.getMemoryPercent(),
                metrics.getLoadAvg(), metrics.getProcessCount());

        if (metrics.getSystem() == null) {
            log.warn("System JSON was null for agentId={}, defaulting to empty object", dto.getAgentId());
            metrics.setSystem("{}");
        }
        if (metrics.getChecks() == null) {
            log.warn("Checks JSON was null for agentId={}, defaulting to empty object", dto.getAgentId());
            metrics.setChecks("{}");
        }

        Metrics saved = metricsRepository.save(metrics);
        log.info("Metrics saved: id={}, agentId={}", saved.getId(), saved.getAgent().getAgentId());

        pushService.pushMetrics(saved,saved.getAgent().getAgentId());
        pushService.pushChecks(saved.getChecks(), saved.getAgent().getAgentId());
        pushService.pushSystem(saved.getSystem(), saved.getAgent().getAgentId());
        log.debug("Pushed metrics/checks/services/system updates for agentId={}", saved.getAgent().getAgentId());

        try{
            Map<String, Object> metric = Map.of(
               "agnet.id" , saved.getAgent().getAgentId(),
               "system.cpu.cpu_percent", saved.getCpuPercent(),
               "system.memory.percent", saved.getMemoryPercent(),
               "system.disk_usage.0.percent", FistDiskPercent(dto.getSystem()),
               "system.network.total_errin", nestedInteger(dto.getSystem(),"network", "total_errin"),
               "system.processes.total_count", saved.getProcessCount(),
               "system.cpu.load_avg.0", saved.getLoadAvg(),
               "system.memory.swap_percent", nestedDouble(dto.getSystem(), "memory", "swap_percent")
            );
            publisher.publishEvent(new AnomalyEvent(saved.getAgent().getAgentId(), metric));
        }catch(NullPointerException e){}
        return saved;
    }

    public Page<Metrics> listForAgent(String agentId, Pageable pageable) {
        log.debug("Listing metrics for agentId={}, pageable={}", agentId, pageable);
        Page<Metrics> page = metricsRepository.findByAgent_AgentIdOrderByReceivedAtDesc(agentId, pageable);
        log.debug("Listed {} metrics (total={}) for agentId={}", page.getNumberOfElements(), page.getTotalElements(), agentId);
        return page;
    }

    public Optional<Metrics> latestForAgent(String agentId) {
        log.debug("Fetching latest metrics for agentId={}", agentId);
        Optional<Metrics> latest = metricsRepository.findFirstByAgent_AgentIdOrderByReceivedAtDesc(agentId);
        if (latest.isEmpty()) {
            log.debug("No metrics found for agentId={}", agentId);
        } else {
            log.trace("Latest metrics for agentId={}: id={}, receivedAt={}", agentId, latest.get().getId(), latest.get().getReceivedAt());
        }
        return latest;
    }

    @SuppressWarnings("unchecked")
    private Double nestedDouble(Map<String, Object> system, String block, String field) {
        if (system == null) {
            log.trace("nestedDouble: system map is null for block={}, field={}", block, field);
            return null;
        }
        Object nested = system.get(block);
        if (!(nested instanceof Map)) {
            log.trace("nestedDouble: block '{}' is missing or not a Map (field={})", block, field);
            return null;
        }
        Object value = ((Map<String, Object>) nested).get(field);
        Double result = toDouble(value);
        log.trace("nestedDouble: block={}, field={}, rawValue={}, parsed={}", block, field, value, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Integer nestedInteger(Map<String, Object> system, String block, String field) {
        Double d = nestedDouble(system, block, field);
        Integer result = d == null ? null : d.intValue();
        log.trace("nestedInteger: block={}, field={}, parsed={}", block, field, result);
        return result;
    }

    private Double firstLoadAvg(Map<String, Object> system) {
        if (system == null) {
            return null;
        }
        // 1. top-level load_avg
        Double result = firstOfList(system.get("load_avg"));
        if (result != null) {
            return result;
        }
        // 2. nested under cpu
        Object cpu = system.get("cpu");
        if (cpu instanceof Map<?, ?> cpuMap) {
            result = firstOfList(cpuMap.get("load_avg"));
        }
        log.trace("firstLoadAvg: nested cpu.load_avg first element = {}", result);
        return result;
    }

    private Double firstOfList(Object value) {
        if (value instanceof List<?> list && !list.isEmpty()) {
            return toDouble(list.get(0));
        }
        return null;
    }

    private Double FistDiskPercent(Map<String, Object> system){
        if (system == null) {
            log.trace("FistDiskPercent: system map is null");
            return null;
        }
        Object diskUsage = system.get("disk_usage");
        if (diskUsage instanceof List<?> list && !list.isEmpty()) {
            Object firstDisk = list.get(0);
            if(firstDisk instanceof Map){
                Double result = toDouble( ((Map<?, ?>) firstDisk).get("percent"));
                log.info("FistDiskPercent: parsed from top-level disk_usage list = {}", result);
                return  result;
            }
            return null;
        }
        return  null;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            log.trace("toDouble: value is not a Number, class={}, value={}", value.getClass().getName(), value);
        }
        return null;
    }
}
