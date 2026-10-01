package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.AnomalyAlert;
import com.elkassimi.monitoring_v2_0.dto.AnomalyEvent;
import com.elkassimi.monitoring_v2_0.dto.AnomalyResult;
import com.elkassimi.monitoring_v2_0.model.Alert;
import com.elkassimi.monitoring_v2_0.model.AnomalyAlertEntity;
import com.elkassimi.monitoring_v2_0.repository.AnomalyAlertRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
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
