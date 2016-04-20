/*
 * Copyright (C) 2013 Intel Corporation
 * All rights reserved.
 */
package com.intel.dcsg.cpg.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides singular and plural methods for working with a multi-valued map. If you know or expect to have
 * a single value for a certain key you can use simply get and put.
 * 
 * A different approach to multivalued maps than javax.ws.rs.core.MultivaluedMap.
 * This class implements  informally because we don't want to add a 
 * dependency on jax-rs from cpg-io.
 * 
 * To reuse this class with jax-rs:
 * public class MultivaluedMapImpl extends MultivaluedHashMap<K,V> implements MultivaluedMap<K,V> { } 
 * 
 * @author jbuhacoff
 */

public class MultivaluedHashMap<K,V> {
    private Map<K,List<V>> map = new HashMap<>();
    
    public void put(K k, V v) {
        ArrayList<V> list = new ArrayList<>();
        list.add(v);
        map.put(k, list);        
    }
    
    public void putAll(K k, List<V> vs) {
        map.put(k, vs);
    }
    
    public void putAll(K k, V... vs) {
        ArrayList<V> list = new ArrayList<>();
        Collections.addAll(list, vs);
        map.put(k, list);
    }
    
    public void putAll(MultivaluedHashMap<K,V> other) {
        for(K k : other.keySet()) {
            List<V> list = other.getAll(k);
            putAll(k, list);
        }
    }
    
    public void putAll(Map<K,V> other) {
        for(K k : other.keySet()) {
            V item = other.get(k);
            put(k,item);
        }
    }
    
    public void add(K k, V v) {
        List<V> list = map.get(k);
        if( list == null ) {
            list = new ArrayList<>();
        }
        list.add(v);
        map.put(k, list);
    }
    
    public void addAll(K k, List<V> vs) {
        List<V> list = map.get(k);
        if( list == null ) {
            list = new ArrayList<>();
        }
        list.addAll(vs);
        map.put(k, list);
    }

    public void addAll(K k, V... vs) {
        List<V> list = map.get(k);
        if( list == null ) {
            list = new ArrayList<>();
        }
        Collections.addAll(list, vs);
        map.put(k, list);
    }
    
    public void addAll(MultivaluedHashMap<K,V> other) {
        for(K k : other.keySet()) {
            List<V> list = other.getAll(k);
            if( list != null ) {
                addAll(k, list);
            }
        }
    }

    public void addAll(Map<K,V> other) {
        for(K k : other.keySet()) {
            V item = other.get(k);
            add(k,item);
        }
    }
    
    /**
     * 
     * @param k
     * @return the value for k, or the first value for k if there is more than one
     */
    public V get(K k) {
        List<V> list = map.get(k);
        if( list == null || list.isEmpty() ) { return null; }
        return list.get(0);
    }
    
    /**
     * 
     * @param k
     * @return all values for key k or null if there are no values; never an empty collection
     */
    public List<V> getAll(K k) {
        List<V> list = map.get(k);
        if( list == null || list.isEmpty() ) { return null; }
        return list;
    }
    
    /**
     * Removes key k from the map completely
     * @param k 
     */
    public void remove(K k) {
        map.remove(k);
    }    

    /**
     * Removes all the given values from key k
     * 
     * @param k
     * @param vs 
     */
    public void removeAll(K k, List<V> vs) {
        List<V> list = map.get(k);
        if( list == null || list.isEmpty() ) { return; }
        list.removeAll(vs);
        map.put(k, list);
    }
    
    /**
     * Removes all the given values from key k
     * 
     * @param k
     * @param vs 
     */
    public void removeAll(K k, V... vs) {
        List<V> list = map.get(k);
        if( list == null || list.isEmpty() ) { return; }
        for(V item : vs) {
            list.remove(item);
        }
        map.put(k, list);
    }
    
    
    public void clear() {
        map.clear();
    }
    
    public boolean isEmpty() {
        return map.isEmpty();
    }
    
    public boolean containsKey(K k) {
        return map.containsKey(k);
    }
    
    /**
     * Searches for v among all values for all keys
     * @param v
     * @return 
     */
    public boolean containsValue(V v) {
        for(K k : map.keySet()) {
            List<V> list = map.get(k);
            if( list != null && list.contains(v) ) {
                return true;
            }
        }
        return false;
    }
    
    public Set<K> keySet() {
        return map.keySet();
    }
    
    public int size() {
        return map.size();
    }
    
    // Jackson mappers can already serialize and deserialize a Map
    // so the next two methods are for convenience to get and set
    // the map
    public Map<K,List<V>> getMap() { return map; }
    public void setMap(Map<K,List<V>> map) { this.map = map; }
    
}
