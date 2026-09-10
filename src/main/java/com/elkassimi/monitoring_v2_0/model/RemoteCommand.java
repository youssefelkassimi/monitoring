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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** A command queued for an agent's remote_commands poller
 * (remote/commands.py -> RemoteCommandHandler, allow-listed on the agent side). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoteCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    /** Bare command name; the agent only executes it if it's on its own
     * config.yaml allow list, matched by basename. */
    @Column(nullable = false)
    private String command;

    /** Arguments as a JSON array string, e.g. ["-c","3"]. Nullable - many
     * allow-listed commands (uptime, free, hostname) take none. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "args_json")
    private String argsJson;

    @Builder.Default
    private Integer timeout = 30;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CommandStatus status = CommandStatus.PENDING;

    @Column(name = "created_at")
    private Instant createdAt;

    /** When the backend handed this command to the agent on a poll. */
    @Column(name = "sent_at")
    private Instant sentAt;

    /** When the agent reported a result back. */
    @Column(name = "executed_at")
    private Instant executedAt;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String stdout;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String stderr;

    @Column(name = "exit_code")
    private Integer exitCode;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public enum CommandStatus {
        PENDING, SENT, SUCCESS, ERROR, TIMEOUT, REJECTED
    }
}
