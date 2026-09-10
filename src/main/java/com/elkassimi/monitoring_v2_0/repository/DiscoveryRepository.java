package com.elkassimi.monitoring_v2_0.repository;

import com.elkassimi.monitoring_v2_0.model.Discovery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DiscoveryRepository extends JpaRepository<Discovery, UUID> {

    Page<Discovery> findByAgent_AgentIdOrderByTimestampDesc(String agentId, Pageable pageable);
}
