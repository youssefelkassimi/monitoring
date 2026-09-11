package com.elkassimi.monitoring_v2_0.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentLivenessCacheTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private SetOperations<String, String> setOps;

    private AgentLivenessCache cache;

    @BeforeEach
    void setUp() {
        cache = new AgentLivenessCache(redis);
        // @Value has no effect on a plain `new` instance - set it directly,
        // matching the default from application.yaml (app.agent.timeout-seconds).
        ReflectionTestUtils.setField(cache, "timeoutSeconds", 60L);
    }

    @Test
    void markAlive_setsTtlKeyAndAddsToOnlineSet() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);

        cache.markAlive("agt-1");

        verify(valueOps).set(eq("agent:alive:agt-1"), eq("1"), eq(Duration.ofSeconds(60)));
        verify(setOps).add("agent:online", "agt-1");
    }

    @Test
    void isAlive_keyPresent_returnsTrue() {
        when(redis.hasKey("agent:alive:agt-2")).thenReturn(true);

        assertThat(cache.isAlive("agt-2")).isTrue();
    }

    @Test
    void isAlive_keyAbsent_returnsFalse() {
        when(redis.hasKey("agent:alive:agt-3")).thenReturn(false);

        assertThat(cache.isAlive("agt-3")).isFalse();
    }

    @Test
    void isAlive_hasKeyReturnsNull_returnsFalse() {
        when(redis.hasKey("agent:alive:agt-4")).thenReturn(null);

        assertThat(cache.isAlive("agt-4")).isFalse();
    }

    @Test
    void onlineAgentIds_returnsSetMembers() {
        when(redis.opsForSet()).thenReturn(setOps);
        when(setOps.members("agent:online")).thenReturn(Set.of("agt-1", "agt-2"));

        assertThat(cache.onlineAgentIds()).containsExactlyInAnyOrder("agt-1", "agt-2");
    }

    @Test
    void onlineAgentIds_nullMembers_returnsEmptySet() {
        when(redis.opsForSet()).thenReturn(setOps);
        when(setOps.members("agent:online")).thenReturn(null);

        assertThat(cache.onlineAgentIds()).isEmpty();
    }

    @Test
    void markOffline_removesFromSetAndDeletesTtlKey() {
        when(redis.opsForSet()).thenReturn(setOps);

        cache.markOffline("agt-5");

        verify(setOps).remove("agent:online", "agt-5");
        verify(redis).delete("agent:alive:agt-5");
    }
}
