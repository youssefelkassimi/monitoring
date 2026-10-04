package com.elkassimi.monitoring_v2_0.model;

import com.elkassimi.monitoring_v2_0.dto.AnomalyAlert;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(
        name = "anomaly_alerts",
        indexes = {
                @Index(name = "idx_anomaly_agent_status", columnList = "agent_id, status"),
                @Index(name = "idx_anomaly_started_at", columnList = "started_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AnomalyAlertEntity {

    public enum Status { FIRING, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.FIRING;

    @Column(nullable = false)
    private double score;

    @Column(name = "peak_score", nullable = false)
    private double peakScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_features", columnDefinition = "jsonb")
    private Map<String, Double> topFeatures;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(nullable = false)
    private boolean acknowledged = false;


    public void resolve(Instant at) {
        this.status = Status.RESOLVED;
        this.resolvedAt = at;
    }
}
