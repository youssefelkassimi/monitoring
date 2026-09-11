package com.elkassimi.monitoring_v2_0.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilTest {

    @Test
    void fromEpochSeconds_null_returnsApproximatelyNow() {
        Instant before = Instant.now();
        Instant result = TimeUtil.fromEpochSeconds(null);
        Instant after = Instant.now();

        assertThat(result).isBetween(before, after);
    }

    @Test
    void fromEpochSeconds_wholeNumber_convertsCorrectly() {
        Instant result = TimeUtil.fromEpochSeconds(1_700_000_000.0);
        assertThat(result).isEqualTo(Instant.ofEpochMilli(1_700_000_000_000L));
    }

    @Test
    void fromEpochSeconds_fractionalSeconds_convertsWithMillisecondPrecision() {
        Instant result = TimeUtil.fromEpochSeconds(1_700_000_000.123);
        assertThat(result).isEqualTo(Instant.ofEpochMilli(1_700_000_000_123L));
    }

    @Test
    void fromEpochSeconds_zero_returnsEpoch() {
        Instant result = TimeUtil.fromEpochSeconds(0.0);
        assertThat(result).isEqualTo(Instant.EPOCH);
    }
}
