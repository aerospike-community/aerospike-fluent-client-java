# Info Commands

Learn how to retrieve metadata, statistics, and configuration from an Aerospike cluster using the `info()` API.

## Goal

By the end of this guide, you'll know how to:
- Access the `InfoCommands` interface from a `Session`
- Fetch a list of namespaces, sets, and secondary indexes
- Get detailed statistics for a specific namespace or index
- Check the stability and status of the cluster

## Prerequisites

- [Core Concepts](../../concepts/README.md)
- Basic understanding of Aerospike architecture (namespaces, sets, nodes)

---

## The `info()` Method

The entry point for all info operations is `session.info()`. This method returns an `InfoCommands` object, which provides a high-level API for querying cluster metadata.

```java
import com.aerospike.InfoCommands;

// Get the InfoCommands interface from a session
InfoCommands info = session.info();
```

---

## Common Info Operations

### Listing Namespaces

Get a `Set` of all namespace names in the cluster.

```java
Set<String> namespaces = session.info().namespaces();

System.out.println("Available namespaces:");
namespaces.forEach(System.out::println);
// Example output:
// Available namespaces:
// test
// bar
```

### Getting Namespace Details

Fetch detailed statistics and configuration for a specific namespace.

```java
import com.aerospike.info.classes.NamespaceDetail;
import java.util.Optional;

Optional<NamespaceDetail> details = session.info().namespaceDetails("test");

details.ifPresent(ns -> {
    System.out.println("Namespace: " + ns.getName());
    System.out.println("  Replication Factor: " + ns.getReplicationFactor());
    System.out.println("  Memory Used: " + ns.getMemoryUsedBytes() + " bytes");
    System.out.println("  Disk Used: " + ns.getDeviceUsedBytes() + " bytes");
    System.out.println("  Object Count: " + ns.getObjectCount());
});
```

### Listing Sets

Get a `List` of all set names within a specific namespace.

```java
List<String> sets = session.info().sets("test");

System.out.println("Sets in 'test' namespace:");
sets.forEach(System.out::println);
// Example output:
// Sets in 'test' namespace:
// users
// products
```

### Listing Secondary Indexes

Get a `List` of all secondary indexes, optionally filtered by namespace.

```java
import com.aerospike.info.classes.Sindex;

// Get all secondary indexes in the cluster
List<Sindex> allIndexes = session.info().secondaryIndexes();

// Get secondary indexes for a specific namespace
List<Sindex> testIndexes = session.info().secondaryIndexes("test");

testIndexes.forEach(sindex -> {
    System.out.println("Index Name: " + sindex.getIndexName());
    System.out.println("  Namespace: " + sindex.getNamespace());
    System.out.println("  Set: " + sindex.getSet());
    System.out.println("  Bin: " + sindex.getBin());
});
```

### Getting Secondary Index Details

Fetch detailed statistics for a specific secondary index.

```java
import com.aerospike.info.classes.SindexDetail;

// Assumes an index named "idx_users_age" exists
Optional<SindexDetail> indexDetails = session.info()
    .secondaryIndexDetails("test", "idx_users_age");

indexDetails.ifPresent(idx -> {
    System.out.println("Index: " + idx.getIndexName());
    System.out.println("  Load Percentage: " + idx.getLoadPrc() + "%");
    System.out.println("  Keys: " + idx.getKeys());
    System.out.println("  Bins: " + idx.getBins());
});
```

### Checking Cluster Status

```java
// Check if all nodes agree on the current cluster state
boolean isStable = session.info().isClusterStable();

if (isStable) {
    System.out.println("Cluster is stable.");
} else {
    System.out.println("Warning: Cluster is in a state of flux.");
}

// Get the size of the cluster (number of nodes)
int clusterSize = session.info().getClusterSize();
System.out.println("Cluster size: " + clusterSize + " nodes");
```

---

## Raw Info Commands

For advanced use cases, you can send any raw info command string to the cluster nodes using `info()`. This is equivalent to using the `asinfo` command-line tool.

### `info(String command)`

Sends the command to a single node in the cluster and returns the raw string response.

```java
// Get statistics from one node
String stats = session.info().info("statistics");
System.out.println(stats);
```

### `infoOnAllNodes(String command)`

Sends the command to **all** nodes in the cluster and returns a `Map` of node names to their raw string responses.

```java
import java.util.Map;

// Get the build version from all nodes
Map<String, String> builds = session.info().infoOnAllNodes("build");

builds.forEach((node, response) -> {
    System.out.println("Node " + node + " is running build: " + response);
});
```

> **Reference**: For a full list of available info commands, see the [Aerospike documentation](https://aerospike.com/docs/operations/reference/info/index.html).

---

## Complete Example: Cluster Monitoring Tool

This example shows how you could build a simple monitoring utility to print a health summary of the cluster.

```java
public class ClusterMonitor {
    private final Session session;

    public ClusterMonitor(Session session) {
        this.session = session;
    }

    public void printHealthSummary() {
        InfoCommands info = session.info();

        System.out.println("--- Cluster Health Summary ---");
        
        // 1. Check cluster stability
        System.out.println("Cluster Stable: " + (info.isClusterStable() ? "✅" : "❌"));
        System.out.println("Cluster Size: " + info.getClusterSize() + " nodes");
        System.out.println();
        
        // 2. Print details for each namespace
        info.namespaces().forEach(namespace -> {
            Optional<NamespaceDetail> details = info.namespaceDetails(namespace);
            details.ifPresent(ns -> {
                System.out.println("Namespace: " + ns.getName());
                System.out.println("  Objects: " + ns.getObjectCount());
                System.out.println("  Memory Free: " + ns.getMemoryFreePrc() + "%");
                System.out.println("  Disk Free: " + ns.getDeviceFreePrc() + "%");
                System.out.println();
            });
        });
        
        // 3. Print secondary index status
        info.secondaryIndexes().forEach(sindex -> {
            info.secondaryIndexDetails(sindex.getNamespace(), sindex.getIndexName()).ifPresent(idx -> {
                System.out.println("Index: " + idx.getIndexName());
                System.out.println("  Load: " + idx.getLoadPrc() + "%");
            });
        });
        
        System.out.println("--- End of Summary ---");
    }
}
```

---

## Best Practices

### ✅ DO

**Use the high-level methods when possible.**
Methods like `namespaces()`, `sets()`, and `secondaryIndexes()` are easier to use and parse than raw info commands.

**Cache results for a short period if needed.**
Info commands query the cluster nodes directly. If you need the same information frequently (e.g., every few seconds), consider caching the results in your application to reduce load.

**Check for `Optional.isPresent()`**
Details for a namespace or index might not be available if it's being created or deleted. Always check the `Optional` result.

### ❌ DON'T

**Don't run info commands in high-frequency, performance-critical code paths.**
These are administrative commands and have a higher overhead than standard database operations. Use them for monitoring, admin panels, or application startup checks.

**Don't parse raw info strings if a typed method is available.**
The structure of raw info strings can change between server versions. The typed methods (`NamespaceDetail`, `SindexDetail`) provide a stable API.

---

## Next Steps

- **[Batch Operations](../performance/batch-operations.md)** - Learn about high-performance bulk read and write operations.
- **[API Reference: `InfoCommands`](../../api/info-commands.md)** - For a full list of all available high-level info methods.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
