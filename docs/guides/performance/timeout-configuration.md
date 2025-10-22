# Timeout Configuration

Learn how to configure timeouts to control how long the client waits for operations to complete, preventing your application from hanging on slow or unresponsive cluster nodes.

## Goal

By the end of this guide, you'll know how to:
- Understand the primary timeout setting: `abandonCallAfter`
- Configure timeouts for all operations globally
- Set specific timeouts for different types of operations (e.g., reads vs. writes)
- Configure timeouts programmatically (Java) and via YAML

## Prerequisites

- Understanding of [Sessions & Behavior](../../concepts/sessions-and-behavior.md)
- Familiarity with either [Java-based](./behavior-java.md) or [YAML-based](./yaml-configuration.md) behavior configuration

---

## The `abandonCallAfter` Timeout

The most important timeout setting in the Fluent Client is `abandonCallAfter`. This is the total transaction timeout in milliseconds.

- **What it does**: It specifies the maximum amount of time the client will wait for an entire operation (including all retries) to complete.
- **When it starts**: The timer starts when the transaction is initiated.
- **What happens on timeout**: If the timeout is reached, the client will stop waiting for a response and throw an `AerospikeException.Timeout`.

This timeout is crucial for ensuring your application remains responsive and does not wait indefinitely for a slow or failing database operation.

---

## Configuring Timeouts

You can configure timeouts at different levels of granularity. Policies are inherited, so you can set a default timeout and override it for specific operation types.

### Hierarchy of Policies

1.  **`forAllOperations()`**: The base level. Settings here apply to all operations unless overridden.
2.  **Operation-Specific**: You can override the base settings for specific categories like reads, writes, queries, etc.

### 1. Configuring a Global Timeout

It's a best practice to set a default timeout for all operations.

#### Using Java Builder:
```java
Behavior customBehavior = Behavior.DEFAULT.deriveWithChanges("custom", builder ->
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(5)) // 5-second timeout for all operations
    .done()
);
```

#### Using YAML:
```yaml
behaviors:
  - name: "custom"
    parent: "default"
    allOperations:
      abandonCallAfter: "5s" # 5-second timeout for all operations
```

### 2. Configuring Specific Timeouts

You can set different timeouts for different types of operations. For example, you might want a shorter timeout for fast reads and a longer one for complex queries or writes.

#### Using Java Builder:
```java
Behavior readWriteTimeouts = Behavior.DEFAULT.deriveWithChanges("read-write-timeouts", builder ->
    builder
        // Set a default timeout first
        .forAllOperations()
            .abandonCallAfter(Duration.ofSeconds(10))
        .done()
        
        // Override for fast availability-mode reads
        .onAvailablityModeReads()
            .abandonCallAfter(Duration.ofMillis(500))
        .done()

        // Override for critical, retryable writes
        .onRetryableWrites()
            .abandonCallAfter(Duration.ofSeconds(3))
        .done()
        
        // Override for long-running queries
        .onQuery()
            .abandonCallAfter(Duration.ofMinutes(1))
        .done()
);
```

#### Using YAML:
```yaml
behaviors:
  - name: "read-write-timeouts"
    parent: "default"
    # Default for all operations
    allOperations:
      abandonCallAfter: "10s"
      
    # Override for availability-mode reads
    availabilityModeReads:
      abandonCallAfter: "500ms"
      
    # Override for retryable writes
    retryableWrites:
      abandonCallAfter: "3s"

    # Override for queries
    query:
      abandonCallAfter: "1m"
```

---

## Complete Example: Applying Timeout Behaviors

This example shows how to create sessions with different timeout behaviors for different parts of your application.

```java
public class DataService {
    private final Session fastReadSession;
    private final Session reliableWriteSession;
    private final Session querySession;

    public DataService(Cluster cluster) {
        // Assume the "read-write-timeouts" behavior from above is defined
        // either programmatically or loaded from YAML.
        
        Behavior timeoutBehavior = Behavior.getBehavior("read-write-timeouts");

        // Create different sessions for different needs
        this.fastReadSession = cluster.createSession(timeoutBehavior);
        this.reliableWriteSession = cluster.createSession(timeoutBehavior);
        this.querySession = cluster.createSession(timeoutBehavior);
    }

    public Optional<User> getQuickUser(String userId) {
        // This operation will use the 500ms timeout defined in `onAvailablityModeReads`
        return fastReadSession.query(users.id(userId)).execute().getFirst(userMapper);
    }

    public void saveUser(User user) {
        // This operation will use the 3s timeout from `onRetryableWrites`
        reliableWriteSession.upsert(users).object(user).execute();
    }

    public List<User> findInactiveUsers() {
        // This operation will use the 1m timeout from `onQuery`
        return querySession.query(users)
            .where("$.status == 'INACTIVE'")
            .execute()
            .toObjectList(userMapper);
    }
}
```

---

## Other Timeout-Related Settings

While `abandonCallAfter` is the primary timeout, there are other related settings in `ClientPolicy` that are not directly exposed in the Fluent Client's `Behavior` builder but are worth knowing about:

- **`connectTimeout`**: The time to wait when opening a new connection to the server. Default is 5000ms. This is configured on the `ClusterDefinition` level.
- **`sleepBetweenRetries`**: The delay between retry attempts for a transaction. This is part of the overall `abandonCallAfter` time.

---

## Best Practices

- **Set Realistic Timeouts**: Don't set timeouts too low, as this can cause operations to fail unnecessarily under normal load. Don't set them too high, or your application may become unresponsive.
- **Profile Your Application**: Measure the typical latency of your database operations to determine a reasonable baseline for your timeouts.
- **Use Specific Overrides**: Set a sensible global default, and then override it for specific operations that you know will be faster or slower.
- **Align with Retries**: Ensure your `abandonCallAfter` timeout is long enough to accommodate all potential retry attempts. For example, if you have `maximumNumberOfCallAttempts = 3` and `delayBetweenRetries = 100ms`, your timeout should be greater than `(operation_time * 3) + (100ms * 2)`.

---

## Next Steps

- **[Connection Pooling](./connection-pooling.md)**: Understand how the client manages connections.
- **[Sessions & Behavior](../../concepts/sessions-and-behavior.md)**: Revisit the core concepts of behavior configuration.
