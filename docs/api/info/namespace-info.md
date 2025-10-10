# NamespaceDetail Class

The `NamespaceDetail` class is a data object that holds aggregated statistics and configuration information for an Aerospike namespace.

## Overview

An instance of `NamespaceDetail` is returned by the `InfoCommands.namespaceDetails()` method. It contains a large number of fields, each corresponding to a specific metric or configuration parameter for the namespace.

The fields in this class are populated by the `InfoParser` from the raw info command results. Many fields are aggregated (e.g., summed or averaged) across all nodes in the cluster.

## Key Fields

The class contains dozens of metrics. Some of the most commonly used include:
- `getObjects()`: Total number of records.
- `getDataUsedBytes()`: Memory used for data and primary indexes.
- `getDeviceUsedBytes()`: Disk space used (for SSD namespaces).
- `getEffectiveReplicationFactor()`: The configured replication factor.
- `isStopWrites()`: Indicates if the namespace is currently blocking writes.
- `getExpiredObjects()`: Count of expired objects.
- `getEvictedObjects()`: Count of evicted objects.

For a complete list of fields, refer to the source code or your IDE's auto-completion for the `NamespaceDetail` class.

---

## Example Usage

```java
import com.aerospike.info.classes.NamespaceDetail;
import java.util.Optional;

Optional<NamespaceDetail> detailsOpt = session.info().namespaceDetails("test");

detailsOpt.ifPresent(details -> {
    System.out.println("Objects: " + details.getObjects());
    System.out.println("Memory Used: " + details.getDataUsedBytes());
});
```

---

## Next Steps

- **[InfoCommands](./info-commands.md)**: The class used to retrieve `NamespaceDetail` objects.
- **[Namespace Information Guide](../../guides/advanced/namespace-info.md)**: A how-to guide with more examples.
