package com.elkassimi.monitoring_v2_0.websocket;

public final class Topics {

    public static final String METRICS = "/topic/metrics";
    public static final String SYSTEM = "/topic/system";
    public static final String SERVICE = "/topic/service";
    public static final String USERS = "/topic/users";
    public static final String USERS_UPDATE = "/topic/user_update";
    public static final String USERS_STATUS_UPDATE = "/topic/user_status_update";
    public static final String ALERTS = "/topic/alerts";
    public static final String HEARTBEATS = "/topic/heartbeats";
    public static final String AGENTS = "/topic/agents";
    public static final String AGENTS_UPDATE = "/topic/agents_UPDATE";
    public static final String DISCOVERY = "/topic/discovery";
    public static final String INVENTORY = "/topic/inventory";
    public static final String LOGS = "/topic/logs";
    public static final String COMMANDS = "/topic/commands";
    public static final String COMMAND_RESULT = "/topic/command_result";
    public static final String ANOMALY = "/topic/anomaly";

    /** Broadcast channel for admin-only dashboard events (user management,
     * RBAC audit feed...). Not access-controlled server-side - anyone who
     * subscribes receives it. Real "admin-only" delivery needs the security
     * layer (Principal per session) you're adding yourself; until then this
     * just keeps admin-facing events on their own channel so the frontend
     * can choose whether to subscribe based on the logged-in user's role. */
    public static final String ADMIN = "/topic/admin";

    private Topics() {}
}
