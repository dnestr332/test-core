package com.dnestr.base.tools.faultproxy;

import java.time.Duration;

/** What the proxy does with a request a {@link FaultRule} matched. */
public sealed interface Fault {

    /** Answers in place of the server, which never sees the request. */
    record Respond(int status, String body) implements Fault {
    }

    /** Lets the server handle the request, then closes the connection without the response. */
    record DropResponse() implements Fault {
    }

    /** Resets the connection before the request reaches the server. */
    record Refuse() implements Fault {
    }

    /** Holds the request for a while, then forwards it. */
    record Delay(Duration duration) implements Fault {
    }
}
