package com.elkassimi.monitoring_v2_0.websocket;


import com.elkassimi.monitoring_v2_0.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class RealTimePushService {

    private final SimpMessagingTemplate messaging;

    public void push(Object message, String topic){
        log.trace("WS push -> topic={}", topic);
        try {
            messaging.convertAndSend(topic, message);
        } catch (MessagingException e) {
            log.warn("WebSocket push failed on {}: {}", topic, e.getMessage());
        }
    }

    public void pushToUser(Object message, String topic, String user){
        log.trace("WS pushToUser -> topic={}, user={}", topic, user);
        try {
            messaging.convertAndSendToUser(user, topic, message);
        } catch (MessagingException e) {
            log.warn("WebSocket push failed on {} {}: {}", topic, user, e.getMessage());
        }
    }

    public void pushAgent(Object message, String topic, String agent){
        log.trace("WS pushAgent -> topic={}, agent={}", topic, agent);
        try {
            messaging.convertAndSend( topic + "/" + agent, message);
        } catch (MessagingException e) {
            log.warn("WebSocket push failed on {} {}: {}", topic, agent, e.getMessage());
        }
    }

    public void pushMetrics(Object metrics, String agent) {
        pushAgent(metrics, Topics.METRICS, agent);
    }

    public void pushAlert(Object alert) {
        push(alert, Topics.ALERTS);
    }

    public void pushHeartbeat(Object heartbeat, String agent) {
        pushAgent(heartbeat, Topics.HEARTBEATS, agent);
    }

    public void pushAgents(Object agents) {
        push(agents, Topics.AGENTS);
    }

    public void pushAgentUpdate(Object agent){
        push(agent,Topics.AGENTS_UPDATE);
    }

    public void pushSystem(Object system, String agent){
        pushAgent(system,Topics.SYSTEM,agent );
    }
    public void pushUser(Object user){
        push(user,Topics.USERS);
    }
    public void pushService(Object service, String user){
        pushAgent(service,Topics.SERVICE, user);
    }
    public void pushChecks(Object checks, String agent){
        pushAgent(checks,Topics.SERVICE, agent);
    }

    public void pushUserUpdate(Object userUpdate, String user){
        pushToUser(userUpdate,Topics.USERS_UPDATE, user);
    }



    public void pushDiscovery(Object discovery, String agent) {
        pushAgent(discovery, Topics.DISCOVERY, agent);
    }

    public void pushInventory(Object inventory, String agent) {
        pushAgent(inventory, Topics.INVENTORY, agent);
    }

    public void pushLogs(Object logs, String agent) {
        pushAgent(logs, Topics.LOGS, agent);
    }

    /** A command was queued for an agent (used to refresh "Recent RPC Executions"). */
    public void pushCommand(Object command, String agent) {
        pushAgent(command, Topics.COMMANDS, agent);
    }

    /** An agent reported back a command result. */
    public void pushCommandResult(Object result, String agent) {
        pushAgent(result, Topics.COMMAND_RESULT, agent);
    }

    public void pushAnomaly(Object anomaly, String agent) {
        pushAgent(anomaly, Topics.ANOMALY, agent);
    }

    /** Broadcasts to Topics.ADMIN - see the javadoc there for what this
     * does and doesn't guarantee without a real auth layer. */
    public void pushToAdmins(Object message) {
        push(message, Topics.ADMIN);
    }

    // ------------------------------------------------------------------
    // Additions (not present before). Remove if you don't want them.
    // ------------------------------------------------------------------

    /**
     * Existence check for a user/topic pair, mirroring the other primitives.
     * Not currently used by any caller in the classes I've seen, but handy
     * if you ever want to fan out an update to a specific user's own topic
     * (e.g. their ALERTS tab) rather than a dashboard-wide broadcast.
     */
    public void pushToUser(String topic, String user, Object message) {
        pushToUser(message, topic, user);
    }

    /**
     * Push a payload to every subscriber of an arbitrary topic. Thin wrapper
     * so callers don't have to remember argument order of the private push.
     */
    public void pushToTopic(String topic, Object message) {
        push(message, topic);
    }
}