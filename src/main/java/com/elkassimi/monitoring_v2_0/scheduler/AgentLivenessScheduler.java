package com.elkassimi.monitoring_v2_0.scheduler;

import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.repository.AgentRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Periodically sweeps for agents that have gone quiet and flips them
 * OFFLINE. Backed by AgentLivenessCache (Redis) rather than a Postgres
 * scan: every tick is a set of Redis reads, and Postgres is only written
 * to for the agents that actually went stale.
 *
 * Requires @EnableScheduling on Application (added) and
 * livenessCache.markAlive(agentId) called from AgentService.touch()/
 * recordHeartbeat() (also wired in this pass).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentLivenessScheduler {

    private final AgentRepository agentRepository;
    private final AgentLivenessCache livenessCache;
    private final RealTimePushService pushService;

    @Scheduled(fixedRateString = "${app.agent.liveness-check-ms:15000}")
    @Transactional
    @CacheEvict(value = "agents", allEntries = true)
    public void markStaleAgentsOffline() {
        Set<String> trackedOnline = livenessCache.onlineAgentIds();
        if (trackedOnline.isEmpty()) {
            return;
        }

        List<Agent> newlyOffline = new ArrayList<>();

        for (String agentId : trackedOnline) {
            if (livenessCache.isAlive(agentId)) {
                continue; // still within the TTL window - nothing to do
            }

            agentRepository.findByAgentId(agentId).ifPresent(agent -> {
                if (agent.getStatus() == Agent.AgentStatus.ONLINE) {
                    agent.setStatus(Agent.AgentStatus.OFFLINE);
                    newlyOffline.add(agentRepository.save(agent));
                }
            });
            livenessCache.markOffline(agentId);
        }

        if (newlyOffline.isEmpty()) {
            return;
        }

        log.info("Marked {} agent(s) offline after going silent", newlyOffline.size());
        newlyOffline.forEach(pushService::pushAgentUpdate);
        pushService.pushAgents(newlyOffline);
    }
}
