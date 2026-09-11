package com.elkassimi.monitoring_v2_0.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RealTimePushServiceTest {

    @Mock
    private SimpMessagingTemplate messaging;

    @InjectMocks
    private RealTimePushService pushService;

    @Test
    void push_sendsToTopic() {
        Object payload = new Object();
        pushService.push(payload, "/topic/whatever");
        verify(messaging).convertAndSend("/topic/whatever", payload);
    }

    @Test
    void push_swallowsMessagingException() {
        doThrow(new MessagingException("boom") {})
                .when(messaging).convertAndSend(eq("/topic/whatever"), org.mockito.ArgumentMatchers.any(Object.class));

        // must not throw
        pushService.push(new Object(), "/topic/whatever");
    }

    @Test
    void pushToUser_delegatesToConvertAndSendToUser() {
        Object payload = new Object();
        pushService.pushToUser(payload, "/topic/x", "user-1");
        verify(messaging).convertAndSendToUser("user-1", "/topic/x", payload);
    }

    @Test
    void pushToUser_swallowsMessagingException() {
        doThrow(new MessagingException("boom") {})
                .when(messaging).convertAndSendToUser(eq("user-1"), eq("/topic/x"), org.mockito.ArgumentMatchers.any(Object.class));

        pushService.pushToUser(new Object(), "/topic/x", "user-1");
    }

    @Test
    void pushAgent_sendsToTopicSlashAgentId() {
        Object payload = new Object();
        pushService.pushAgent(payload, Topics.METRICS, "agt-1");
        verify(messaging).convertAndSend("/topic/metrics/agt-1", payload);
    }

    @Test
    void pushAgent_swallowsMessagingException() {
        doThrow(new MessagingException("boom") {})
                .when(messaging).convertAndSend(eq("/topic/metrics/agt-1"), org.mockito.ArgumentMatchers.any(Object.class));

        pushService.pushAgent(new Object(), Topics.METRICS, "agt-1");
    }

    @Test
    void pushMetrics_usesMetricsTopicPerAgent() {
        Object payload = new Object();
        pushService.pushMetrics(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.METRICS + "/agt-1", payload);
    }

    @Test
    void pushAlert_broadcastsOnAlertsTopic() {
        Object payload = new Object();
        pushService.pushAlert(payload);
        verify(messaging).convertAndSend(Topics.ALERTS, payload);
    }

    @Test
    void pushHeartbeat_usesHeartbeatsTopicPerAgent() {
        Object payload = new Object();
        pushService.pushHeartbeat(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.HEARTBEATS + "/agt-1", payload);
    }

    @Test
    void pushAgents_broadcastsOnAgentsTopic() {
        Object payload = new Object();
        pushService.pushAgents(payload);
        verify(messaging).convertAndSend(Topics.AGENTS, payload);
    }

    @Test
    void pushAgentUpdate_broadcastsOnAgentsUpdateTopic() {
        Object payload = new Object();
        pushService.pushAgentUpdate(payload);
        verify(messaging).convertAndSend(Topics.AGENTS_UPDATE, payload);
    }

    @Test
    void pushSystem_usesSystemTopicPerAgent() {
        Object payload = new Object();
        pushService.pushSystem(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.SYSTEM + "/agt-1", payload);
    }

    @Test
    void pushUser_broadcastsOnUsersTopic() {
        Object payload = new Object();
        pushService.pushUser(payload);
        verify(messaging).convertAndSend(Topics.USERS, payload);
    }

    @Test
    void pushServiceMethod_usesServiceTopicPerAgent() {
        Object payload = new Object();
        pushService.pushService(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.SERVICE + "/agt-1", payload);
    }

    @Test
    void pushChecks_usesServiceTopicPerAgent() {
        Object payload = new Object();
        pushService.pushChecks(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.SERVICE + "/agt-1", payload);
    }

    @Test
    void pushUserUpdate_sendsToUserOnUsersUpdateTopic() {
        Object payload = new Object();
        pushService.pushUserUpdate(payload, "user-1");
        verify(messaging).convertAndSendToUser("user-1", Topics.USERS_UPDATE, payload);
    }

    @Test
    void pushDiscovery_usesDiscoveryTopicPerAgent() {
        Object payload = new Object();
        pushService.pushDiscovery(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.DISCOVERY + "/agt-1", payload);
    }

    @Test
    void pushInventory_usesInventoryTopicPerAgent() {
        Object payload = new Object();
        pushService.pushInventory(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.INVENTORY + "/agt-1", payload);
    }

    @Test
    void pushLogs_usesLogsTopicPerAgent() {
        Object payload = new Object();
        pushService.pushLogs(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.LOGS + "/agt-1", payload);
    }

    @Test
    void pushCommand_usesCommandsTopicPerAgent() {
        Object payload = new Object();
        pushService.pushCommand(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.COMMANDS + "/agt-1", payload);
    }

    @Test
    void pushCommandResult_usesCommandResultTopicPerAgent() {
        Object payload = new Object();
        pushService.pushCommandResult(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.COMMAND_RESULT + "/agt-1", payload);
    }

    @Test
    void pushAnomaly_usesAnomalyTopicPerAgent() {
        Object payload = new Object();
        pushService.pushAnomaly(payload, "agt-1");
        verify(messaging).convertAndSend(Topics.ANOMALY + "/agt-1", payload);
    }

    @Test
    void pushToAdmins_broadcastsOnAdminTopic() {
        Object payload = new Object();
        pushService.pushToAdmins(payload);
        verify(messaging).convertAndSend(Topics.ADMIN, payload);
    }
}
