# `ClusterDefinition`

The entry point for defining and configuring a connection to an Aerospike cluster.

`com.aerospike.ClusterDefinition`

## Overview

`ClusterDefinition` is a **fluent builder** used to specify the hosts, credentials, TLS settings, and other policies required to establish a connection to an Aerospike cluster. Once configured, its `connect()` method returns a `Cluster` object, which represents an active connection.

This class is designed to be used in a "define once, connect once" pattern at application startup.

## Creating a `ClusterDefinition`

### Single Host

```java
import com.aerospike.ClusterDefinition;
import com.aerospike.client.Host;

// For local development or a single-node cluster
ClusterDefinition definition = new ClusterDefinition("localhost", 3000);
```

### Multiple Hosts

Providing multiple seed nodes is the recommended practice for production environments.

```java
ClusterDefinition definition = new ClusterDefinition(
    new Host("node1.my-cluster.com", 3000),
    new Host("node2.my-cluster.com", 3000),
    new Host("node3.my-cluster.com", 3000)
);
```

## Core Configuration Methods

### `withNativeCredentials(String username, String password)`

Sets the username and password for clusters with authentication enabled.

- **Parameters**:
    - `username` (String): The user to authenticate as.
    - `password` (String): The user's password.
- **Returns**: `ClusterDefinition` (for chaining)

**Example**:
```java
.withNativeCredentials("admin", "mysecretpassword")
```

### `usingServicesAlternate()`

Configures the client to use alternate service addresses, which is often required in containerized environments like Docker or Kubernetes.

- **Returns**: `ClusterDefinition` (for chaining)

**Example**:
```java
.usingServicesAlternate()
```

### `withTls(Consumer<TlsBuilder> tlsBuilder)`

Configures Transport Layer Security (TLS) for a secure connection.

- **Parameters**:
    - `tlsBuilder` (Consumer): A lambda for configuring the `TlsBuilder`.
- **Returns**: `ClusterDefinition` (for chaining)

**Example**:
```java
.withTls(tls -> tls
    .withCaFile(new File("/path/to/ca.pem"))
    .withProtocols("TLSv1.2", "TLSv1.3")
    .forAllHosts()
)
```

### `preferredRacks(int... rackIds)`

Configures rack awareness, allowing the client to prefer nodes in specific racks for read operations.

- **Parameters**:
    - `rackIds` (int...): A varargs array of preferred rack IDs.
- **Returns**: `ClusterDefinition` (for chaining)

**Example**:
```java
// Prefer nodes in the same rack as the client
.preferredRacks(1)
```

### `validateClusterNameIs(String clusterName)`

Ensures the client connects to a cluster with a specific name, preventing misconfiguration.

- **Parameters**:
    - `clusterName` (String): The expected cluster name.
- **Returns**: `ClusterDefinition` (for chaining)

**Example**:
```java
.validateClusterNameIs("prod-cluster-us-east-1")
```

### `withLogLevel(Log.Level level)`

Sets the logging level for the client.

- **Parameters**:
    - `level` (`Log.Level`): The log level (e.g., `Level.INFO`, `Level.DEBUG`, `Level.WARN`).
- **Returns**: `ClusterDefinition` (for chaining)

**Example**:
```java
.withLogLevel(Log.Level.DEBUG)
```

## Terminal Method

### `connect()`

Establishes the connection to the Aerospike cluster based on the provided configuration.

- **Returns**: `Cluster` - An active, ready-to-use cluster connection.
- **Throws**: `AerospikeException` if the connection fails (e.g., hosts unreachable, authentication failure).

---

## Complete Examples

### Basic Local Connection

```java
import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;

public class ConnectionExample {
    public static void main(String[] args) {
        ClusterDefinition definition = new ClusterDefinition("localhost", 3000);
        
        try (Cluster cluster = definition.connect()) {
            System.out.println("Successfully connected to cluster: " + cluster.getClusterName());
            // ... use the cluster object ...
        } catch (Exception e) {
            System.err.println("Failed to connect to cluster: " + e.getMessage());
        }
    }
}
```

### Production-Ready Connection

This example shows a more complete configuration suitable for a production environment.

```java
import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.client.Host;
import com.aerospike.client.Log;

public class ProductionConnection {
    public Cluster connectToProd() {
        ClusterDefinition definition = new ClusterDefinition(
            new Host("node1.prod.com", 3000),
            new Host("node2.prod.com", 3000),
            new Host("node3.prod.com", 3000)
        )
        .withNativeCredentials("app-user", System.getenv("AEROSPIKE_PASSWORD"))
        .validateClusterNameIs("prod-payments")
        .usingServicesAlternate()
        .withTls(tls -> tls.forAllHosts()) // Assumes system trust store is configured
        .preferredRacks(1, 2)
        .withLogLevel(Log.Level.INFO);
        
        return definition.connect();
    }
}
```

## Thread Safety

`ClusterDefinition` is **not thread-safe**. It is a builder intended to be configured and used by a single thread at application startup. The `Cluster` object it produces, however, **is thread-safe**.

## Related Classes

- **`Cluster`**: The result of a successful `connect()` call.
- **`Host`**: Represents a single seed node address.
- **`TlsBuilder`**: The builder for configuring TLS settings.
