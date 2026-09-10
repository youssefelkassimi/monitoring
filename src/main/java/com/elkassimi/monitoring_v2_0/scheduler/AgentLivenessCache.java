package com.elkassimi.monitoring_v2_0.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Redis-backed "is this agent still alive" tracker. Since agent status
 * rarely flips, this avoids hitting Postgres on every heartbeat/metrics
 * call - each touch just refreshes a TTL'd key in Redis, and Postgres is
 * only written to when an agent actually goes stale (see
 * AgentLivenessScheduler). Uses Spring Boot's auto-configured
 * StringRedisTemplate.
 */
@Component
@RequiredArgsConstructor
public class AgentLivenessCache {

    private static final String ALIVE_KEY_PREFIX = "agent:alive:";
    private static final String ONLINE_SET_KEY = "agent:online";

    private final StringRedisTemplate redis;

    @Value("${app.agent.timeout-seconds:60}")
    private long timeoutSeconds;

    /** Call this on every heartbeat/metrics/inventory/... call for an agent. */
    public void markAlive(String agentId) {
        redis.opsForValue().set(aliveKey(agentId), "1", Duration.ofSeconds(timeoutSeconds));
        redis.opsForSet().add(ONLINE_SET_KEY, agentId);
    }

    public boolean isAlive(String agentId) {
        return Boolean.TRUE.equals(redis.hasKey(aliveKey(agentId)));
    }

    /** Every agentId currently tracked as online - the scheduler only needs
     * to check these, not the whole agents table. */
    public Set<String> onlineAgentIds() {
        Set<String> members = redis.opsForSet().members(ONLINE_SET_KEY);
        return members != null ? members : Set.of();
    }

    /** Stop watching this agent (its DB row has just been flipped to OFFLINE). */
    public void markOffline(String agentId) {
        redis.opsForSet().remove(ONLINE_SET_KEY, agentId);
        redis.delete(aliveKey(agentId));
    }

    private String aliveKey(String agentId) {
        return ALIVE_KEY_PREFIX + agentId;
    }
}
