package com.elkassimi.monitoring_v2_0.repository;

import com.elkassimi.monitoring_v2_0.model.Heartbeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HeartbeatRepository extends JpaRepository<Heartbeat, UUID> {

    Optional<Heartbeat> findFirstByAgent_AgentIdOrderByTimestampDesc(String agentId);
}
