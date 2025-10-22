# Behavior Configuration (Java)

Learn how to fine-tune the client's operational policies, such as timeouts, retries, and consistency, using the `Behavior` class.

## Goal

By the end of this guide, you'll know how to:
- Understand the purpose and structure of `Behavior`
- Create new behaviors by deriving from `Behavior.DEFAULT`
- Configure policies for different operation categories
- Create a hierarchy of behaviors using inheritance
- Apply behaviors to a `Session`

## Prerequisites

- [Core Concepts](../../concepts/sessions-and-behavior.md)
- Familiarity with the Java builder pattern

---

## What is `Behavior`?

A `Behavior` is an **immutable** object that holds a collection of policies controlling how the client executes database operations. You can think of it as a template of settings for timeouts, retries, durability, consistency, and more.

### Key Concepts

- **Immutability**: Once a `Behavior` is created, it cannot be changed. This makes it thread-safe and reusable.
- **Inheritance**: Behaviors can inherit from other behaviors, allowing you to create a hierarchy of policies.
- **Categorization**: Policies are grouped by operation type (e.g., reads, writes, batch operations), allowing for granular control.

---

## Creating and Deriving Behaviors

All custom behaviors are created by deriving from an existing one, usually `Behavior.DEFAULT`.

### The Builder Pattern

You create a new `Behavior` using `deriveWithChanges()`, which provides a fluent builder.

```java
Behavior newBehavior = Behavior.DEFAULT.deriveWithChanges("newBehaviorName", builder -> {
    // ... configure the builder here ...
});
```

### Configuring Operation Categories

The builder allows you to specify policies for different categories of operations.

**Example**: Set a custom timeout for all read operations that can tolerate stale data.

```java
Behavior fastReadsBehavior = Behavior.DEFAULT.deriveWithChanges("fast-reads", builder -> 
    builder.onAvailablityModeReads() // Select the category
        .waitForCallToComplete(Duration.ofMillis(50))
        .abandonCallAfter(Duration.ofMillis(200))
        .maximumNumberOfCallAttempts(2)
    .done() // Finalize the category configuration
);
```

### Key Operation Categories

- `forAllOperations()`: A base configuration that applies to all categories.
- `onReads()`: Applies to all single-record read operations.
- `onWrites()`: Applies to all single-record write operations (insert, update, upsert).
- `onDeletes()`: Applies to all single-record delete operations.
- `onBatchReads()`: Applies to batch read operations.
- `onBatchWrites()`: Applies to batch write operations.
- `onScans()`: Applies to scan operations.
- `onQueries()`: Applies to query operations.
- `onAvailablityModeReads()`: Reads that can tolerate stale data (e.g., `replica: ANY`).
- `onStrongConsistencyModeWrites()`: Writes that require strong consistency.

---

## Common Policy Configurations

### Timeouts

- `waitForCallToComplete()`: The timeout for a single attempt to a server node.
- `abandonCallAfter()`: The total timeout for an operation, including all retries.

```java
builder.forAllOperations()
    .waitForCallToComplete(Duration.ofMillis(100))
    .abandonCallAfter(Duration.ofSeconds(1))
.done()
```

### Retries

- `maximumNumberOfCallAttempts()`: The maximum number of times to try an operation before failing.
- `delayBetweenRetries()`: The duration to wait between retries.

```java
builder.onWrites()
    .maximumNumberOfCallAttempts(3)
    .delayBetweenRetries(Duration.ofMillis(10))
.done()
```

### Replica and Consistency

- `replicaOrder()`: The order in which to try replicas (`MASTER`, `ANY_REPLICA`, `MASTER_PROLES`).
- `useDurableDelete()`: Ensures deletes are permanent and not lost during cluster changes.

```java
// Prioritize reads from replicas to reduce load on the master
builder.onReads()
    .replicaOrder(Replica.MASTER_PROLES)
.done()

// Ensure strong consistency for writes
builder.onStrongConsistencyModeWrites()
    .useDurableDelete(true)
.done()
```

---

## Creating a Behavior Hierarchy

You can create a parent-child relationship between behaviors. Child behaviors inherit and can override settings from their parent.

### 1. Create a Base Behavior

```java
Behavior baseBehavior = Behavior.DEFAULT.deriveWithChanges("base", builder ->
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(5)) // Global timeout
    .done()
    .onReads()
        .maximumNumberOfCallAttempts(3) // Default retries for reads
    .done()
);
```

### 2. Create a Child Behavior

The child inherits all settings from `baseBehavior` and can modify them or add new ones.

```java
Behavior aggressiveReads = baseBehavior.deriveWithChanges("aggressive-reads", builder ->
    builder.onReads() // Target the reads category
        .maximumNumberOfCallAttempts(5) // Override the parent's setting
        .waitForCallToComplete(Duration.ofMillis(50)) // Add a new setting
    .done()
);
```

**Result**: `aggressiveReads` will have an `abandonCallAfter` of 5 seconds (from `baseBehavior`) and `maximumNumberOfCallAttempts` of 5 for reads (overridden).

---

## Applying Behaviors to a Session

A `Session` is created with a specific `Behavior`. All operations performed with that session will use the policies defined in its behavior.

```java
// Create a session that uses our custom behavior
Session aggressiveSession = cluster.createSession(aggressiveReads);

// All operations on this session will use the "aggressive-reads" policies
aggressiveSession.query(users.id("alice")).execute();

// You can also create sessions with other behaviors
Session standardSession = cluster.createSession(baseBehavior);
```

This allows you to use different operational strategies for different parts of your application.

---

## Complete Example: Web Application Behaviors

```java
public class BehaviorFactory {

    public static final Behavior DEFAULT_BEHAVIOR;
    public static final Behavior READ_ONLY_API;
    public static final Behavior CRITICAL_WRITE_API;
    
    static {
        // Base behavior for the whole application
        DEFAULT_BEHAVIOR = Behavior.DEFAULT.deriveWithChanges("app-default", builder ->
            builder.forAllOperations()
                .abandonCallAfter(Duration.ofSeconds(2))
            .done()
        );
        
        // Behavior for endpoints that only read data, can be more aggressive with retries
        READ_ONLY_API = DEFAULT_BEHAVIOR.deriveWithChanges("read-only", builder ->
            builder.onReads()
                .maximumNumberOfCallAttempts(4)
                .replicaOrder(Replica.MASTER_PROLES) // Prefer replicas
            .done()
            .onScans()
                .waitForCallToComplete(Duration.ofSeconds(5)) // Allow longer scans
            .done()
        );
        
        // Behavior for critical financial transactions, less retries, faster timeouts
        CRITICAL_WRITE_API = DEFAULT_BEHAVIOR.deriveWithChanges("critical-write", builder ->
            builder.onWrites()
                .maximumNumberOfCallAttempts(2) // Fail faster
                .waitForCallToComplete(Duration.ofMillis(200))
            .done()
        );
    }
}

// In your application logic:
public class TransactionService {
    private final Session criticalSession;
    
    public TransactionService(Cluster cluster) {
        this.criticalSession = cluster.createSession(BehaviorFactory.CRITICAL_WRITE_API);
    }
    
    public void executeTransaction(Transaction tx) {
        // These operations will use the fast-fail, critical write policies
        criticalSession.update(...).execute();
    }
}
```

---

## Best Practices

### ✅ DO

**Create a base behavior for your application.**
This provides a consistent foundation and makes future changes easier.

**Create specialized behaviors for different use cases.**
For example, a high-throughput read path might have different retry logic than a low-latency write path.

**Store your `Behavior` objects as constants.**
Since they are immutable and thread-safe, they can be safely shared and reused.

**Use descriptive names for your behaviors.**
`"read-heavy-api"` is better than `"behavior1"`.

### ❌ DON'T

**Don't create new behaviors for every operation.**
This is inefficient. Create them once at startup and reuse them.

**Don't make overly complex inheritance chains.**
One or two levels of inheritance are usually sufficient.

---

## Next Steps

- **[YAML Configuration](./yaml-configuration.md)** - Learn how to externalize your behavior definitions into YAML files for dynamic configuration.
- **[API Reference](../../api/behavior.md)** - For a complete list of all policy settings.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
