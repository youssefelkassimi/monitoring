package com.elkassimi.monitoring_v2_0.repository;

import com.elkassimi.monitoring_v2_0.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {

    Optional<Agent> findByAgentId(String agentId);

    boolean existsByAgentId(String agentId);

    List<Agent> findByStatus(Agent.AgentStatus status);
}
