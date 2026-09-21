package com.elkassimi.monitoring_v2_0.repository;

import com.elkassimi.monitoring_v2_0.model.RemoteCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RemoteCommandRepository extends JpaRepository<RemoteCommand, UUID> {

    List<RemoteCommand> findByAgent_AgentIdAndStatus(String agentId, RemoteCommand.CommandStatus status);

    Page<RemoteCommand> findByAgent_AgentIdOrderByCreatedAtDesc(String agentId, Pageable pageable);

    @Query("select rc.user.id from RemoteCommand rc where rc.id = :id")
    Optional<String> getUseId(@Param("id") UUID id);
}
