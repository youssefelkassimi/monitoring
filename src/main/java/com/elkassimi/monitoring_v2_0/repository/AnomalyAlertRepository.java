package com.elkassimi.monitoring_v2_0.repository;


import com.elkassimi.monitoring_v2_0.model.Alert;
import com.elkassimi.monitoring_v2_0.model.AnomalyAlertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnomalyAlertRepository extends JpaRepository<AnomalyAlertEntity, Long> {

    Optional<AnomalyAlertEntity> findFirstByAgentAgentIdAndStatusOrderByStartedAtDesc(
            String agentId, AnomalyAlertEntity.Status status);

    Page<AnomalyAlertEntity> findByStatus(AnomalyAlertEntity.Status status, Pageable pageable);

    Page<AnomalyAlertEntity> findByAgentAgentId(String agentId, Pageable pageable);

    Page<AnomalyAlertEntity> findByAgent_AgentIdOrderByStartedAtDesc(String agentId, Pageable pageable);

    Page<AnomalyAlertEntity> findAllByOrderByStartedAtDesc(Pageable pageable);

    Page<AnomalyAlertEntity> findByStatusOrderByStartedAtDesc(AnomalyAlertEntity.Status status, Pageable pageable);
}
