# Batch Operations

Learn how to perform high-performance, bulk operations on multiple records in a single network call.

## Goal

By the end of this guide, you'll know how to:
- Perform batch reads, writes, and deletes
- Understand the performance benefits of batching
- Differentiate between batch operations and transactions
- Configure batch-specific policies in your `Behavior`

## Prerequisites

- [Core Concepts](../../concepts/README.md)
- Understanding of network latency and round-trips

---

## Why Use Batch Operations?

Every database operation involves a network round-trip between the client and the server. Batch operations allow you to bundle many individual operations (reads, writes, deletes) on different records into a **single network call**. This dramatically reduces network overhead and can increase throughput by orders of magnitude.

**Scenario**: You need to read 100 user profiles.

**Without Batching (Bad)**:
```java
// 100 network round-trips
for (String userId : userIds) {
    session.query(users.id(userId)).execute();
}
```

**With Batching (Good)**:
```java
// 1 network round-trip
List<Key> userKeys = users.ids(userIds);
session.query(userKeys).execute();
```

---

## Types of Batch Operations

### Batch Reads

Read multiple records by their keys in a single operation.

```java
DataSet users = DataSet.of("test", "users");
List<String> userIds = List.of("user-1", "user-2", "user-3");

// 1. Create a list of keys
List<Key> userKeys = users.ids(userIds);

// 2. Perform the batch read
RecordStream results = session.query(userKeys).execute();

// 3. Process the results
results.forEach(record -> {
    System.out.println("Found user: " + record.key.userKey);
});
```

**Result Order**: The `RecordStream` will return the records in the same order as the keys you provided. If a key is not found, it is simply omitted from the results.

### Batch Writes (Upsert)

Write to multiple records in a single operation. You can either write the **same data** to all records or provide **different data** for each record.

**Same Data for All Records**:
```java
// Deactivate a list of users
List<Key> keysToDeactivate = users.ids("user-4", "user-5");

session.update(keysToDeactivate)
    .bin("isActive").setTo(false)
    .bin("deactivatedAt").setTo(System.currentTimeMillis())
    .execute();
```

**Different Data for Each Record**:
```java
// Update the ages for multiple users
session.upsert(users.ids("user-1", "user-2"))
    .bins("name", "age")
    .values("Alice", 31) // For user-1
    .values("Bob", 46)   // For user-2
    .execute();
```

### Batch Deletes

Delete multiple records by their keys.

```java
List<Key> keysToDelete = users.ids("user-6", "user-7");

session.delete(keysToDelete).execute();
```
This is a "fire and forget" operation and does not return information about which records were successfully deleted.

---

## Batch Operations vs. Transactions

It's crucial to understand the difference between batch operations and transactions.

| Feature | Batch Operation | Transaction |
|---|---|---|
| **Atomicity** | **Per-record** | **All-or-nothing** |
| **Consistency** | Eventual | Atomic |
| **Isolation** | None | Isolated |
| **Durability** | Per-record | Atomic |
| **Performance** | **Very high** | Lower (due to coordination) |

- A **batch operation** is a performance optimization. It bundles many independent operations together. If one operation in the batch fails, the others are **not** affected.
- A **transaction** is a correctness guarantee. It ensures that a group of operations either all succeed or all fail together.

**Use a batch operation when**: You need to perform many similar, independent operations and want maximum throughput.
**Use a transaction when**: You need to ensure a multi-step process is atomic (e.g., transferring funds between two accounts).

---

## Configuring Batch Policies

You can fine-tune the behavior of batch operations using a custom `Behavior`.

**Key Policies for Batches**:
- `onBatchReads()` / `onBatchWrites()`: Categories for batch-specific policies.
- `maxConcurrentServers()`: The maximum number of server nodes the client will send concurrent batch requests to. The default is 0 (no limit).
- `allowInlineMemoryAccess()` / `allowInlineSsdAccess()`: Performance optimizations that allow the server to process sub-tasks of a batch request in the same thread, reducing context switching.

**Example**: Create a behavior optimized for large batch write jobs.

```java
Behavior batchWriteBehavior = Behavior.DEFAULT.deriveWithChanges("batch-write", builder ->
    builder.onBatchWrites()
        .abandonCallAfter(Duration.ofSeconds(10)) // Allow longer total timeout
        .maxConcurrentServers(8) // Limit concurrency to 8 nodes at a time
        .allowInlineSsdAccess(true) // Optimize for SSD namespaces
    .done()
);

Session batchSession = cluster.createSession(batchWriteBehavior);

// All batch writes on this session will use the optimized policies
batchSession.upsert(aLargeListOfKeys)...execute();
```

---

## Best Practices

### ✅ DO

**Always use batch operations for more than a handful of records.**
The performance improvement is significant.

**Batch reads for UI displays.**
When a user interface needs to display data from 20 different records, fetch them all in a single batch read instead of 20 individual reads.

**Use batch writes for data import/ETL jobs.**
Process records in large batches (e.g., hundreds or thousands at a time) for maximum ingest speed.

**Tune batch policies in your `Behavior`.**
Adjusting concurrency and timeouts can have a big impact on the performance of large batch jobs.

### ❌ DON'T

**Don't create batches that are too large.**
While there's no hard limit, extremely large batches (e.g., hundreds of thousands of keys) can consume significant client-side memory and might lead to timeouts. It's often better to break a very large job into multiple, moderately-sized batches.

**Don't use a batch operation if you need all-or-nothing atomicity.**
Use a `TransactionalSession` for that.

---

## Next Steps

- **[Query Optimization](./query-optimization.md)** - Learn how to make your queries and scans more efficient.
- **[API Reference: `Behavior`](../../api/behavior.md)** - See all available policy settings.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
