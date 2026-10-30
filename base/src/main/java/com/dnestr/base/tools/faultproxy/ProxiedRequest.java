package com.dnestr.base.tools.faultproxy;

import java.time.Instant;

/** One request the proxy saw, and what it did with it. */
public record ProxiedRequest(Instant at, String method, String path, Outcome outcome) {

    /** What the proxy did with a request. */
    public enum Outcome {
        /** Passed to the server, and its response passed back. */
        FORWARDED,
        /** Answered by the proxy with the rule's response; the server never saw it. */
        RESPONDED,
        /** Passed to the server, then its response withheld and the connection reset. */
        RESPONSE_DROPPED,
        /** The connection reset without the request reaching the server. */
        REFUSED,
        /** The server could not be reached, so the connection was reset. */
        UPSTREAM_DOWN;

        /** Whether the server saw the request. */
        public boolean reachedServer() {
            return this == FORWARDED || this == RESPONSE_DROPPED;
        }
    }

    /** Whether this request has {@code method} (any, when null) and a path ending in {@code path}, as a {@link FaultRule} matches it. */
    public boolean matches(String method, String path) {
        return (method == null || method.equalsIgnoreCase(this.method)) && FaultRule.pathPattern(path).matcher(this.path).find();
    }
}
