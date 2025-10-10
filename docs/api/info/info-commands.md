# InfoCommands Class

The `InfoCommands` class provides a high-level API for retrieving metadata and statistics from the Aerospike cluster.

## Overview

The `InfoCommands` class is accessed via the `session.info()` method. It provides convenient methods for common info queries, parsing the results into structured Java objects.

This class simplifies cluster monitoring and administration tasks by abstracting away the low-level details of info command syntax.

## Key Concepts

- **Aggregated vs. Per-Node**: Most methods are available in two forms: one that aggregates results from all nodes into a single response, and another that returns a `Map<Node, ...>` with separate results for each node.
- **Structured Results**: The class returns results as Java objects (e.g., `NamespaceDetail`, `Sindex`), making the information easy to work with.

---

## Core Methods

### Namespace and Set Information

| Method | Description |
| --- | --- |
| `namespaces()` | Returns a `Set<String>` of all namespace names in the cluster. |
| `namespaceDetails(String ns)` | Returns an `Optional<NamespaceDetail>` with aggregated statistics for the specified namespace. |
| `namespaceDetailsPerNode(String ns)` | Returns a `Map<Node, Optional<NamespaceDetail>>` with statistics for each node. |
| `sets()` | Returns a `List<SetDetail>` with aggregated information for all sets in the cluster. |
| `setsPerNode()` | Returns a `Map<Node, List<SetDetail>>` with set information for each node. |

### Secondary Index Information

| Method | Description |
| --- | --- |
| `secondaryIndexes()` | Returns a `List<Sindex>` of all secondary indexes in the cluster. |
| `secondaryIndexesPerNode()` | Returns a `Map<Node, List<Sindex>>` with index information for each node. |
| `secondaryIndexDetails(String ns, String name)` | Returns an `Optional<SindexDetail>` with detailed statistics for a specific index. |
| `secondaryIndexDetailsPerNode(String ns, String name)` | Returns a `Map<Node, Optional<SindexDetail>>` with index details for each node. |

### Other Information

| Method | Description |
| --- | --- |
| `build()` | Returns a `Set<String>` of the Aerospike server build versions running in the cluster. |

---

## Example Usage

```java
import com.aerospike.info.InfoCommands;
import com.aerospike.info.classes.NamespaceDetail;
import java.util.Optional;
import java.util.Set;

// Get the InfoCommands interface
InfoCommands info = session.info();

// List all namespaces
Set<String> namespaces = info.namespaces();
System.out.println("Namespaces: " + namespaces);

// Get details for the "test" namespace
Optional<NamespaceDetail> nsDetails = info.namespaceDetails("test");
nsDetails.ifPresent(details -> {
    System.out.println("Test Namespace Objects: " + details.getObjects());
    System.out.println("Test Namespace Memory Used: " + details.getDataUsedBytes());
});
```

---

## Next Steps

- **[Namespace Information Guide](../../guides/advanced/namespace-info.md)**: A how-to guide on using these commands for monitoring.
- **[Session API](../connection/session.md)**: The entry point for accessing `InfoCommands`.
