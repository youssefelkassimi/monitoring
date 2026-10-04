package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.AlertEventDto;
import com.elkassimi.monitoring_v2_0.dto.AnomalyAlert;
import com.elkassimi.monitoring_v2_0.dto.AnomalyEvent;
import com.elkassimi.monitoring_v2_0.dto.AnomalyResult;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Alert;
import com.elkassimi.monitoring_v2_0.model.AnomalyAlertEntity;
import com.elkassimi.monitoring_v2_0.repository.AnomalyAlertRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AnomalyService {

    private final RestClient client;
    private final int confirm;
    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final RealTimePushService pushService;
    private final AnomalyAlertRepository alertRepository;
    private final AgentService agentService;

    private static class State {int hits, ok ; boolean active;}

    public AnomalyService(RestClient anomalyRestClient, @Value("${anomaly.confirm}") int confirm, RealTimePushService pushService, AnomalyAlertRepository alertRepository, AgentService agentService) {
        this.client = anomalyRestClient;
        this.confirm = confirm;
        this.pushService = pushService;
        this.alertRepository = alertRepository;
        this.agentService = agentService;
    }

    @Async
    @EventListener
    public void evaluate(AnomalyEvent event) throws Exception {
        AnomalyResult result ;
        try{
            result = client.post()
                    .uri(u-> u.path("/check").queryParam("host_id", event.agentId()).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event.metrics())
                    .retrieve()
                    .body(AnomalyResult.class);
        }catch (RestClientException e){
            log.warn("Anomaly API unavailable: {}", e.getMessage());
            return ;
        }
        if(result == null) return ;

        log.error("anomal: {}", result);
        State state = states.computeIfAbsent(event.agentId(), k->new State());



        synchronized (state){
            if(result.anomaly()){
                state.hits++;
                state.ok = 0;
            }
            else{
                state.ok++;
                state.hits = 0;
            }
            if(!state.active && state.hits >= confirm){
                state.active = true;
                var anomaly = new AnomalyAlert(event.agentId(), "FIRING", result.score(),result.topFutures(), Instant.now());
                alertRepository.save(from(anomaly));
                pushService.pushAnomaly(anomaly, event.agentId());
                return ;
            }
            if(state.active && state.ok >= confirm){
                state.active = false;
                var anomaly = new AnomalyAlert(event.agentId(), "RESOLVED", result.score(), Map.of(),Instant.now());
                alertRepository.save(from(anomaly));
                pushService.pushAnomaly(anomaly, event.agentId());
            }
        }
    }

    public Page<AnomalyAlertEntity> listForAgent(String agentId, Pageable pageable) {
        log.debug("Listing alerts for agentId={}, pageable={}", agentId, pageable);
        Page<AnomalyAlertEntity> page = alertRepository.findByAgent_AgentIdOrderByStartedAtDesc(agentId, pageable);
        log.debug("Listed {} alerts (total={}) for agentId={}",
                page.getNumberOfElements(), page.getTotalElements(), agentId);
        return page;
    }

    public Page<AnomalyAlertEntity> listAll(Pageable pageable) {
        log.debug("Listing all alerts, pageable={}", pageable);
        Page<AnomalyAlertEntity> page = alertRepository.findAllByOrderByStartedAtDesc(pageable);
        log.debug("Listed {} alerts (total={})", page.getNumberOfElements(), page.getTotalElements());
        return page;
    }

    public Page<AnomalyAlertEntity> listByStatus(AnomalyAlertEntity.Status status, Pageable pageable) {
        log.debug("Listing AnomalyAlertEntitys by status={}, pageable={}", status, pageable);
        Page<AnomalyAlertEntity> page = alertRepository.findByStatusOrderByStartedAtDesc(status, pageable);
        log.debug("Listed {} alerts (total={}) with status={}",
                page.getNumberOfElements(), page.getTotalElements(), status);
        return page;
    }

    private AnomalyAlertEntity from(AnomalyAlert alert) throws Exception {
        return  AnomalyAlertEntity.builder()
                .agent(agentService.findOrThrow(alert.agentId()))
                .score(alert.score())
                .peakScore(alert.score())
                .status(AnomalyAlertEntity.Status.valueOf(alert.status()))
                .topFeatures(alert.topFeatures())
                .startedAt(alert.at())
                .build();
    }




}
