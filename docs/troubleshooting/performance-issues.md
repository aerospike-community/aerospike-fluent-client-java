# Troubleshooting Performance Issues

This guide provides a starting point for diagnosing and resolving performance issues with the Aerospike Fluent Client.

## Common Performance Problems

### 1. High Latency

**Symptoms:**
- Individual operations (`upsert`, `query`) are taking longer than expected.
- Your application is experiencing frequent timeouts.

**Potential Causes:**
- **Network Latency**: High network latency between your application and the Aerospike cluster.
- **Server Overload**: The Aerospike cluster is under-provisioned and cannot handle the current load.
- **Inefficient Queries**: Using full scans instead of secondary index queries for large datasets.
- **Large Records**: Reading or writing very large records can increase latency.
- **Incorrect Timeout Configuration**: Timeouts might be set too low, causing operations to fail prematurely.

**Solutions:**
- **Check Network**: Use tools like `ping` to measure network latency.
- **Monitor Server**: Use the Aerospike Monitoring Stack or `asadm` to check the server's CPU, memory, and disk usage.
- **Optimize Queries**: Ensure you have secondary indexes for your common query patterns. Use the `where()` clause to filter on indexed bins. See the [Query Optimization](../../guides/performance/query-optimization.md) guide.
- **Review Data Model**: Avoid storing very large objects in a single record. Consider splitting them into multiple records.
- **Tune Timeouts**: Adjust your `abandonCallAfter` settings to be appropriate for your workload. See the [Timeout Configuration](../../guides/performance/timeout-configuration.md) guide.

### 2. Low Throughput

**Symptoms:**
- Your application is not achieving the desired number of operations per second.

**Potential Causes:**
- **Client-Side Bottleneck**: The client application may not be able to generate load fast enough. This can be due to single-threaded processing or other application logic.
- **Connection Pool Saturation**: The connection pool may be too small for the number of concurrent requests.
- **Server Overload**: The server may be the bottleneck.

**Solutions:**
- **Parallelize on the Client**: Use a multi-threaded approach to send requests to Aerospike in parallel.
- **Use Batch Operations**: For bulk reads or writes, use batch operations to reduce the number of round trips. See the [Batch Operations](../../guides/performance/batch-operations.md) guide.
- **Review Connection Pool**: While the defaults are usually sufficient, you may need to investigate advanced tuning if you have an extremely high-throughput application. See the [Connection Pooling](../../guides/performance/connection-pooling.md) guide.
- **Scale the Cluster**: If the server is the bottleneck, you may need to add more nodes to your Aerospike cluster.

---

## Performance Tuning Checklist

- Are you using the latest version of the Fluent Client?
- Are you closing your `Cluster` and `Session` objects correctly?
- Are you using secondary indexes for your queries?
- Are you using batch operations for bulk workloads?
- Have you configured appropriate timeouts?
- Is your Aerospike cluster adequately provisioned for your workload?

---

## Next Steps

- **[Query Optimization](../../guides/performance/query-optimization.md)**
- **[Batch Operations](../../guides/performance/batch-operations.md)**
- **[Timeout Configuration](../../guides/performance/timeout-configuration.md)**
- **[Connection Pooling](../../guides/performance/connection-pooling.md)**
