# Partition Targeting

Learn how to improve query performance and enable parallel processing by targeting specific partitions.

## Goal

By the end of this guide, you'll know how to:
- Understand what partitions are in Aerospike
- Target a single partition for a query
- Target a range of partitions
- Use partition targeting to parallelize large scans

## Prerequisites

- Understanding of [Core Concepts](../../concepts/README.md)
- Familiarity with [Querying Data](../querying/simple-queries.md)

---

## What are Partitions?

Aerospike automatically divides the data within a namespace into 4096 partitions. Each record is assigned to a single partition based on a hash of its key. These partitions are then distributed evenly across the nodes in the cluster.

By default, a scan operation will visit all 4096 partitions across all nodes to find the required data. For large datasets, this can be time-consuming. **Partition targeting** allows you to restrict a scan to a specific subset of partitions, which can significantly improve performance and enable parallelization.

---

## Targeting Partitions

The `QueryBuilder` provides methods to specify which partitions a query should run on.

### 1. Targeting a Single Partition

Use `onPartition()` to restrict a query to a single partition. This is useful if you know that the data you need is located in a specific partition, but this is an advanced use case.

```java
// This query will only scan partition 1000
RecordStream results = session.query(users)
    .onPartition(1000)
    .execute();
```

### 2. Targeting a Range of Partitions

Use `onPartitionRange()` to scan a contiguous block of partitions. The start of the range is inclusive, and the end is exclusive.

```java
// This query will scan partitions 0 through 1023 (the first quarter of the keyspace)
RecordStream results = session.query(users)
    .onPartitionRange(0, 1024)
    .execute();
```

---

## Use Case: Parallelizing a Full Scan

The most powerful use case for partition targeting is to parallelize a full data scan across multiple client-side threads. You can divide the 4096 partitions among a pool of worker threads, with each thread scanning its assigned portion of the keyspace.

This pattern can dramatically reduce the time it takes to process every record in a large dataset.

### Complete Example: Parallel Word Count

This example demonstrates how to use a `ForkJoinPool` to scan the `users` set in parallel to count the occurrences of words in a "bio" bin.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

public class ParallelScan {
    private final Session session;
    private final DataSet users;

    public ParallelScan(Session session) {
        this.session = session;
        this.users = DataSet.of("test", "users");
    }

    public Map<String, Long> countWordsInBios() {
        int totalPartitions = 4096;
        int parallelism = ForkJoinPool.getCommonPoolParallelism();
        int partitionsPerThread = (totalPartitions + parallelism - 1) / parallelism;
        
        Map<String, Long> wordCounts = new ConcurrentHashMap<>();

        System.out.printf("Starting parallel scan with %d threads, %d partitions per thread...%n", 
            parallelism, partitionsPerThread);

        ForkJoinPool customPool = new ForkJoinPool(parallelism);

        try {
            customPool.submit(() -> 
                IntStream.range(0, parallelism).parallel().forEach(i -> {
                    int start = i * partitionsPerThread;
                    int end = Math.min(start + partitionsPerThread, totalPartitions);
                    
                    if (start >= end) return;

                    System.out.printf("Thread %d scanning partitions %d to %d%n", i, start, end - 1);

                    RecordStream results = session.query(users)
                        .onPartitionRange(start, end)
                        .readingOnlyBins("bio")
                        .execute();
                    
                    results.forEach(record -> {
                        String bio = record.recordOrThrow().getString("bio");
                        if (bio != null) {
                            for (String word : bio.split("\\s+")) {
                                wordCounts.merge(word.toLowerCase(), 1L, Long::sum);
                            }
                        }
                    });
                })
            ).get(); // .get() waits for the parallel stream to complete
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel scan failed", e);
        } finally {
            customPool.shutdown();
        }

        System.out.println("Parallel scan complete.");
        return wordCounts;
    }
}
```

#### How It Works:
1.  **Calculate Partition Ranges**: The 4096 partitions are divided as evenly as possible among the available threads in a `ForkJoinPool`.
2.  **Parallel Stream**: `IntStream.parallel()` is used to create a parallel stream of tasks.
3.  **Assign Work**: Each task is assigned a range of partitions (`onPartitionRange(start, end)`).
4.  **Execute Query**: Each thread executes its own query against its assigned partitions.
5.  **Aggregate Results**: Results are collected into a `ConcurrentHashMap` to handle thread-safe writes.

---

## Best Practices

- **Use for Full Scans**: Partition targeting is most effective for full dataset scans that don't use a secondary index.
- **Align with Cluster Size**: For optimal performance, the number of parallel threads on the client should have some relation to the number of nodes in the cluster, though it's not a strict requirement.
- **Stateless Logic**: The logic executed by each parallel task should be stateless and thread-safe.
- **Use `readingOnlyBins()`**: When scanning, only request the bins you absolutely need to reduce network traffic and memory usage.
- **Do Not Use with Secondary Index Queries**: Partition targeting is for scans. If you have a `where` clause that uses a secondary index, the query will be routed to the correct nodes and partitions automatically by the server. Applying a partition filter on top of that is unnecessary and may lead to unexpected results.

---

## Next Steps

- **[Performance Tuning](../performance/query-optimization.md)**: Dive deeper into other ways to optimize your application's performance.
- **[Advanced Guides](../advanced/README.md)**: Explore other advanced features of the Fluent Client.
