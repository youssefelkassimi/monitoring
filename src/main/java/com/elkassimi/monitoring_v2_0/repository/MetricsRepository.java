package com.elkassimi.monitoring_v2_0.repository;

import com.elkassimi.monitoring_v2_0.model.Metrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricsRepository extends JpaRepository<Metrics, UUID> {

    Page<Metrics> findByAgent_AgentIdOrderByReceivedAtDesc(String agentId, Pageable pageable);

    Optional<Metrics> findFirstByAgent_AgentIdOrderByReceivedAtDesc(String agentId);
}
