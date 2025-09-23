package com.aerospike;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class AerospikeMap implements SortedMap<Object, Object> {
    private static AerospikeComparator comparator = new AerospikeComparator();
    private final TreeMap<Object, Object> values;
    
    public AerospikeMap() {
        this.values = new TreeMap<>(new AerospikeComparator());
    }
    
    public AerospikeMap(Map<Object, Object> map) {
        this();
        this.values.putAll(map);
    }

    @Override
    public int size() {
        return this.values.size();
    }

    @Override
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.values.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return values.get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        return values.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return values.remove(key);
    }

    @Override
    public void putAll(Map<? extends Object, ? extends Object> m) {
        values.putAll(m);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public Comparator<? super Object> comparator() {
        return comparator;
    }

    @Override
    public SortedMap<Object, Object> subMap(Object fromKey, Object toKey) {
        return values.subMap(fromKey, toKey);
    }

    @Override
    public SortedMap<Object, Object> headMap(Object toKey) {
        return values.headMap(toKey);
    }

    @Override
    public SortedMap<Object, Object> tailMap(Object fromKey) {
        return values.tailMap(fromKey);
    }

    @Override
    public Object firstKey() {
        return values.firstKey();
    }

    @Override
    public Object lastKey() {
        return values.lastKey();
    }

    @Override
    public Set<Object> keySet() {
        return values.keySet();
    }

    @Override
    public Collection<Object> values() {
        return values.values();
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return values.entrySet();
    }
    
    @Override
    public String toString() {
        return values.toString();
    }
    
    public static void main(String[] args) {
        AerospikeMap map = new AerospikeMap();
        map.put("a", 1);
        map.put(2, "b");
        map.put(new byte[] {0x01, 0x02, 0x03}, "c");
        map.put("d", new Object[] {1, "2", 3.0});
        System.out.println(map.toString());
    }
}
