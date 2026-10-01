package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.AgentProvisionRequestDto;
import com.elkassimi.monitoring_v2_0.dto.AgentProvisionResponseDto;
import com.elkassimi.monitoring_v2_0.dto.HeartbeatRequestDto;
import com.elkassimi.monitoring_v2_0.dto.RegistrationRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Heartbeat;
import com.elkassimi.monitoring_v2_0.repository.AgentRepository;
import com.elkassimi.monitoring_v2_0.repository.HeartbeatRepository;
import com.elkassimi.monitoring_v2_0.scheduler.AgentLivenessCache;
import com.elkassimi.monitoring_v2_0.util.TimeUtil;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.expression.ExpressionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final AgentRepository agentRepository;
    private final HeartbeatRepository heartbeatRepository;
    private final RealTimePushService pushService;
    private final AgentLivenessCache livenessCache;
    private final JwtService jwtService;


    @Transactional
    @CacheEvict(value = "agents", allEntries = true)
    public AgentProvisionResponseDto provision(AgentProvisionRequestDto dto) {
        log.info("Provisioning new agent with label='{}', validityMinutes={}", dto.label(), dto.validityMinutes());

        String agentId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(dto.validityMinutes() * 60L);
        String token = jwtService.createAgentToken(agentId, expiresAt);

        Agent agent = Agent.builder()
                .agentId(agentId)
                .label(dto.label())
                .status(Agent.AgentStatus.OFFLINE)
                .provisioningStatus(Agent.ProvisioningStatus.PENDING)
                .tokenExpiresAt(expiresAt)
                .tokenHash(TokenHash.sha256(token))
                .build();

        Agent saved = agentRepository.save(agent);
        log.info("Agent provisioned successfully: agentId={}, label='{}', expiresAt={}", saved.getAgentId(), saved.getLabel(), expiresAt);

        pushService.pushAgents(saved);
        log.debug("Pushed provisioned agent to real-time clients: agentId={}", saved.getAgentId());

        return AgentProvisionResponseDto.builder()
                .id(saved.getAgentId())
                .label(saved.getLabel())
                .token(token)
                .tokenExpiresAt(expiresAt)
                .build();
    }

    @Transactional
    @CacheEvict(value = "agents", allEntries = true)
    public Agent register(RegistrationRequestDto dto) throws Exception {
        log.info("Registering agent: agentId={}, hostname='{}'", dto.getAgentId(), dto.getHostname());

        Optional<Agent> existingAgent = agentRepository.findByAgentId(dto.getAgentId());
        boolean newAgent = existingAgent.isEmpty();
        Agent agent = existingAgent
                .orElseGet(() -> {
                    log.warn("Agent not found for agentId={}, creating it during registration", dto.getAgentId());
                    return Agent.builder()
                            .agentId(dto.getAgentId())
                            .provisioningStatus(Agent.ProvisioningStatus.PENDING)
                            .build();
                });

        log.debug("Updating agent metadata for agentId={}: os={}, osVersion={}, architecture={}, pythonVersion={}",
                dto.getAgentId(), dto.getOs(), dto.getOsVersion(), dto.getArchitecture(), dto.getPythonVersion());

        agent.setHostname(dto.getHostname());
        agent.setOs(dto.getOs());
        agent.setOsVersion(dto.getOsVersion());
        agent.setArchitecture(dto.getArchitecture());
        agent.setPythonVersion(dto.getPythonVersion());
        agent.setStatus(Agent.AgentStatus.ONLINE);
        agent.setRegisteredAt(TimeUtil.fromEpochSeconds(dto.getRegisteredAt()));
        agent.setLastSeenAt(Instant.now());
        agent.setTokenExpiresAt(Instant.now().plus(40, ChronoUnit.DAYS));

        if (agent.getProvisioningStatus() == null || agent.getProvisioningStatus() == Agent.ProvisioningStatus.PENDING) {
            log.info("Activating agent provisioning status for agentId={}", dto.getAgentId());
            agent.setProvisioningStatus(Agent.ProvisioningStatus.ACTIVE);
        }


        Agent saved = agentRepository.save(agent);

        log.debug("pushAgentUpdate payload={}", saved);

        if (newAgent) {
            pushService.pushAgents(saved);
        } else {
            pushService.pushAgentUpdate(saved);
        }
        log.info("Agent registered successfully: agentId={}, status={}, provisioningStatus={}",
                saved.getAgentId(), saved.getStatus(), saved.getProvisioningStatus());
        return saved;
    }

    /** Finds the agent, creating a bare-bones record if it heartbeats before
     * a register call has landed (e.g. auto_register disabled on the agent). */
    @Transactional
    public Agent findOrCreate(String agentId) {
        log.debug("Looking up agent by agentId={}", agentId);
        return agentRepository.findByAgentId(agentId)
                .orElseGet(() -> {
                    log.warn("Agent not found for agentId={}, creating bare-bones record", agentId);
                    return agentRepository.save(
                            Agent.builder().agentId(agentId).status(Agent.AgentStatus.ONLINE).build());
                });
    }

    @Transactional
    @CacheEvict(value = "agents", allEntries = true)
    public void recordHeartbeat(HeartbeatRequestDto dto) {
        log.debug("Recording heartbeat for agentId={}, status='{}'", dto.getAgentId(), dto.getStatus());

        Agent agent = agentRepository.findByAgentId(dto.getAgentId())
                .orElseGet(() -> {
                    log.warn("Agent not found for agentId={}, creating bare-bones record", dto.getAgentId());
                    return Agent.builder().agentId(dto.getAgentId()).status(Agent.AgentStatus.ONLINE).build();
                });
        agent.setHostname(dto.getHostname());
        agent.setStatus("alive".equalsIgnoreCase(dto.getStatus())
                ? Agent.AgentStatus.ONLINE
                : Agent.AgentStatus.OFFLINE);
        agent.setLastSeenAt(Instant.now());
        agentRepository.save(agent);
        livenessCache.markAlive(dto.getAgentId());

        Instant ts = TimeUtil.fromEpochSeconds(dto.getTimestamp());
        Heartbeat heartbeat = Heartbeat.builder()
                .agent(agent)
                .hostname(dto.getHostname())
                .status(agent.getStatus())
                .timestamp(ts)
                .build();
        Heartbeat savedHeartbeat = heartbeatRepository.save(heartbeat);
        log.info("Heartbeat recorded: agentId={}, status={}, timestamp={}",
                dto.getAgentId(), agent.getStatus(), ts);

        pushService.pushHeartbeat(savedHeartbeat, dto.getAgentId());
        log.debug("Pushed heartbeat to real-time clients: agentId={}, heartbeatId={}",
                dto.getAgentId(), savedHeartbeat.getId());
    }


    /** Touches lastSeenAt without changing status - used by every other
     * ingest endpoint so "last seen" reflects any traffic, not just heartbeats. */
    @Transactional
    @CacheEvict(value = "agents", key = "#agentId")
    public Agent touch(String agentId) {
        log.debug("Touching agent lastSeenAt for agentId={}", agentId);
        Agent agent = findOrCreate(agentId);
        agent.setLastSeenAt(Instant.now());
        livenessCache.markAlive(agentId);
        Agent saved = agentRepository.save(agent);
        log.trace("Agent touched: agentId={}, lastSeenAt={}", agentId, saved.getLastSeenAt());
        return saved;
    }

    @Cacheable(value = "agents", key = "'findAll'")
    public List<Agent> findAll() {
        log.debug("Fetching all agents");
        List<Agent> agents = agentRepository.findAll();
        log.debug("Fetched {} agents", agents.size());
        return agents;
    }

    @Cacheable(value = "agents", key = "#agentId")
    public Agent getByAgentId(String agentId) {
        log.debug("Fetching agent by agentId={}", agentId);
        return agentRepository.findByAgentId(agentId)
                .orElseThrow(() -> {
                    log.warn("Agent lookup failed: unknown agentId={}", agentId);
                    return new NoSuchElementException("Unknown agent: " + agentId);
                });
    }

    public Agent getById(String id) {
        log.debug("Fetching agent by id={}", id);
        return agentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Agent lookup failed: unknown id={}", id);
                    return new NoSuchElementException("Unknown agent id: " + id);
                });
    }

    @Cacheable(value = "agents", key = "'status:' + #status")
    public List<Agent> findByStatus(Agent.AgentStatus status) {
        log.debug("Fetching agents by status={}", status);
        List<Agent> agents = agentRepository.findByStatus(status);
        log.debug("Fetched {} agents with status={}", agents.size(), status);
        return agents;
    }

    public long agentCount(){
        log.trace("Counting all agents");
        long count = agentRepository.count();
        log.debug("Total agent count={}", count);
        return count;
    }

    public long onlineAgentCount(){
        log.trace("Counting online agents");
        long count = findByStatus(Agent.AgentStatus.ONLINE).size();
        log.debug("Online agent count={}", count);
        return count;
    }

    @CacheEvict(value = "agents", key = "#agentId")
    public void toggleRevokeAgent(String agentId) throws Exception{
        log.info("toggle revoke for agent with id: {}",agentId);
        Agent agent = findOrThrow(agentId);
        Agent.ProvisioningStatus status =
                agent.getProvisioningStatus().equals(Agent.ProvisioningStatus.REVOKED) ?
                        Agent.ProvisioningStatus.PENDING : Agent.ProvisioningStatus.REVOKED;
        agent.setProvisioningStatus(status);
        Agent saved =agentRepository.save(agent);
        pushService.pushAgentUpdate(saved);

    }

    public Agent findOrThrow(String agentId) throws Exception{
        log.info("Get agent with id: {} from database",agentId);
        return agentRepository.findByAgentId(agentId)
                .orElseThrow(()->new Exception("agent not foud with id: "+agentId));
    }

}
