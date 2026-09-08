package com.dnestr.base.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseScenarioContext<K> {

    private final Map<K, Object> context = new ConcurrentHashMap<>();
    private final Map<String, Object> dynamicContext = new ConcurrentHashMap<>();

    public <V> void set(K key, V value) {
        context.put(key, value);
    }

    public <V> void set(String key, V value) {
        dynamicContext.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <V> V get(K key) {
        return (V) context.get(key);
    }

    @SuppressWarnings("unchecked")
    public <V> V get(String key) {
        return (V) dynamicContext.get(key);
    }

    public boolean contains(K key) {
        return context.containsKey(key);
    }

    public boolean contains(String key) {
        return dynamicContext.containsKey(key);
    }

    public void remove(K key) {
        context.remove(key);
    }

    public void remove(String key) {
        dynamicContext.remove(key);
    }

    public void clear() {
        context.clear();
        dynamicContext.clear();
    }
}