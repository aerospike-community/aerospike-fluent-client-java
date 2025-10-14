# Connection Pooling

Learn how the Fluent Client manages connection pools to ensure efficient and high-performance communication with the Aerospike cluster.

## Goal

By the end of this guide, you'll understand:
- What connection pooling is and why it's important
- How the Fluent Client manages connection pools automatically
- That the default settings are suitable for most applications
- Where to find information for advanced tuning (if needed)

## Prerequisites

- Understanding of [Core Concepts](../../concepts/README.md), especially `Cluster` and `ClusterDefinition`.

---

## What is Connection Pooling?

Establishing a network connection to a database server can be a slow process. A connection pool is a cache of database connections maintained by the client so that connections can be reused for future requests.

**Benefits of Connection Pooling:**
- **Reduced Latency**: Avoids the overhead of creating a new TCP connection for every database operation.
- **Improved Throughput**: Allows for a high rate of concurrent operations by reusing a managed set of connections.
- **Resource Management**: Prevents the client from exhausting socket resources by limiting the total number of open connections.

The Aerospike Fluent Client, via the underlying traditional Java client, uses a sophisticated, asynchronous connection pooling mechanism to manage connections to each node in the cluster.

---

## Automatic and Transparent Management

For developers using the Fluent Client, **connection pooling is managed automatically and transparently**.

- **Automatic Creation**: When you create a `Cluster` object, a connection pool is created for each node in the Aerospike cluster.
- **Asynchronous Nature**: The client uses an asynchronous, multiplexed I/O model. This means a small number of connections can handle a large number of concurrent requests, making it highly efficient.
- **Dynamic Sizing**: The pool will automatically grow and shrink based on workload, up to a configured maximum.
- **Health Checks**: The client periodically checks the health of connections in the pool and removes stale or broken connections.

You do **not** need to manually configure, create, or manage connection pools. The default settings are optimized for a wide range of workloads and are sufficient for the vast majority of applications.

---

## Default Configuration

The Fluent Client uses the default `ClientPolicy` settings from the traditional Aerospike Java client for connection pooling. These defaults include:

- **`maxConnsPerNode`**: The maximum number of asynchronous connections allowed per server node. The default is **300**.
- **`connPoolsPerNode`**: The number of synchronous connection pools per server node. The default is **1**. *(Note: The Fluent Client primarily uses the async architecture)*.
- **`connectTimeout`**: Timeout for creating a new connection. Default is **5000 ms**.

These defaults provide a high degree of concurrency and are rarely a bottleneck.

---

## Advanced Tuning (When and How)

While the `ClusterDefinition` builder in the Fluent Client does **not currently expose direct methods** for tuning connection pool parameters (like `maxConnsPerNode`), it is an area for future enhancement.

### When Might You Need to Tune?

- **Extremely High Throughput Applications**: If you have an application that needs to sustain an exceptionally high number of concurrent operations per second (e.g., hundreds of thousands), you might need to increase the pool size.
- **Resource-Constrained Environments**: If your client application is running in an environment with very limited memory or socket handles, you might consider lowering the pool size.

### How to Tune (Currently)

If you have a critical need for advanced tuning, you would need to drop down to the traditional `ClientPolicy` and `AerospikeClient` from the underlying Java client.

```java
import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.policy.ClientPolicy;

// 1. Create a traditional ClientPolicy
ClientPolicy policy = new ClientPolicy();

// 2. Set custom connection pool size
policy.maxConnsPerNode = 500; // Increase max connections

// 3. (Optional) Set user/password, timeouts, etc.
policy.user = "my-user";
policy.password = "my-pass";

// 4. Create a traditional AerospikeClient
AerospikeClient traditionalClient = new AerospikeClient(policy, new Host("localhost", 3000));

// 5. Wrap it in a Fluent Client Cluster object
// Note: This constructor is package-private and may not be available in future versions.
// This is an advanced, off-label use case.
Cluster cluster = new Cluster(traditionalClient);

// Now you can use the fluent API with the custom-tuned client
Session session = cluster.createSession(Behavior.DEFAULT);
// ...
```

> **⚠️ Warning**: The code above demonstrates an advanced technique that bypasses the standard `ClusterDefinition` builder. The `Cluster(IAerospikeClient)` constructor is package-private and its visibility or existence is not guaranteed in future versions. You should only consider this approach if default settings have been proven to be a bottleneck through performance testing.

---

## Best Practices

- **Stick to the Defaults**: For 99% of use cases, the default connection pool settings are optimal.
- **Focus on Application Logic**: Spend time optimizing your data model and query patterns rather than tuning the connection pool.
- **Monitor Your Application**: Use monitoring tools to observe your application's resource usage (CPU, memory, network). If you suspect a bottleneck, use a profiler to identify the root cause before assuming it's the connection pool.
- **Properly Close the `Cluster`**: Always use a `try-with-resources` block or explicitly call `cluster.close()` to ensure the connection pools are shut down gracefully and all connections are terminated.

```java
try (Cluster cluster = new ClusterDefinition("localhost", 3000).connect()) {
    // Your code here...
} // Connections are automatically closed here
```

---

## Next Steps

- **[Timeout Configuration](./timeout-configuration.md)**: Learn how to configure timeouts for individual operations, which is a more common performance tuning technique.
- **[Sessions & Behavior](../../concepts/sessions-and-behavior.md)**: Revisit how behaviors control the policies for operations that use the connection pool.

