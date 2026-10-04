package com.elkassimi.monitoring_v2_0.websocket;


import com.elkassimi.monitoring_v2_0.dto.UpdateStatusDto;
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
    public  void pushAgentAlert(Object alert, String agentId){
        pushAgent(alert, Topics.ALERTS, agentId);
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
    public void pushUser(UserDto user){
        push(user,Topics.USERS);
    }
    public void pushService(Object service, String user){pushAgent(service,Topics.SERVICE, user);}
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

    public void pushCommand(Object command, String agent) {
        pushAgent(command, Topics.COMMANDS, agent);
    }

    public void pushCommandResult(Object result, String agent) {
        pushAgent(result, Topics.COMMAND_RESULT, agent);
    }

    public void pushCommandResultToUser(Object result, String agent, String userid) {
        pushToUser(result, Topics.COMMAND_RESULT + "/" + agent, userid );
    }


    public void pushAnomaly(Object anomaly, String agent) {
        pushAgent(anomaly, Topics.ANOMALY, agent);
    }



    public void pushToAdmins(Object message) {
        push(message, Topics.ADMIN);
    }


    public void pushToUser(String topic, String user, Object message) {
        pushToUser(message, topic, user);
    }


    public void pushUserStatusUpdate(UpdateStatusDto message) {
        push(message, Topics.USERS_STATUS_UPDATE);
    }
}