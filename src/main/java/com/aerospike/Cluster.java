package com.aerospike;

import java.io.Closeable;
import java.time.Duration;
import java.util.Set;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Log;
import com.aerospike.dsl.Index;
import com.aerospike.policy.Behavior;

/**
 * Represents a connection to an Aerospike cluster.
 * 
 * <p>This class manages the lifecycle of a connection to an Aerospike cluster,
 * including the underlying client, index monitoring, and record mapping factory.
 * It implements {@link Closeable} to ensure proper resource cleanup.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * try (Cluster cluster = new ClusterDefinition("localhost", 3100).connect()) {
 *     Session session = cluster.createSession(Behavior.DEFAULT);
 *     // Use the session for database operations...
 * }
 * }</pre>
 * 
 * @see ClusterDefinition
 * @see Session
 * @see Behavior
 */
public class Cluster implements Closeable {
    /**
     * Default interval for refreshing index information from the cluster.
     */
    public static final Duration INDEX_REFRESH = Duration.ofSeconds(5);

    private final IAerospikeClient client;
    private final IndexesMonitor indexesMonitor;
    private volatile String clusterName;  // May be null initially, discovered later
    // TODO: Where should this live?
    private RecordMappingFactory recordMappingFactory = null;

    
    // package visibility
    Cluster(IAerospikeClient client, String providedClusterName) {
        this.client = client;
        this.clusterName = providedClusterName;
        this.indexesMonitor = new IndexesMonitor();
        this.indexesMonitor.startMonitor(createSession(Behavior.DEFAULT), INDEX_REFRESH);
        
        // If no cluster name provided, discover it from the server
        if (providedClusterName == null || providedClusterName.isEmpty()) {
            discoverClusterName();
        }
    }
    
    /**
     * Gets the underlying Aerospike client instance.
     * 
     * <p>This provides access to the raw Aerospike client for advanced operations
     * that may not be available through the high-level API.</p>
     * 
     * <p>Note that this method is unlikely to form part of the final interface</p>
     * @return the underlying IAerospikeClient instance
     */
    public IAerospikeClient getUnderlyingClient() {
        return client;
    }
    
    /**
     * Gets the set of available indexes in the cluster.
     * 
     * <p>This returns the current set of secondary indexes that are available
     * for querying. The index information is automatically refreshed at regular
     * intervals.</p>
     * 
     * @return a set of Index objects representing available secondary indexes
     * @see Index
     */
    public Set<Index> getIndexes() {
        return indexesMonitor.getIndexes();
    }
    
    /**
     * Sets the record mapping factory for this cluster.
     * 
     * <p>The record mapping factory is responsible for providing mappers that
     * convert between Aerospike records and Java objects. This enables automatic
     * object serialization/deserialization when working with typed datasets.</p>
     * 
     * @param factory the record mapping factory to use
     * @return this Cluster for method chaining
     * @see RecordMappingFactory
     * @see DefaultRecordMappingFactory
     */
    public Cluster setRecordMappingFactory(RecordMappingFactory factory) {
        this.recordMappingFactory = factory;
        return this;
    }
    
    /**
     * Creates a new session with the specified behavior.
     * 
     * <p>A session represents a logical connection to the cluster with specific
     * behavior settings that control how operations are performed (timeouts,
     * retry policies, consistency levels, etc.).</p>
     * 
     * @param behavior the behavior configuration for the session
     * @return a new Session instance
     * @see Session
     * @see Behavior
     */
    public Session createSession(Behavior behavior) {
        return new Session(this, behavior);
    }

    public TransactionalSession createTransactionalSession(Behavior behavior) {
        return new TransactionalSession(this, behavior);
    }
    
    /**
     * Gets the current record mapping factory.
     * 
     * @return the current record mapping factory, or null if none is set
     * @see RecordMappingFactory
     */
    public RecordMappingFactory getRecordMappingFactory() {
        return recordMappingFactory;
    }
    
    /**
     * Checks if the cluster connection is currently active.
     * 
     * @return true if the connection is active, false otherwise
     */
    public boolean isConnected() {
        return client.isConnected();
    }
    
    /**
     * Gets the cluster name.
     * 
     * <p>This may be the name provided via {@code validateClusterName()}, or the
     * name discovered from the server if none was provided.</p>
     * 
     * @return the cluster name, or null if not specified and not yet discovered
     */
    public String getClusterName() {
        return clusterName;
    }
    
    /**
     * Discovers the cluster name from the server.
     * Called automatically if no cluster name was provided at connection time.
     */
    private void discoverClusterName() {
        try {
            String discovered = client.getCluster().getClusterName();
            if (discovered != null && !discovered.isEmpty()) {
                this.clusterName = discovered;
                // Notify registry - may upgrade to cluster-specific settings
                SystemSettingsRegistry.getInstance().updateClusterName(this, discovered);
            }
        } catch (Exception e) {
            // Cluster name discovery failed, continue with null name
            Log.warn("Failed to discover cluster name: " + e.getMessage());
        }
    }
    
    /**
     * Applies system settings dynamically to this cluster.
     * Called by {@link SystemSettingsRegistry} when settings are updated.
     * 
     * <p><b>Note:</b> This is an internal method and should not be called directly.
     * System settings are automatically managed by the registry.</p>
     * 
     * <p><b>Limitation:</b> Most system settings cannot be changed dynamically with
     * the current Aerospike client API. They are applied at connection time via
     * {@link com.aerospike.client.policy.ClientPolicy}. This method is provided for
     * future compatibility when dynamic updates become available.</p>
     * 
     * @param settings the system settings to apply
     */
    void applySystemSettings(SystemSettings settings) {
        if (settings == null) {
            return;
        }
        
        // Currently, the Aerospike Java client does not support dynamic updates
        // to system-level settings like connection pool sizes, socket idle times,
        // circuit breaker settings, or tend intervals.
        //
        // These settings are applied at connection time via ClientPolicy.
        // This method is a placeholder for future enhancement.
        //
        // When dynamic updates become available, implement them here:
        // - client.setMinConnsPerNode(settings.getMinimumConnectionsPerNode())
        // - client.setMaxConnsPerNode(settings.getMaximumConnectionsPerNode())
        // - etc.
        
        Log.info("System settings updated for cluster '" + 
            (clusterName != null ? clusterName : "(unnamed)") + 
            "'. Note: Settings will take effect on next connection.");
    }
    
    /**
     * Closes the cluster connection and releases all associated resources.
     * 
     * <p>This method stops the index monitor and closes the underlying client
     * connection. It should be called when the cluster is no longer needed
     * to ensure proper resource cleanup.</p>
     * 
     * <p>This method is automatically called when using try-with-resources:</p>
     * <pre>{@code
     * try (Cluster cluster = new ClusterDefinition("localhost", 3100).connect()) {
     *     // Use the cluster...
     * } // cluster.close() is automatically called here
     * }</pre>
     */
    @Override
    public void close() {
        indexesMonitor.stopMonitor();
        this.client.close();
    }
}
