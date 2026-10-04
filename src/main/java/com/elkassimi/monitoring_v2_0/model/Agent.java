package com.elkassimi.monitoring_v2_0.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Agent {

    @Id
    @Column(name = "agent_id", nullable = false, unique = true)
    private String agentId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 7)
    private User.role role = User.role.AGENT;

    private String label;

    private String hostname;
    private String os;

    @Column(name = "os_version")
    private String osVersion;

    private String architecture;

    @Column(name = "python_version")
    private String pythonVersion;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AgentStatus status = AgentStatus.ONLINE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "provisioning_status")
    private ProvisioningStatus provisioningStatus = ProvisioningStatus.PENDING;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "registered_at")
    private Instant registeredAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    public enum AgentStatus {
        ONLINE, OFFLINE
    }

    public enum ProvisioningStatus {
        PENDING, ACTIVE, EXPIRED, REVOKED
    }
}
