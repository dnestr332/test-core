package com.dnestr.base.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Appends {@code value} to the list stored under the typed key {@code key}, atomically creating
     * and storing a new list first if none exists yet (so concurrent first calls can't drop a value).
     *
     * @throws ClassCastException if {@code key} already holds a value that isn't a {@link List}
     */
    public <T> void addToList(K key, T value) {
        addToList(context, key, value);
    }

    /** {@link #addToList(Object, Object)} for the dynamic key {@code key}. */
    public <T> void addToList(String key, T value) {
        addToList(dynamicContext, key, value);
    }

    /**
     * Returns the list stored under the typed key {@code key} (the live list — changes to it are
     * stored), or a new empty, unstored list if absent. Unchecked cast to {@code List<T>} — callers
     * must know the actual element type.
     */
    public <T> List<T> getList(K key) {
        return getList(context, key);
    }

    /** {@link #getList(Object)} for the dynamic key {@code key}. */
    public <T> List<T> getList(String key) {
        return getList(dynamicContext, key);
    }

    /**
     * Puts {@code value} under {@code mapKey} in the map stored under the typed key {@code key}, atomically creating
     * and storing a new insertion-ordered map first if none exists yet. A later put for the same {@code mapKey} wins.
     *
     * @throws ClassCastException if {@code key} already holds a value that isn't a {@link Map}
     */
    public <MK, V> void putToMap(K key, MK mapKey, V value) {
        putToMap(context, key, mapKey, value, false);
    }

    /** {@link #putToMap(Object, Object, Object)} for the dynamic key {@code key}. */
    public <MK, V> void putToMap(String key, MK mapKey, V value) {
        putToMap(dynamicContext, key, mapKey, value, false);
    }

    /**
     * As {@link #putToMap(Object, Object, Object)}, but the first value put for {@code mapKey} wins - for remembering
     * a record's original state before a scenario changes it, possibly more than once.
     */
    public <MK, V> void putToMapIfAbsent(K key, MK mapKey, V value) {
        putToMap(context, key, mapKey, value, true);
    }

    /** {@link #putToMapIfAbsent(Object, Object, Object)} for the dynamic key {@code key}. */
    public <MK, V> void putToMapIfAbsent(String key, MK mapKey, V value) {
        putToMap(dynamicContext, key, mapKey, value, true);
    }

    /**
     * Returns the map stored under the typed key {@code key} (the live map - changes to it are stored), or a new
     * empty, unstored map if absent. Unchecked cast to {@code Map<MK, V>} - callers must know the actual types.
     */
    public <MK, V> Map<MK, V> getMap(K key) {
        return getMap(context, key);
    }

    /** {@link #getMap(Object)} for the dynamic key {@code key}. */
    public <MK, V> Map<MK, V> getMap(String key) {
        return getMap(dynamicContext, key);
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

    @SuppressWarnings("unchecked")
    private static <M, T> void addToList(Map<M, Object> map, M key, T value) {
        ((List<T>) map.computeIfAbsent(key, k -> new ArrayList<>())).add(value);
    }

    @SuppressWarnings("unchecked")
    private static <M, T> List<T> getList(Map<M, Object> map, M key) {
        return (List<T>) Optional.ofNullable(map.get(key)).orElseGet(ArrayList::new);
    }

    @SuppressWarnings("unchecked")
    private static <M, MK, V> void putToMap(Map<M, Object> map, M key, MK mapKey, V value, boolean ifAbsent) {
        Map<MK, V> values = (Map<MK, V>) map.computeIfAbsent(key, k -> new LinkedHashMap<>());
        if (ifAbsent) {
            values.putIfAbsent(mapKey, value);
        } else {
            values.put(mapKey, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static <M, MK, V> Map<MK, V> getMap(Map<M, Object> map, M key) {
        return (Map<MK, V>) Optional.ofNullable(map.get(key)).orElseGet(LinkedHashMap::new);
    }
}
