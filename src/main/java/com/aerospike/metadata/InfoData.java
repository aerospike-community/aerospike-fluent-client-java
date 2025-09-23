package com.aerospike.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.aerospike.ASNode;
import com.aerospike.Session;

public abstract class InfoData<T extends InfoData> {
    public static interface ChangeHandler{
        void onChange(Object oldValue, Object newValue);
    }
    
    private Map<String, Map<String, String>> currentData;
    private final InfoMetadata metadata;
    private final ASNode node;
    private final Session session;
    private Thread monitorThread = null;
    private volatile int refreshTimeMs = 0;
    private Map<String, List<ChangeHandler>> handlers;
    
    public InfoData(InfoMetadata metadata, ASNode node, Session session, int refreshInterval) {
        if (node == null && session == null) {
            throw new IllegalArgumentException("Either node or session must be specified");
        }
        this.metadata = metadata;
        this.node = node;
        this.session = session;
    }

    /** 
     * This method should be invoked once initialization of the sub-classes are complete.
     */
    protected void completeInitialization() {
        this.loadData();
    }
    private void loadData() {
        Map<String, Map<String, String>> nodesData = new HashMap<>();
        if (node != null) {
            nodesData.put(node.getName(), getInfoForNode(node));
        }
        else {
            // TODO: Parallelize this?
            for (ASNode node: session.getNodes()) {
                nodesData.put(node.getName(), getInfoForNode(node));
            }
        }
        this.load(nodesData);
    }
    
    private synchronized void load(Map<String, Map<String, String>> nodesMap) {
        this.currentData = nodesMap;
    }
    
    protected boolean getBoolean(String name) {
        return this.getBoolean(name, currentData);
    }
    
    protected boolean getBoolean(String name, Map<String, Map<String, String>> data) {
        boolean currentValue = false;
        boolean firstValue = true;
        for (String nodeName : data.keySet()) {
            String thisValue = data.get(nodeName).get(name);
            if (thisValue != null) {
                boolean thisBool = Boolean.valueOf(thisValue);
                if (firstValue) {
                    currentValue = thisBool;
                    firstValue = false;
                }
                else {
                    currentValue = metadata.merge(name, currentValue, thisBool);
                }
            }
        }
        if (firstValue) {
            throw new IllegalStateException("Configuration value " + name + " is not a recognized metric on any servers");
        }
        return currentValue;
    }
    
    protected boolean getLong(String name) {
        return this.getBoolean(name, currentData);
    }
    
    protected long getLong(String name, Map<String, Map<String, String>> data) {
        long currentValue = 0;
        boolean firstValue = true;
        for (String nodeName : data.keySet()) {
            String thisValue = data.get(nodeName).get(name);
            if (thisValue != null) {
                long thisLong = Long.valueOf(thisValue);
                if (firstValue) {
                    currentValue = thisLong;
                    firstValue = false;
                }
                else {
                    currentValue = metadata.merge(name, currentValue, thisLong);
                }
            }
        }
        if (firstValue) {
            throw new IllegalStateException("Configuration value " + name + " is not a recognized metric on any servers");
        }
        return currentValue;
    }
    
    private synchronized boolean addHandler(String name, ChangeHandler handler) {
        if (this.handlers == null) {
            this.handlers = new HashMap<>();
        }
        List<ChangeHandler> handlerList = this.handlers.get(name);
        if (handlerList == null) {
            handlerList = new ArrayList<>();
            this.handlers.put(name, handlerList);
        }
        if (!handlerList.contains(handler)) {
            handlerList.add(handler);
            return true;
        }
        return false;
    }
    
    private synchronized void removeAllHandlers(String name) {
        if (this.handlers != null) {
            this.handlers.remove(name);
        }
    }
    
    private synchronized boolean removeHandler(String name, ChangeHandler handler) {
        if (this.handlers == null) {
            return false;
        }
        List<ChangeHandler> handlerList = this.handlers.get(name);
        if (handlerList == null) {
            return false;
        }
        return handlerList.remove(handler);
    }
    
    private synchronized void dataRefreshed(Map<String, Map<String, String>> oldValues, Map<String, Map<String, String>> newValues) {
        if (this.handlers != null) {
            for (String key : this.handlers.keySet()) {
                List<ChangeHandler> handlers = this.handlers.get(key);
                if (handlers != null) {
                    // Find the old value and the new value
                    // TODO: Need the metric type (long, bool, etc)
                    long oldValue = this.getLong(key, oldValues);
                    long newValue = this.getLong(key, newValues);
                    if (oldValue != newValue) {
                        for (ChangeHandler handler : handlers) {
                            handler.onChange(oldValue, newValue);
                        }
                    }
                }
            }
        }
    }
    
    public T refreshEvery(int value, TimeUnit unit) {
        int refreshTime = (int)unit.toMillis(value);
        if (this.monitorThread == null || this.refreshTimeMs != refreshTime) {
            this.refreshTimeMs = refreshTime;
            if (this.monitorThread == null) {
                this.monitorThread = new Thread(() -> {
                    while (true) {
                        try {
                            Thread.sleep(refreshTimeMs);
                        } catch (InterruptedException e) {
                            break;
                        }
                        Map<String, Map<String, String>> oldValues = this.currentData;
                        this.loadData();
                        this.dataRefreshed(oldValues, this.currentData);
                    }
                });
                this.monitorThread.setDaemon(true);
                this.monitorThread.start();
            }
        }
        return (T)this;
    }
    
    public T onChange(String name, ChangeHandler handler) {
        this.addHandler(name, handler);
        return (T)this;
    }
    
    public T stopWatching(String name) {
        this.removeAllHandlers(name);
        return (T)this;
    }
    
    public T stopWatching(String name, ChangeHandler handler) {
        this.removeHandler(name, handler);
        return (T)this;
    }
    
    public T stopRefreshing() {
        if (this.monitorThread != null) {
            this.monitorThread.interrupt();
            this.monitorThread = null;
            this.refreshTimeMs = 0;
        }
        return (T) this;
    }
    protected abstract Map<String, String> getInfoForNode(ASNode node);
}
