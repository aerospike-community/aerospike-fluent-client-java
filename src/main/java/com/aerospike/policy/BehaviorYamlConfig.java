package com.aerospike.policy;

import java.time.Duration;
import java.util.List;

import com.aerospike.client.policy.ReadModeAP;
import com.aerospike.client.policy.ReadModeSC;
import com.aerospike.client.policy.Replica;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;

class BehaviorYamlConfig {
    
    @JsonProperty("behaviors")
    private List<BehaviorConfig> behaviors;
    
    @JsonProperty("system")
    private SystemConfig system;
    
    // Getters and setters
    public List<BehaviorConfig> getBehaviors() { return behaviors; }
    public void setBehaviors(List<BehaviorConfig> behaviors) { this.behaviors = behaviors; }
    
    public SystemConfig getSystem() { return system; }
    public void setSystem(SystemConfig system) { this.system = system; }
    
    // Individual behavior configuration
    public static class BehaviorConfig {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("parent")
        private String parent;
        
        @JsonProperty("sendKey")
        private Boolean sendKey;
        
        @JsonProperty("useCompression")
        private Boolean useCompression;
        
        @JsonProperty("allOperations")
        private PolicyConfig allOperations;
        
        @JsonProperty("readModeSC")
        private ConsistencyModeReadConfig readModeSC;
        
        @JsonProperty("readModeAP")
        private AvailabilityModeReadConfig readModeAP;
        
        @JsonProperty("retryableWrites")
        private WriteConfig retryableWrites;
        
        @JsonProperty("nonRetryableWrites")
        private WriteConfig nonRetryableWrites;
        
        @JsonProperty("batchReads")
        private BatchConfig batchReads;
        
        @JsonProperty("batchWrites")
        private BatchConfig batchWrites;
        
        @JsonProperty("query")
        private QueryConfig query;
        
        @JsonProperty("info")
        private InfoConfig info;
        
        @JsonProperty("systemTxnVerify")
        private SystemTxnVerifyConfig systemTxnVerify;
        
        @JsonProperty("systemTxnRoll")
        private SystemTxnRollConfig systemTxnRoll;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getParent() { return parent; }
        public void setParent(String parent) { this.parent = parent; }
        
        public Boolean getSendKey() { return sendKey; }
        public void setSendKey(Boolean sendKey) { this.sendKey = sendKey; }
        
        public Boolean getUseCompression() { return useCompression; }
        public void setUseCompression(Boolean useCompression) { this.useCompression = useCompression; }
        
        public PolicyConfig getAllOperations() { return allOperations; }
        public void setAllOperations(PolicyConfig allOperations) { this.allOperations = allOperations; }
        
        public ConsistencyModeReadConfig getConsistencyModeReads() { return readModeSC; }
        public void setConsistencyModeReads(ConsistencyModeReadConfig consistencyModeReads) { this.readModeSC = consistencyModeReads; }
        
        public AvailabilityModeReadConfig getAvailabilityModeReads() { return readModeAP; }
        public void setAvailabilityModeReads(AvailabilityModeReadConfig availabilityModeReads) { this.readModeAP = availabilityModeReads; }
        
        public WriteConfig getRetryableWrites() { return retryableWrites; }
        public void setRetryableWrites(WriteConfig retryableWrites) { this.retryableWrites = retryableWrites; }
        
        public WriteConfig getNonRetryableWrites() { return nonRetryableWrites; }
        public void setNonRetryableWrites(WriteConfig nonRetryableWrites) { this.nonRetryableWrites = nonRetryableWrites; }
        
        public BatchConfig getBatchReads() { return batchReads; }
        public void setBatchReads(BatchConfig batchReads) { this.batchReads = batchReads; }
        
        public BatchConfig getBatchWrites() { return batchWrites; }
        public void setBatchWrites(BatchConfig batchWrites) { this.batchWrites = batchWrites; }
        
        public QueryConfig getQuery() { return query; }
        public void setQuery(QueryConfig query) { this.query = query; }
        
        public InfoConfig getInfo() { return info; }
        public void setInfo(InfoConfig info) { this.info = info; }
        
        public SystemTxnVerifyConfig getSystemTxnVerify() { return systemTxnVerify; }
        public void setSystemTxnVerify(SystemTxnVerifyConfig systemTxnVerify) { this.systemTxnVerify = systemTxnVerify; }
        
        public SystemTxnRollConfig getSystemTxnRoll() { return systemTxnRoll; }
        public void setSystemTxnRoll(SystemTxnRollConfig systemTxnRoll) { this.systemTxnRoll = systemTxnRoll; }
    }
    
    // Base policy configuration
    public static class PolicyConfig {
        @JsonDeserialize(using = DurationDeserializer.class)
        @JsonSerialize(using = DurationSerializer.class)
        @JsonProperty("abandonCallAfter")
        private Duration abandonCallAfter;
        
        @JsonDeserialize(using = DurationDeserializer.class)
        @JsonSerialize(using = DurationSerializer.class)
        @JsonProperty("delayBetweenRetries")
        private Duration delayBetweenRetries;
        
        @JsonProperty("maximumNumberOfCallAttempts")
        private Integer maximumNumberOfCallAttempts;
        
        @JsonProperty("replicaOrder")
        private Replica replicaOrder;
        
        @JsonProperty("resetTtlOnReadAtPercent")
        private Integer resetTtlOnReadAtPercent;
        
        @JsonProperty("sendKey")
        private Boolean sendKey;
        
        @JsonProperty("useCompression")
        private Boolean useCompression;
        
        @JsonDeserialize(using = DurationDeserializer.class)
        @JsonSerialize(using = DurationSerializer.class)
        @JsonProperty("waitForCallToComplete")
        private Duration waitForCallToComplete;
        
        @JsonDeserialize(using = DurationDeserializer.class)
        @JsonSerialize(using = DurationSerializer.class)
        @JsonProperty("waitForConnectionToComplete")
        private Duration waitForConnectionToComplete;
        
        @JsonDeserialize(using = DurationDeserializer.class)
        @JsonSerialize(using = DurationSerializer.class)
        @JsonProperty("waitForSocketResponseAfterCallFails")
        private Duration waitForSocketResponseAfterCallFails;
        
        // Getters and setters
        public Duration getAbandonCallAfter() { return abandonCallAfter; }
        public void setAbandonCallAfter(Duration abandonCallAfter) { this.abandonCallAfter = abandonCallAfter; }
        
        public Duration getDelayBetweenRetries() { return delayBetweenRetries; }
        public void setDelayBetweenRetries(Duration delayBetweenRetries) { this.delayBetweenRetries = delayBetweenRetries; }
        
        public Integer getMaximumNumberOfCallAttempts() { return maximumNumberOfCallAttempts; }
        public void setMaximumNumberOfCallAttempts(Integer maximumNumberOfCallAttempts) { this.maximumNumberOfCallAttempts = maximumNumberOfCallAttempts; }
        
        public Replica getReplicaOrder() { return replicaOrder; }
        public void setReplicaOrder(Replica replicaOrder) { this.replicaOrder = replicaOrder; }
        
        public Integer getResetTtlOnReadAtPercent() { return resetTtlOnReadAtPercent; }
        public void setResetTtlOnReadAtPercent(Integer resetTtlOnReadAtPercent) { this.resetTtlOnReadAtPercent = resetTtlOnReadAtPercent; }
        
        public Boolean getSendKey() { return sendKey; }
        public void setSendKey(Boolean sendKey) { this.sendKey = sendKey; }
        
        public Boolean getUseCompression() { return useCompression; }
        public void setUseCompression(Boolean useCompression) { this.useCompression = useCompression; }
        
        public Duration getWaitForCallToComplete() { return waitForCallToComplete; }
        public void setWaitForCallToComplete(Duration waitForCallToComplete) { this.waitForCallToComplete = waitForCallToComplete; }
        
        public Duration getWaitForConnectionToComplete() { return waitForConnectionToComplete; }
        public void setWaitForConnectionToComplete(Duration waitForConnectionToComplete) { this.waitForConnectionToComplete = waitForConnectionToComplete; }
        
        public Duration getWaitForSocketResponseAfterCallFails() { return waitForSocketResponseAfterCallFails; }
        public void setWaitForSocketResponseAfterCallFails(Duration waitForSocketResponseAfterCallFails) { this.waitForSocketResponseAfterCallFails = waitForSocketResponseAfterCallFails; }
    }
    
    // Consistency mode read configuration
    public static class ConsistencyModeReadConfig extends PolicyConfig {
        @JsonProperty("readModeSC")
        private ReadModeSC readModeSC;
        
        public ReadModeSC getReadConsistency() { return readModeSC; }
        public void setReadConsistency(ReadModeSC readConsistency) { this.readModeSC = readConsistency; }
    }
    
    // Availability mode read configuration
    public static class AvailabilityModeReadConfig extends PolicyConfig {
        @JsonProperty("readModeAP")
        private ReadModeAP readModeAP;
        
        public ReadModeAP getMigrationReadConsistency() { return readModeAP; }
        public void setMigrationReadConsistency(ReadModeAP migrationReadConsistency) { this.readModeAP = migrationReadConsistency; }
    }
    
    // Write configuration
    public static class WriteConfig extends PolicyConfig {
        @JsonProperty("useDurableDelete")
        private Boolean useDurableDelete;
        
        public Boolean getUseDurableDelete() { return useDurableDelete; }
        public void setUseDurableDelete(Boolean useDurableDelete) { this.useDurableDelete = useDurableDelete; }
    }
    
    // Batch configuration
    public static class BatchConfig extends PolicyConfig {
        @JsonProperty("maxConcurrentServers")
        private Integer maxConcurrentServers;
        
        @JsonProperty("allowInlineMemoryAccess")
        private Boolean allowInlineMemoryAccess;
        
        @JsonProperty("allowInlineSsdAccess")
        private Boolean allowInlineSsdAccess;
        
        public Integer getMaxConcurrentServers() { return maxConcurrentServers; }
        public void setMaxConcurrentServers(Integer maxConcurrentServers) { this.maxConcurrentServers = maxConcurrentServers; }
        
        public Boolean getAllowInlineMemoryAccess() { return allowInlineMemoryAccess; }
        public void setAllowInlineMemoryAccess(Boolean allowInlineMemoryAccess) { this.allowInlineMemoryAccess = allowInlineMemoryAccess; }
        
        public Boolean getAllowInlineSsdAccess() { return allowInlineSsdAccess; }
        public void setAllowInlineSsdAccess(Boolean allowInlineSsdAccess) { this.allowInlineSsdAccess = allowInlineSsdAccess; }
    }
    
    // Query configuration
    public static class QueryConfig extends PolicyConfig {
        @JsonProperty("recordQueueSize")
        private Integer recordQueueSize;
        
        @JsonProperty("maxConcurrentServers")
        private Integer maxConcurrentServers;
        
        public Integer getRecordQueueSize() { return recordQueueSize; }
        public void setRecordQueueSize(Integer recordQueueSize) { this.recordQueueSize = recordQueueSize; }
        
        public Integer getMaxConcurrentServers() { return maxConcurrentServers; }
        public void setMaxConcurrentServers(Integer maxConcurrentServers) { this.maxConcurrentServers = maxConcurrentServers; }
    }
    
    // Info configuration
    public static class InfoConfig extends PolicyConfig {
        // Info only has abandonCallAfter from the base PolicyConfig
    }
    
    // System - Transaction Verify configuration (read-like settings)
    public static class SystemTxnVerifyConfig extends PolicyConfig {
        @JsonProperty("consistency")
        private ReadModeSC consistency;
        
        public ReadModeSC getConsistency() { return consistency; }
        public void setConsistency(ReadModeSC consistency) { this.consistency = consistency; }
    }
    
    // System - Transaction Roll configuration (write-like settings)
    public static class SystemTxnRollConfig extends PolicyConfig {
        // Uses base PolicyConfig fields: abandonCallAfter, delayBetweenRetries, maximumNumberOfCallAttempts,
        // replicaOrder, waitForCallToComplete, waitForConnectionToComplete, waitForSocketResponseAfterCallFails
    }
    
    // -----------------------------------------------------------------------------------
    // System Settings Configuration (for unified YAML)
    // -----------------------------------------------------------------------------------
    
    /**
     * Top-level system settings configuration containing default and cluster-specific settings.
     */
    public static class SystemConfig {
        @JsonProperty("default")
        private SystemSettingsConfig defaultSettings;
        
        @JsonProperty("clusters")
        private java.util.Map<String, SystemSettingsConfig> clusters;
        
        public SystemSettingsConfig getDefaultSettings() { return defaultSettings; }
        public void setDefaultSettings(SystemSettingsConfig defaultSettings) { 
            this.defaultSettings = defaultSettings; 
        }
        
        public java.util.Map<String, SystemSettingsConfig> getClusters() { return clusters; }
        public void setClusters(java.util.Map<String, SystemSettingsConfig> clusters) { 
            this.clusters = clusters; 
        }
    }
    
    /**
     * System settings for a single cluster or default settings.
     */
    public static class SystemSettingsConfig {
        @JsonProperty("connections")
        private ConnectionsConfig connections;
        
        @JsonProperty("circuitBreaker")
        private CircuitBreakerConfig circuitBreaker;
        
        @JsonProperty("refresh")
        private RefreshConfig refresh;
        
        public ConnectionsConfig getConnections() { return connections; }
        public void setConnections(ConnectionsConfig connections) { this.connections = connections; }
        
        public CircuitBreakerConfig getCircuitBreaker() { return circuitBreaker; }
        public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) { this.circuitBreaker = circuitBreaker; }
        
        public RefreshConfig getRefresh() { return refresh; }
        public void setRefresh(RefreshConfig refresh) { this.refresh = refresh; }
    }
    
    /**
     * Connection pool configuration.
     */
    public static class ConnectionsConfig {
        @JsonProperty("minimumConnectionsPerNode")
        private Integer minimumConnectionsPerNode;
        
        @JsonProperty("maximumConnectionsPerNode")
        private Integer maximumConnectionsPerNode;
        
        @JsonDeserialize(using = DurationDeserializer.class)
        @JsonSerialize(using = DurationSerializer.class)
        @JsonProperty("maximumSocketIdleTime")
        private Duration maximumSocketIdleTime;
        
        public Integer getMinimumConnectionsPerNode() { return minimumConnectionsPerNode; }
        public void setMinimumConnectionsPerNode(Integer minimumConnectionsPerNode) { 
            this.minimumConnectionsPerNode = minimumConnectionsPerNode; 
        }
        
        public Integer getMaximumConnectionsPerNode() { return maximumConnectionsPerNode; }
        public void setMaximumConnectionsPerNode(Integer maximumConnectionsPerNode) { 
            this.maximumConnectionsPerNode = maximumConnectionsPerNode; 
        }
        
        public Duration getMaximumSocketIdleTime() { return maximumSocketIdleTime; }
        public void setMaximumSocketIdleTime(Duration maximumSocketIdleTime) { 
            this.maximumSocketIdleTime = maximumSocketIdleTime; 
        }
    }
    
    /**
     * Circuit breaker configuration.
     */
    public static class CircuitBreakerConfig {
        @JsonProperty("numTendIntervalsInErrorWindow")
        private Integer numTendIntervalsInErrorWindow;
        
        @JsonProperty("maximumErrorsInErrorWindow")
        private Integer maximumErrorsInErrorWindow;
        
        public Integer getNumTendIntervalsInErrorWindow() { return numTendIntervalsInErrorWindow; }
        public void setNumTendIntervalsInErrorWindow(Integer numTendIntervalsInErrorWindow) { 
            this.numTendIntervalsInErrorWindow = numTendIntervalsInErrorWindow; 
        }
        
        public Integer getMaximumErrorsInErrorWindow() { return maximumErrorsInErrorWindow; }
        public void setMaximumErrorsInErrorWindow(Integer maximumErrorsInErrorWindow) { 
            this.maximumErrorsInErrorWindow = maximumErrorsInErrorWindow; 
        }
    }
    
    /**
     * Cluster refresh configuration.
     */
    public static class RefreshConfig {
        @JsonDeserialize(using = DurationDeserializer.class)
        @JsonSerialize(using = DurationSerializer.class)
        @JsonProperty("tendInterval")
        private Duration tendInterval;
        
        public Duration getTendInterval() { return tendInterval; }
        public void setTendInterval(Duration tendInterval) { 
            this.tendInterval = tendInterval; 
        }
    }
} 