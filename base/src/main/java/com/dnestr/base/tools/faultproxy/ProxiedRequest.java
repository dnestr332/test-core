package com.dnestr.base.tools.faultproxy;

import java.time.Instant;

public record ProxiedRequest(Instant at, String method, String path, Outcome outcome) {

    public enum Outcome {
        FORWARDED,
        RESPONDED,
        RESPONSE_DROPPED,
        REFUSED,
        UPSTREAM_DOWN;

        /** Whether the server saw the request. */
        public boolean reachedServer() {
            return this == FORWARDED || this == RESPONSE_DROPPED;
        }
    }

    public boolean matches(String method, String path) {
        return (method == null || method.equalsIgnoreCase(this.method)) && FaultRule.pathPattern(path).matcher(this.path).find();
    }
}
