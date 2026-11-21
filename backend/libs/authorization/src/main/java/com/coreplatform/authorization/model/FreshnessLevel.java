package com.coreplatform.authorization.model;

import java.time.Duration;

public enum FreshnessLevel {

    LOW(Duration.ofSeconds(30)),
    MEDIUM(Duration.ofSeconds(5)),
    HIGH(Duration.ofSeconds(5)),
    CRITICAL(Duration.ZERO);

    private final Duration maxStaleness;

    FreshnessLevel(Duration maxStaleness) {
        this.maxStaleness = maxStaleness;
    }

    public Duration getMaxStaleness() {
        return maxStaleness;
    }
}
