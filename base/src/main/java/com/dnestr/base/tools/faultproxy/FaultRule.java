package com.dnestr.base.tools.faultproxy;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Applies {@link #fault()} to the requests whose method and path match, {@code times} times or, with
 * {@link #ALWAYS}, until the rules are cleared.
 */
public final class FaultRule {

    public static final int ALWAYS = -1;

    private final String method;
    private final Pattern path;
    private final Fault fault;
    private final AtomicInteger remaining;

    private FaultRule(String method, Pattern path, Fault fault, int times) {
        if (times == 0 || times < ALWAYS) {
            throw new IllegalArgumentException("A rule applies at least once or ALWAYS, not " + times + " times");
        }
        this.method = method;
        this.path = path;
        this.fault = fault;
        this.remaining = new AtomicInteger(times);
    }

    /**
     * {@code path} matches the end of the request path, so {@code /account} matches {@code /api/v1/account}
     * but not {@code /api/v1/account/stats}; {@code *} stands for one path segment. A null method matches any.
     */
    public static FaultRule on(String method, String path, Fault fault, int times) {
        return new FaultRule(method, pathPattern(path), fault, times);
    }

    static Pattern pathPattern(String path) {
        String regex = Arrays.stream(path.split("\\*", -1))
                .map(part -> part.isEmpty() ? "" : Pattern.quote(part))
                .collect(Collectors.joining("[^/]+"));
        return Pattern.compile(regex + "$");
    }

    /** What this rule does to a matching request. */
    public Fault fault() {
        return fault;
    }

    boolean matches(String requestMethod, String requestPath) {
        return (method == null || method.equalsIgnoreCase(requestMethod)) && path.matcher(requestPath).find();
    }

    /** Claims one use, so two requests racing for the last use don't both get the fault. */
    boolean claim() {
        return remaining.getAndUpdate(n -> n > 0 ? n - 1 : n) != 0;
    }

    @Override
    public String toString() {
        return "%s %s -> %s (%s)".formatted(method == null ? "*" : method, path, fault,
                remaining.get() == ALWAYS ? "always" : remaining.get() + " left");
    }
}
