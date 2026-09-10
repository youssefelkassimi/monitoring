package com.elkassimi.monitoring_v2_0.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "cpu_percent")
    private Double cpuPercent;

    @Column(name = "memory_percent")
    private Double memoryPercent;

    @Column(name = "load_avg")
    private Double loadAvg;

    @Column(name = "process_count")
    private Integer processCount;

    /** Raw "system" block (cpu/memory/disk/network/...) as sent by the agent. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "system_json", nullable = false)
    private String system;

    /** Raw "checks" block (dns/http/icmp/service checks) as sent by the agent. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "checks_json", nullable = false)
    private String checks;

    /** Raw "plugins" block, present only when the agent has plugins enabled. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "plugins_json")
    private String plugins;

    /** Raw "alerts_summary" block ({total_alerts, recoveries}) for this cycle. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "alerts_summary_json")
    private String alertsSummary;

    @PrePersist
    private void prePersist() {
        this.receivedAt = Instant.now();
    }
}
