package com.elkassimi.monitoring_v2_0.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** A single trigger-fired or trigger-recovered event, as produced by the
 * agent's TriggerEngine and posted to /api/alerts. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(name = "trigger_name")
    private String triggerName;

    private String severity;

    @Column(length = 1000)
    private String message;

    /** Dotted metrics key the trigger evaluated, e.g. "system.cpu.cpu_percent". */
    private String key;

    private Double value;

    private String operator;

    private Double threshold;

    @Column(name = "held_for_seconds")
    private Double heldForSeconds;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    /** When the agent's trigger engine evaluated this event. */
    private Instant timestamp;

    /** When this row was persisted on the backend. */
    @Column(name = "received_at")
    private Instant receivedAt;

    @PrePersist
    private void prePersist() {
        this.receivedAt = Instant.now();
    }

    public enum AlertStatus {
        PROBLEM, RECOVERY
    }
}
