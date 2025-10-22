# `Cluster`

Represents an active, thread-safe connection to an Aerospike cluster.

`com.aerospike.Cluster`

## Overview

The `Cluster` object is the central point of connectivity to your Aerospike database. It manages the underlying connection pools and tracks the health of the cluster nodes.

A `Cluster` instance is created from a `ClusterDefinition`. It is a heavyweight object, and in a typical application, you should **create only one `Cluster` instance** for the lifetime of your application. It is thread-safe and designed to be shared across all threads.

When you are finished with your application, you must call `close()` on the `Cluster` instance to release all resources. The `try-with-resources` statement is the recommended way to manage the lifecycle of a `Cluster` object.

## Creating a `Cluster`

You obtain a `Cluster` instance by calling `.connect()` on a configured `ClusterDefinition`.

```java
import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;

try (Cluster cluster = new ClusterDefinition("localhost", 3000).connect()) {
    // Use the cluster object...
    System.out.println("Successfully connected to the cluster.");
} catch (Exception e) {
    System.err.println("Failed to connect to the cluster: " + e.getMessage());
}
```

## Methods

### `createSession(Behavior behavior)`

Creates a lightweight `Session` object for performing database operations.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `behavior` | `Behavior` | An object that configures the policies for operations performed within the session. Use `Behavior.DEFAULT` for sensible defaults. |

**Returns:** `Session` - A new session instance.

**Example:**
```java
import com.aerospike.Session;
import com.aerospike.policy.Behavior;

Session session = cluster.createSession(Behavior.DEFAULT);
// Use the session to perform read/write operations
```

---

### `setRecordMappingFactory(RecordMappingFactory factory)`

Sets the factory used for creating `RecordMapper` instances for object-to-record mapping.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `factory` | `RecordMappingFactory` | The factory to use for object mapping. |

**Returns:** `Cluster` - The current cluster instance for method chaining.

**Example:**
```java
import com.aerospike.DefaultRecordMappingFactory;
import java.util.Map;

// Assume Customer.class and CustomerMapper.class are defined
cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
    Customer.class, new CustomerMapper()
)));
```

---

### `getRecordMappingFactory()`

Retrieves the currently configured `RecordMappingFactory`.

**Returns:** `RecordMappingFactory` - The current record mapping factory.

---

### `isConnected()`

Checks if the client is currently connected to any nodes in the cluster.

**Returns:** `boolean` - `true` if connected to at least one node, `false` otherwise.

**Example:**
```java
if (cluster.isConnected()) {
    System.out.println("Client is connected.");
} else {
    System.out.println("Client is not connected.");
}
```

---

### `close()`

Closes all connections to the cluster and releases associated resources. This method should be called when your application is shutting down.

**Example (using try-with-resources):**
```java
try (Cluster cluster = new ClusterDefinition("localhost", 3000).connect()) {
    // Operations...
}
// cluster.close() is automatically called here
```

**Example (manual close):**
```java
Cluster cluster = new ClusterDefinition("localhost", 3000).connect();
try {
    // Operations...
} finally {
    if (cluster != null) {
        cluster.close();
    }
}
```

## Related Classes

- **[`ClusterDefinition`](./cluster-definition.md)**: The builder for a `Cluster` object.
- **[`Session`](./session.md)**: Created from a `Cluster` to perform operations.
- **[`RecordMappingFactory`](../mapping/record-mapping-factory.md)**: Used for object mapping configuration.

## See Also

- **[Guide: Connection Management](../../concepts/connection-management.md)**
- **[Guide: Object Mapping](../../concepts/object-mapping.md)**
