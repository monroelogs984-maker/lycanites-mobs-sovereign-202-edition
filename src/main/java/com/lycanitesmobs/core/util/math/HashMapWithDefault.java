package com.lycanitesmobs.core.util.math;

import java.util.HashMap;

public class HashMapWithDefault<K, V> extends HashMap<K, V> {
    private V defaultValue;

    public void setDefault(V defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public V get(Object key) {
        return containsKey(key) ? super.get(key) : defaultValue;
    }
}
