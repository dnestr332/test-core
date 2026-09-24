package com.dnestr.base.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-scenario key-value store for sharing state between BDD steps (e.g. a value generated in one
 * step and asserted on, or reused, in a later one). Subclassed per app with a concrete {@code K}
 * (typically an enum of known, expected context keys, e.g. {@code USER}, {@code TOKEN}) — see
 * {@code BaseScenarioContextTest.Context} for the pattern.
 * <p>
 * Two independent maps live behind this one class: the typed {@code K}-keyed map is for values a
 * step author knows about ahead of time, while the {@code String}-keyed "dynamic" map is for keys
 * only known at runtime (e.g. a field name pulled from step text, as used by
 * {@code BaseContextResolver} implementations to stash generated test data). They're separate
 * namespaces — {@code set(SOME_KEY, ...)} and {@code set("SOME_KEY", ...)} don't collide.
 * <p>
 * Backed by {@link ConcurrentHashMap} for safe concurrent access within a scenario, but this class
 * does not scope itself per scenario or per thread — the owning app is responsible for giving each
 * scenario its own instance (or calling {@link #clear()} between scenarios if one is reused).
 *
 * @param <K> the app's typed context-key type, typically an enum
 */
public abstract class BaseScenarioContext<K> {

    private final Map<K, Object> context = new ConcurrentHashMap<>();
    private final Map<String, Object> dynamicContext = new ConcurrentHashMap<>();

    /** Stores {@code value} under the typed key {@code key}. */
    public <V> void set(K key, V value) {
        context.put(key, value);
    }

    /** Stores {@code value} under the dynamic (runtime-only-known) key {@code key}, in a separate namespace from the typed map. */
    public <V> void set(String key, V value) {
        dynamicContext.put(key, value);
    }

    /** Returns the value stored under the typed key {@code key}, or {@code null} if absent. Unchecked cast to {@code V} — callers must know the actual type. */
    @SuppressWarnings("unchecked")
    public <V> V get(K key) {
        return (V) context.get(key);
    }

    /** Returns the value stored under the dynamic key {@code key}, or {@code null} if absent. Unchecked cast to {@code V} — callers must know the actual type. */
    @SuppressWarnings("unchecked")
    public <V> V get(String key) {
        return (V) dynamicContext.get(key);
    }

    /** Whether a value is currently stored under the typed key {@code key}. */
    public boolean contains(K key) {
        return context.containsKey(key);
    }

    /** Whether a value is currently stored under the dynamic key {@code key}. */
    public boolean contains(String key) {
        return dynamicContext.containsKey(key);
    }

    /** Removes the value stored under the typed key {@code key}, if any. */
    public void remove(K key) {
        context.remove(key);
    }

    /** Removes the value stored under the dynamic key {@code key}, if any. */
    public void remove(String key) {
        dynamicContext.remove(key);
    }

    /** Clears both the typed and dynamic maps. Call between scenarios if this instance is reused rather than recreated per scenario. */
    public void clear() {
        context.clear();
        dynamicContext.clear();
    }
}