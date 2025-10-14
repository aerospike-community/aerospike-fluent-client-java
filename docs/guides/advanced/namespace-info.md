# Namespace Information

Learn how to retrieve real-time statistics and configuration details for namespaces in your Aerospike cluster.

## Goal

By the end of this guide, you'll know how to:
- Get a list of all available namespaces
- Retrieve detailed statistics for a specific namespace
- Understand some of the key namespace metrics

## Prerequisites

- Understanding of [Core Concepts](../../concepts/README.md)
- An active `Session` connected to an Aerospike cluster

---

## Accessing Info Commands

All info commands are accessed via the `info()` method on a `Session` object. This returns an `InfoCommands` object, which provides a high-level API for querying cluster metadata.

```java
import com.aerospike.info.InfoCommands;

// Get the InfoCommands interface from your session
InfoCommands info = session.info();
```

---

## Retrieving Namespace Information

### 1. Listing All Namespaces

You can get a `Set` of all the namespace names configured in the cluster.

```java
import java.util.Set;

Set<String> namespaceNames = session.info().namespaces();

System.out.println("Available Namespaces:");
for (String name : namespaceNames) {
    System.out.println("- " + name);
}
```
**Expected Output:**
```
Available Namespaces:
- test
- bar
```

### 2. Getting Detailed Namespace Statistics

To get detailed statistics for a specific namespace, use `namespaceDetails()`. This method queries all nodes in the cluster and aggregates the statistics into a single `NamespaceDetail` object.

```java
import com.aerospike.info.classes.NamespaceDetail;
import java.util.Optional;

Optional<NamespaceDetail> detailsOptional = session.info().namespaceDetails("test");

if (detailsOptional.isPresent()) {
    NamespaceDetail details = detailsOptional.get();
    
    System.out.println("Details for namespace 'test':");
    System.out.printf("- Objects: %,d%n", details.getObjects());
    System.out.printf("- Memory Used: %,d bytes%n", details.getDataUsedBytes());
    System.out.printf("- Disk Used: %,d bytes%n", details.getDeviceUsedBytes());
    System.out.printf("- Replication Factor: %d%n", details.getEffectiveReplicationFactor());
    System.out.printf("- Stop Writes: %b%n", details.isStopWrites());
    
} else {
    System.out.println("Namespace 'test' not found.");
}
```

---

## Understanding Key Namespace Metrics

The `NamespaceDetail` object contains a wealth of information. Here are some of the most important metrics:

-   **`getObjects()`**: The total number of primary records (master and replica) stored in this namespace across all nodes.
-   **`getMasterObjects()`**: The number of master records for this namespace on this node.
-   **`getProleObjects()`**: The number of replica (prole) records for this namespace on this node.
-   **`getDataUsedBytes()`**: The amount of memory used by the primary and secondary indexes for this namespace.
-   **`getDeviceUsedBytes()`**: The amount of disk space used by data and primary indexes for this namespace (for SSD/persistent memory namespaces).
-   **`getEffectiveReplicationFactor()`**: The actual replication factor for the namespace.
-   **`isStopWrites()`**: A boolean indicating if writes are currently disabled for this namespace (e.g., due to being over the high-water mark).
-   **`getExpiredObjects()`**: The number of records that have been expired by the server.
-   **`getEvictedObjects()`**: The number of records that have been evicted from memory.

---

## Complete Example: Cluster Health Monitor

This example shows how you could build a simple health monitor that periodically checks the status of all namespaces.

```java
import com.aerospike.info.classes.NamespaceDetail;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClusterMonitor {
    private final Session session;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ClusterMonitor(Session session) {
        this.session = session;
    }

    public void start() {
        // Run a monitoring check every 30 seconds
        scheduler.scheduleAtFixedRate(this::checkNamespaces, 0, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void checkNamespaces() {
        System.out.println("--- Running Health Check ---");
        try {
            Set<String> namespaceNames = session.info().namespaces();
            
            for (String name : namespaceNames) {
                session.info().namespaceDetails(name).ifPresent(details -> {
                    System.out.printf("Namespace: %s%n", name);
                    System.out.printf("  Objects: %,d%n", details.getObjects());
                    System.out.printf("  Memory Usage: %.2f%%%n", details.getMemoryUsedPct());
                    
                    if (details.isStopWrites()) {
                        System.out.println("  !! ALERT: STOP-WRITES IS TRUE !!");
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error during health check: " + e.getMessage());
        }
        System.out.println("--- Health Check Complete ---");
    }
}
```

---

## Next Steps

- **[Info Commands](../info-commands.md)**: Explore other available info commands for monitoring sets, indexes, and more.
- **[API Reference (`InfoCommands`)](../../api/info/info-commands.md)**: See the full API for the `InfoCommands` class.
