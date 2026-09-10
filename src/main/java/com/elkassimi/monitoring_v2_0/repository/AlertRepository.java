package com.elkassimi.monitoring_v2_0.repository;

import com.elkassimi.monitoring_v2_0.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findByAgent_AgentIdOrderByReceivedAtDesc(String agentId, Pageable pageable);

    Page<Alert> findByStatusOrderByReceivedAtDesc(Alert.AlertStatus status, Pageable pageable);

    Page<Alert> findAllByOrderByReceivedAtDesc(Pageable pageable);
}
