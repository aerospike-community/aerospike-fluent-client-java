# `BehaviorBuilder`

A builder for defining and modifying `Behavior` policies.

`com.aerospike.policy.BehaviorBuilder`

## Overview

The `BehaviorBuilder` is a fluent API used to construct or modify the policies within a `Behavior` object. You don't typically create an instance of `BehaviorBuilder` directly. Instead, you receive one in the lambda expression when you call `Behavior.deriveWithChanges()`.

The builder provides a structured way to target specific types of database operations and set policies for them.

## Builder Structure

The builder uses a `for...()` and `on...()` pattern to select an operation type, followed by methods to set specific policy values, and finally a `.done()` call to return to the main `BehaviorBuilder`.

**General Pattern:**
```java
builder
    .on[OperationType]()       // e.g., onQuery(), onRetryableWrites()
        .policyMethod1(value)  // e.g., abandonCallAfter(Duration.ofSeconds(5))
        .policyMethod2(value)
    .done()
    
    .forAllOperations()
        .policyMethod3(value)
    .done();
```

## Operation Selectors

These methods select which category of operations the subsequent policy settings will apply to.

- **`forAllOperations()`**: Applies settings to all operation types. This is used to set the base policies.
- **`onConsistencyModeReads()`**: Targets standard single-record reads (`query(key)`).
- **`onAvailablityModeReads()`**: Targets replica reads in AP namespaces.
- **`onRetryableWrites()`**: Targets write operations that can be safely retried (e.g., `upsert`).
- **`onNonRetryableWrites()`**: Targets write operations that are not idempotent (e.g., `add()`).
- **`onBatchReads()`**: Targets batch read operations (`query(keys)`).
- **`onBatchWrites()`**: Targets batch write operations.
- **`onQuery()`**: Targets queries and scans (`query(dataSet)`).
- **`onInfo()`**: Targets info commands (`session.info()`).

## Common Policy Methods

The following are some of the most commonly used policy methods, available after an operation selector.

### Timeouts

- **`abandonCallAfter(Duration duration)`**: Sets the total timeout for an operation, including all retries. This is the primary timeout setting.
- **`waitForCallToComplete(Duration duration)`**: Sets the socket timeout for a single network call.

### Retries

- **`maximumNumberOfCallAttempts(int attempts)`**: Sets the maximum number of times an operation will be attempted. `1` means no retries.
- **`delayBetweenRetries(Duration duration)`**: Sets the sleep duration between retry attempts.

### Read/Write Behavior

- **`sendKey(boolean sendKey)`**: If `true`, the key is sent to the server for all operations. Required for features like the `key-ordered` UDF.
- **`useCompression(boolean useCompression)`**: If `true`, data is compressed before being sent to the server.
- **`useDurableDelete(boolean durableDelete)`**: If `true`, ensures a delete operation is fully persisted before returning.

### Read Consistency (for SC namespaces)

- **`readConsistency(ReadModeSC mode)`**: Sets the read consistency level for Strongly Consistent namespaces. (e.g., `ReadModeSC.SESSION`, `ReadModeSC.LINEARIZABLE`).

### Query/Scan Policies

- **`recordQueueSize(int size)`**: Sets the size of the client-side queue for records returned by a query.
- **`maxConcurrentServers(int max)`**: Sets the maximum number of server nodes to query in parallel.

## Example

This example creates a sophisticated behavior with different policies for different operations.

```java
import com.aerospike.policy.Behavior;
import com.aerospike.client.policy.ReadModeSC;
import java.time.Duration;

Behavior customBehavior = Behavior.DEFAULT.deriveWithChanges("my-app-behavior", builder -> {
    // --- Base policies for ALL operations ---
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(10))  // Default 10s timeout
        .maximumNumberOfCallAttempts(3)          // Default 3 attempts
    .done();

    // --- Policies specifically for single-record reads ---
    builder.onConsistencyModeReads()
        .abandonCallAfter(Duration.ofMillis(500)) // Faster timeout for point reads
        .readConsistency(ReadModeSC.SESSION)       // Use session consistency
    .done();

    // --- Policies for writes that can be retried ---
    builder.onRetryableWrites()
        .delayBetweenRetries(Duration.ofMillis(50)) // Wait 50ms between retries
    .done();

    // --- Policies for long-running queries/scans ---
    builder.onQuery()
        .abandonCallAfter(Duration.ofMinutes(5)) // Allow queries to run for 5 minutes
        .recordQueueSize(10000)                  // Larger buffer for results
    .done();
});

// Now, this single Behavior object can be used to create sessions
// that are optimized for different tasks.
Session readSession = cluster.createSession(customBehavior);
Session writeSession = cluster.createSession(customBehavior);
```

## Related Classes

- **[`Behavior`](./behavior.md)**: The class that is configured by this builder.

## See Also

- **[Guide: Behavior Configuration (Java)](../../guides/configuration/behavior-java.md)**
