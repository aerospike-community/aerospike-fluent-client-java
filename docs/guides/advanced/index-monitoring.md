# Index Monitoring

Learn about the Fluent Client's automatic index monitoring feature and how it helps optimize queries.

## Goal

By the end of this guide, you'll understand:
- What the `IndexesMonitor` is and what it does
- How the client uses index information to optimize queries
- That this is an automatic, internal feature you don't typically need to manage

## Prerequisites

- Understanding of [Core Concepts](../../concepts/README.md)
- Familiarity with secondary indexes in Aerospike

---

## What is the Index Monitor?

The `IndexesMonitor` is an internal component of the Fluent Client that runs as a background daemon thread. Its sole responsibility is to periodically query the Aerospike cluster for a list of all available secondary indexes and maintain an up-to-date, in-memory cache of this information.

### How It Works

1.  **Automatic Startup**: When you create a `Cluster` object (`new ClusterDefinition(...).connect()`), an `IndexesMonitor` is created and started automatically.
2.  **Periodic Refresh**: The monitor thread wakes up at a regular interval (e.g., every 5 seconds).
3.  **Fetch Indexes**: It issues an info command to the cluster to get a list of all secondary indexes across all namespaces.
4.  **Cache Update**: It updates an internal, thread-safe `Set<Index>` with the latest index information.
5.  **Sleep**: The thread then sleeps until the next refresh interval.

This process ensures that the client has a reasonably fresh view of the available secondary indexes without needing to query the server for this information on every single query.

---

## Why is Index Monitoring Important?

The information cached by the `IndexesMonitor` is crucial for **query optimization**.

While the Fluent Client does not yet have a full query planner that will dynamically rewrite queries, the cached index information is designed to be used by future enhancements to:

-   **Automatically Choose Scan vs. Secondary Index Query**: In the future, the client could use this cache to determine if a `where()` clause can be satisfied by a secondary index. If an index exists, it could automatically use a more efficient secondary index query instead of a full partition scan.
-   **Provide Query Validation**: The client could warn you at development time if you are trying to filter on a bin that is not indexed, preventing a slow, full-scan query from accidentally being deployed to production.
-   **Improve DSL Performance**: The type-safe DSL can leverage this information to make more intelligent decisions about how to construct filter expressions.

For now, this feature is primarily foundational, setting the stage for more advanced query optimization capabilities in future releases.

---

## Do I Need to Do Anything?

**No, not usually.**

The `IndexesMonitor` is an internal, automatic feature.
-   It is **started automatically** when you create a `Cluster`.
-   It is **stopped automatically** when you call `cluster.close()`.
-   It runs as a **daemon thread**, so it will not prevent your application from shutting down.

You do not need to interact with it directly. Its operation is transparent.

---

## Observing the Monitor (Advanced)

While you don't need to manage it, you can observe its effects. For example, if you create a new secondary index in your cluster:

1.  Start your application and connect to the cluster. The `IndexesMonitor` starts.
2.  Use `aql` to create a new index:
    ```sql
    CREATE INDEX idx_users_on_email ON test.users (email) STRING
    ```
3.  Within the monitor's refresh interval, the Fluent Client will become aware of this new index automatically. Future queries that could benefit from this index will be able to use it (once the query planner is implemented).

The main takeaway is that the client is designed to be **adaptive** to changes in the cluster's schema.

---

## Best Practices

-   **Let it Run**: There is no need to disable the `IndexesMonitor`. Its overhead is minimal, and it's essential for future query performance enhancements.
-   **Proper Shutdown**: Always close your `Cluster` object (preferably with a `try-with-resources` block) to ensure the `IndexesMonitor` and other resources are shut down gracefully.

```java
// Proper lifecycle management ensures the monitor is cleaned up
try (Cluster cluster = new ClusterDefinition("localhost", 3000).connect()) {
    // Your application logic...
} // cluster.close() is called automatically, which stops the monitor.
```

---

## Next Steps

- **[Namespace Information](./namespace-info.md)**: Learn about other info commands for monitoring your cluster.
- **[Query Optimization](../performance/query-optimization.md)**: Explore other ways to improve query performance.

