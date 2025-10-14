# Connection Management

Learn how to establish and manage connections to Aerospike clusters using the Fluent Client.

## Overview

Connection management in the Fluent Client uses two main classes:

- **`ClusterDefinition`**: Builder for configuring connection parameters
- **`Cluster`**: Represents an active connection to an Aerospike cluster

```
ClusterDefinition (Configuration)
        ↓ .connect()
    Cluster (Active Connection)
        ↓ .createSession()
    Session (Operations)
```

## Basic Connection

### Single Node

The simplest connection to a local Aerospike instance:

```java
try (Cluster cluster = new ClusterDefinition("localhost", 3000)
        .connect()) {
    
    Session session = cluster.createSession(Behavior.DEFAULT);
    // Use session for operations...
    
} // Cluster automatically closed
```

**Key Points**:
- Host: `localhost`
- Port: `3000` (default Aerospike port)
- Uses try-with-resources for automatic cleanup
- No authentication (suitable for development)

### Remote Server

Connect to a remote Aerospike server:

```java
Cluster cluster = new ClusterDefinition("db.example.com", 3000)
    .connect();
```

## Authentication

### Username/Password Authentication

For clusters with security enabled:

```java
Cluster cluster = new ClusterDefinition("secure.example.com", 3000)
    .withNativeCredentials("app_user", "secure_password")
    .connect();
```

**Security Best Practices**:
- ❌ Don't hardcode credentials in source code
- ✅ Use environment variables or configuration files
- ✅ Use a secrets management system in production

```java
// Read from environment
String username = System.getenv("AEROSPIKE_USERNAME");
String password = System.getenv("AEROSPIKE_PASSWORD");

Cluster cluster = new ClusterDefinition("secure.example.com", 3000)
    .withNativeCredentials(username, password)
    .connect();
```

## Multi-Node Clusters

### Connecting to Multiple Seeds

For high availability, provide multiple seed nodes:

```java
import com.aerospike.client.Host;

Cluster cluster = new ClusterDefinition(
        new Host("node1.example.com", 3000),
        new Host("node2.example.com", 3000),
        new Host("node3.example.com", 3000)
    )
    .connect();
```

**How It Works**:
- Client connects to any available seed node
- Discovers all other nodes in the cluster automatically
- If a seed node is down, tries the next one
- Only need one seed to be available

### Using a List of Hosts

```java
import java.util.List;

List<Host> hosts = List.of(
    new Host("node1.example.com", 3000),
    new Host("node2.example.com", 3000),
    new Host("node3.example.com", 3000)
);

Cluster cluster = new ClusterDefinition(hosts)
    .connect();
```

## TLS/SSL Configuration

### Basic TLS

For encrypted connections:

```java
Cluster cluster = new ClusterDefinition("secure.example.com", 4333)
    .withNativeCredentials("username", "password")
    .withTls(tls -> tls
        .enabledProtocols("TLSv1.3")
    )
    .connect();
```

### TLS with Trust Store

For production environments with certificate validation:

```java
Cluster cluster = new ClusterDefinition("prod.example.com", 4333)
    .withNativeCredentials("username", "password")
    .withTls(tls -> tls
        .enabledProtocols("TLSv1.3")
        .trustStorePath("/path/to/truststore.jks")
        .trustStorePassword("truststorepass")
    )
    .connect();
```

### Mutual TLS (mTLS)

For environments requiring client certificates:

```java
Cluster cluster = new ClusterDefinition("secure.example.com", 4333)
    .withTls(tls -> tls
        .enabledProtocols("TLSv1.3")
        .trustStorePath("/path/to/truststore.jks")
        .trustStorePassword("truststorepass")
        .keyStorePath("/path/to/client-keystore.jks")
        .keyStorePassword("keystorepass")
    )
    .connect();
```

## Advanced Configuration

### Cluster Name Validation

Ensure you're connecting to the correct cluster:

```java
Cluster cluster = new ClusterDefinition("prod.example.com", 3000)
    .validateClusterNameIs("production-cluster")
    .connect();
```

If the cluster name doesn't match, connection fails with an error.

### Rack Awareness

For multi-datacenter deployments, prefer specific racks:

```java
Cluster cluster = new ClusterDefinition(hosts)
    .preferredRacks(1, 2)  // Prefer racks 1 and 2
    .connect();
```

**Benefits**:
- Reduced latency (reads from nearby racks)
- Better failure isolation
- Improved availability

### Services Alternate

For cloud deployments or when using NAT:

```java
Cluster cluster = new ClusterDefinition("cloud-lb.example.com", 3000)
    .usingServicesAlternate()
    .connect();
```

**When to Use**:
- Kubernetes/containerized environments
- Cloud load balancers
- NAT/firewall scenarios

### Logging Configuration

Control client logging:

```java
import java.util.logging.Level;

Cluster cluster = new ClusterDefinition("localhost", 3000)
    .withLogLevel(Level.DEBUG)  // DEBUG, INFO, WARNING, ERROR
    .connect();
```

### Custom Log Handler

```java
import com.aerospike.client.Log.Callback;

Callback logCallback = (level, message) -> {
    System.out.println("[" + level + "] " + message);
    // Or send to your logging system
};

Cluster cluster = new ClusterDefinition("localhost", 3000)
    .useLogSink(logCallback)
    .connect();
```

## Complete Examples

### Development Environment

```java
public class DevelopmentConnection {
    public static Cluster connect() {
        return new ClusterDefinition("localhost", 3000)
            .withLogLevel(Level.DEBUG)
            .connect();
    }
}
```

### Staging Environment

```java
public class StagingConnection {
    public static Cluster connect() {
        return new ClusterDefinition(
                new Host("staging-1.example.com", 3000),
                new Host("staging-2.example.com", 3000)
            )
            .withNativeCredentials(
                System.getenv("AEROSPIKE_USERNAME"),
                System.getenv("AEROSPIKE_PASSWORD")
            )
            .validateClusterNameIs("staging-cluster")
            .withLogLevel(Level.INFO)
            .connect();
    }
}
```

### Production Environment

```java
public class ProductionConnection {
    public static Cluster connect() {
        List<Host> hosts = Arrays.asList(
            new Host("prod-node-1.example.com", 4333),
            new Host("prod-node-2.example.com", 4333),
            new Host("prod-node-3.example.com", 4333)
        );
        
        return new ClusterDefinition(hosts)
            .withNativeCredentials(
                System.getenv("AEROSPIKE_USERNAME"),
                System.getenv("AEROSPIKE_PASSWORD")
            )
            .validateClusterNameIs("production-cluster")
            .preferredRacks(1, 2)
            .usingServicesAlternate()
            .withTls(tls -> tls
                .enabledProtocols("TLSv1.3")
                .trustStorePath("/etc/aerospike/tls/truststore.jks")
                .trustStorePassword(System.getenv("TRUSTSTORE_PASSWORD"))
            )
            .withLogLevel(Level.WARNING)
            .connect();
    }
}
```

## Connection Lifecycle

### Opening a Connection

```java
// 1. Define configuration
ClusterDefinition definition = new ClusterDefinition("localhost", 3000)
    .withNativeCredentials("user", "pass");

// 2. Connect (establishes connection)
Cluster cluster = definition.connect();

// 3. Verify connection
boolean connected = cluster.isConnected();
System.out.println("Connected: " + connected);
```

### Using the Connection

```java
// Create sessions as needed (lightweight)
Session session = cluster.createSession(Behavior.DEFAULT);

// Use for operations
session.upsert(key).bin("x").setTo(1).execute();
```

### Closing the Connection

```java
// Manual close
cluster.close();

// Or use try-with-resources (recommended)
try (Cluster cluster = definition.connect()) {
    // Use cluster
} // Automatically closed
```

## Resource Management Best Practices

### ✅ DO

**Reuse Cluster Instances**
```java
// Create once
Cluster cluster = new ClusterDefinition("localhost", 3000).connect();

// Use many times
Session session1 = cluster.createSession(Behavior.DEFAULT);
Session session2 = cluster.createSession(Behavior.DEFAULT);
```

**Use Try-With-Resources**
```java
try (Cluster cluster = definition.connect()) {
    // Use cluster
} // Automatically cleaned up
```

**Create One Cluster Per Application**
```java
public class DatabaseManager {
    private static final Cluster CLUSTER = 
        new ClusterDefinition("localhost", 3000).connect();
    
    public static Session createSession() {
        return CLUSTER.createSession(Behavior.DEFAULT);
    }
}
```

### ❌ DON'T

**Don't Create Cluster Per Operation**
```java
// ❌ Bad: Creates new connection every time
public void doOperation() {
    try (Cluster cluster = new ClusterDefinition("localhost", 3000).connect()) {
        Session session = cluster.createSession(Behavior.DEFAULT);
        // ... operation
    }
}
```

**Don't Forget to Close**
```java
// ❌ Bad: Resource leak
Cluster cluster = new ClusterDefinition("localhost", 3000).connect();
// ... use cluster
// cluster.close() never called
```

**Don't Share Across Unrelated Components**
```java
// ⚠️ Questionable: Pass session, not cluster
public void process(Cluster cluster) {  // ❌
    Session session = cluster.createSession(Behavior.DEFAULT);
}

public void process(Session session) {  // ✅
    // Use session directly
}
```

## Connection Pooling

The underlying Aerospike client maintains connection pools automatically:

- One connection pool per `Cluster` instance
- Connections are reused across operations
- Thread-safe and optimized for concurrent access
- No need for manual pooling

**Optimal Pattern**:
```java
// One cluster for entire application
private static final Cluster CLUSTER = connect();

// Multiple sessions (lightweight)
Session readSession = CLUSTER.createSession(Behavior.READ_OPTIMIZED);
Session writeSession = CLUSTER.createSession(Behavior.WRITE_OPTIMIZED);
```

## Troubleshooting

### Connection Refused

**Error**: `Connection refused: localhost:3000`

**Causes & Solutions**:
1. Aerospike not running
   ```bash
   docker ps | grep aerospike
   sudo systemctl status aerospike
   ```

2. Wrong host/port
   ```java
   // Check configuration
   new ClusterDefinition("localhost", 3000)  // Default port
   ```

3. Firewall blocking
   ```bash
   # Check port is open
   telnet localhost 3000
   ```

### Authentication Failed

**Error**: `Authentication failed`

**Solutions**:
- Verify username/password are correct
- Check user has necessary permissions
- Ensure security is enabled on server

### Cluster Name Mismatch

**Error**: `Cluster name mismatch`

**Solution**: Remove validation or fix cluster name:
```java
// Remove validation
.connect()

// Or fix cluster name
.validateClusterNameIs("correct-cluster-name")
```

### TLS Errors

**Error**: `SSL handshake failed`

**Solutions**:
- Verify certificate validity
- Check TLS version compatibility
- Ensure trust store contains correct certificates

## API Reference

See [ClusterDefinition API Reference](../api/connection/cluster-definition.md) for complete method documentation.

## Next Steps

- **[Sessions & Behavior](./sessions-and-behavior.md)** - Configure operation behavior
- **[Configuration Guide](../guides/configuration/behavior-java.md)** - Advanced configuration
- **[Troubleshooting Connections](../troubleshooting/connection-issues.md)** - Detailed troubleshooting

---

**Questions?** Check the [FAQ](../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
