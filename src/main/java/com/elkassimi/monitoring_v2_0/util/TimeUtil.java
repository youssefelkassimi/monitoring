package com.elkassimi.monitoring_v2_0.util;

import java.time.Instant;

/** The Python agent sends timestamps as raw epoch-seconds floats
 * (time.time()), not ISO strings - this centralizes the conversion. */
public final class TimeUtil {

    private TimeUtil() {
    }

    public static Instant fromEpochSeconds(Double epochSeconds) {
        if (epochSeconds == null) {
            return Instant.now();
        }
        long millis = Math.round(epochSeconds * 1000.0);
        return Instant.ofEpochMilli(millis);
    }
}
