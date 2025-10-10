# Sessions & Behavior

Learn how to create sessions and configure operational behavior in the Fluent Client.

## Overview

A **Session** is your main interface for performing database operations. It's configured with a **Behavior** that controls how operations are executed (timeouts, retries, consistency, etc.).

```
Cluster
   ↓ .createSession(Behavior)
Session (+ Behavior Configuration)
   ↓
Operations (upsert, query, delete, etc.)
```

### Session and Behavior Relationship Diagram

```
┌───────────────────────────────────────────────────────────────┐
│                          Cluster                               │
│  (Manages connection to Aerospike)                             │
└─────┬─────────┬──────────┬──────────┬─────────────────────────┘
      │         │          │          │
      │ create  │ create   │ create   │ create
      ▼         ▼          ▼          ▼
   ┌──────┐ ┌──────┐  ┌──────┐  ┌──────┐
   │Session│ │Session│  │Session│  │Session│
   │   +   │ │   +   │  │   +   │  │   +   │
   │Behavior││Behavior│ │Behavior│ │Behavior│
   └──────┘ └──────┘  └──────┘  └──────┘
      │         │          │          │
      │         │          │          │
   Fast     Durable    Batch      Custom
   Reads    Writes   Optimized   Timeout
   
┌─────────────────────────────────────────────────────────────────┐
│  Each Session operates independently with its own Behavior      │
│  • Different timeouts                                            │
│  • Different retry policies                                      │
│  • Different consistency levels                                  │
│  • Different query settings                                      │
└─────────────────────────────────────────────────────────────────┘
```

**Key Concept**: One Cluster can spawn multiple Sessions, each with different Behaviors tailored to specific use cases.

### Behavior Configuration Layers

```
┌──────────────────────────────────────────────────────────┐
│                    Behavior Hierarchy                     │
└──────────────────────────────────────────────────────────┘

Level 1: Base Behavior (DEFAULT, etc.)
   ↓
Level 2: Derive with changes (.deriveWithChanges())
   ↓
Level 3: Configuration Scopes
   ├── forAllOperations()       [Applies to all]
   ├── onRetryableWrites()      [Overrides for writes]
   ├── onQuery()                [Overrides for queries]
   ├── onBatchReads()           [Overrides for batch reads]
   └── onInfo()                 [Overrides for info commands]

Priority: Specific scope > All operations > Base behavior

Example:
┌──────────────────────────────────────────────────────────┐
│ Behavior.DEFAULT                                          │
│   └─ forAllOperations()                                   │
│        • abandonCallAfter: 30s       ← Applied to all    │
│   └─ onQuery()                                            │
│        • recordQueueSize: 5000       ← Only for queries  │
│        • abandonCallAfter: 60s       ← Overrides general │
└──────────────────────────────────────────────────────────┘
```

## Creating a Session

### Basic Session

```java
Cluster cluster = new ClusterDefinition("localhost", 3000).connect();
Session session = cluster.createSession(Behavior.DEFAULT);
```

**Key Points**:
- Sessions are lightweight
- Can create multiple sessions from one cluster
- Each session can have different behavior
- Sessions are thread-safe (but see caveats below)

### Multiple Sessions

Create different sessions for different use cases:

```java
// Fast, eventually consistent reads
Session readSession = cluster.createSession(Behavior.FAST_READS);

// Durable, strongly consistent writes
Session writeSession = cluster.createSession(Behavior.DURABLE_WRITES);

// High-throughput batch operations
Session batchSession = cluster.createSession(Behavior.BATCH_OPTIMIZED);
```

## Understanding Behavior

`Behavior` encapsulates all configuration for database operations:

- **Timeouts**: How long to wait for operations
- **Retries**: How many times to retry failed operations
- **Consistency**: Read consistency levels
- **Concurrency**: Query parallelization settings
- **Compression**: Whether to compress data

### Default Behavior

```java
Session session = cluster.createSession(Behavior.DEFAULT);
```

The default behavior provides:
- 30-second total timeout
- No automatic retries on writes
- Sequential consistency for reads
- Reasonable defaults for most use cases

## Configuring Behavior

### Using Java API

Create custom behavior by deriving from an existing one:

```java
Behavior customBehavior = Behavior.DEFAULT.deriveWithChanges("custom", builder ->
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(10))
        .maximumNumberOfCallAttempts(3)
        .delayBetweenRetries(Duration.ofMillis(100))
    .done()
);

Session session = cluster.createSession(customBehavior);
```

### Configuration Hierarchy

Configure globally for all operations, then override per operation type:

```java
Behavior behavior = Behavior.DEFAULT.deriveWithChanges("tiered", builder ->
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(30))
    .done()
    .onRetryableWrites()
        .maximumNumberOfCallAttempts(3)
        .delayBetweenRetries(Duration.ofMillis(50))
    .done()
    .onQuery()
        .recordQueueSize(10000)
        .maxConcurrentServers(4)
    .done()
);
```

### Common Configuration Options

#### Timeouts

```java
builder.forAllOperations()
    .abandonCallAfter(Duration.ofSeconds(10))           // Total timeout
    .waitForCallToComplete(Duration.ofSeconds(5))       // Socket timeout
    .waitForConnectionToComplete(Duration.ofMillis(100))// Connection timeout
.done()
```

#### Retries

```java
builder.onRetryableWrites()
    .maximumNumberOfCallAttempts(3)
    .delayBetweenRetries(Duration.ofMillis(50))
.done()
```

#### Query Settings

```java
builder.onQuery()
    .recordQueueSize(5000)        // Queue size for results
    .maxConcurrentServers(8)      // Parallel server queries
.done()
```

#### Batch Settings

```java
builder.onBatchReads()
    .allowInlineMemoryAccess(true)
    .allowInlineSsdAccess(true)
    .maxConcurrentServers(16)
.done()
```

## YAML Configuration

For easier management, use YAML files:

### behavior-config.yml

```yaml
behaviors:
  - name: "high-performance"
    parent: "default"
    allOperations:
      abandonCallAfter: "10s"
      maximumNumberOfCallAttempts: 2
      delayBetweenRetries: "5ms"
    
    query:
      recordQueueSize: 10000
      maxConcurrentServers: 4

  - name: "high-reliability"
    parent: "default"
    allOperations:
      abandonCallAfter: "60s"
      maximumNumberOfCallAttempts: 5
      delayBetweenRetries: "100ms"
```

### Loading YAML Configuration

```java
// Start monitoring (auto-reloads on file change)
Behavior.startMonitoring("path/to/behavior-config.yml", 2000);

// Get behavior by name
Behavior highPerf = Behavior.getBehavior("high-performance");
Session session = cluster.createSession(highPerf);

// Stop monitoring when done
Behavior.shutdownMonitor();
```

### Dynamic Reloading

The YAML monitor automatically reloads configuration:

```java
// Start monitoring with 2-second check interval
Behavior.startMonitoring("config/behaviors.yml", 2000);

// Get behavior (always gets latest from file)
Behavior behavior = Behavior.getBehavior("high-performance");

// Update behaviors.yml file...
// Changes automatically picked up within 2 seconds

// Force immediate reload
Behavior.reloadBehaviors();
```

## Behavior Inheritance

Behaviors can inherit from other behaviors:

```yaml
behaviors:
  - name: "base"
    parent: "default"
    allOperations:
      abandonCallAfter: "30s"
  
  - name: "fast"
    parent: "base"
    allOperations:
      abandonCallAfter: "5s"      # Overrides base
      maximumNumberOfCallAttempts: 1

  - name: "batch-fast"
    parent: "fast"
    batchReads:
      maxConcurrentServers: 16    # Adds to fast config
```

## Operation Types

Different operation types can have different configurations:

### Available Operation Types

- `forAllOperations()` - Applies to all operations
- `onConsistencyModeReads()` - Strongly consistent reads
- `onAvailabilityModeReads()` - Eventually consistent reads
- `onRetryableWrites()` - Writes that can be retried
- `onNonRetryableWrites()` - Writes that cannot be retried
- `onBatchReads()` - Batch read operations
- `onBatchWrites()` - Batch write operations
- `onQuery()` - Query/scan operations
- `onInfo()` - Info command operations

### Example: Different Configs Per Type

```java
Behavior behavior = Behavior.DEFAULT.deriveWithChanges("mixed", builder ->
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(30))
    .done()
    .onAvailabilityModeReads()
        .abandonCallAfter(Duration.ofSeconds(5))  // Fast reads
    .done()
    .onRetryableWrites()
        .maximumNumberOfCallAttempts(3)            // Retry writes
    .done()
);
```

## Common Patterns

### Pattern 1: Environment-Specific Behavior

```java
public class BehaviorFactory {
    public static Behavior forEnvironment(String env) {
        return switch (env) {
            case "dev" -> Behavior.DEFAULT;
            case "staging" -> Behavior.getBehavior("staging-config");
            case "prod" -> Behavior.getBehavior("production-config");
            default -> throw new IllegalArgumentException("Unknown env: " + env);
        };
    }
}

// Usage
String env = System.getenv("ENVIRONMENT");
Behavior behavior = BehaviorFactory.forEnvironment(env);
Session session = cluster.createSession(behavior);
```

### Pattern 2: Per-Feature Behavior

```java
public class SessionProvider {
    private final Cluster cluster;
    
    public Session getSearchSession() {
        return cluster.createSession(Behavior.getBehavior("query-optimized"));
    }
    
    public Session getAnalyticsSession() {
        return cluster.createSession(Behavior.getBehavior("batch-optimized"));
    }
    
    public Session getTransactionalSession() {
        return cluster.createSession(Behavior.getBehavior("strong-consistency"));
    }
}
```

### Pattern 3: Shared Configuration

```java
// config/behaviors.yml shared across services
Behavior.startMonitoring("config/behaviors.yml", 5000);

// All services use same configuration
Behavior behavior = Behavior.getBehavior("service-default");
```

## When to Use Different Approaches

### ✅ Use Multiple Sessions When:

- **Different SLAs per feature** - Search needs 5s timeout, analytics needs 60s
- **Different consistency requirements** - Mix of strong and eventual consistency
- **Separate resource limits** - Batch operations need different queue sizes
- **Feature isolation** - Failure in one feature shouldn't affect others

**Example**:
```java
Session searchSession = cluster.createSession(Behavior.FAST_READS);
Session writeSession = cluster.createSession(Behavior.DURABLE_WRITES);
Session batchSession = cluster.createSession(Behavior.BATCH_OPTIMIZED);
```

### ✅ Use YAML Configuration When:

- **Multiple environments** - dev, staging, prod configs
- **Runtime configuration changes** - No code deployment for config tweaks
- **Shared configuration** - Multiple services use same config file
- **Non-developers modify config** - Operations team manages timeouts

**Example**: DevOps can adjust timeouts in production without code changes.

### ✅ Use Java Configuration When:

- **Simple applications** - Single environment, basic config
- **Dynamic behavior** - Config based on runtime conditions
- **Type safety important** - Compile-time validation of settings
- **No external dependencies** - Everything self-contained

**Example**: Small microservice with hardcoded sensible defaults.

### ❌ Avoid Multiple Sessions When:

- **All operations have same requirements** - Use single session with DEFAULT behavior
- **Added complexity not justified** - Simple CRUD app doesn't need multiple sessions
- **Resource constrained** - Each session has overhead (minimal, but exists)

**Use single session instead**:
```java
Session session = cluster.createSession(Behavior.DEFAULT);
// Use for all operations
```

### ❌ Avoid YAML Configuration When:

- **Configuration rarely changes** - Static config in code is simpler
- **No separate environments** - Local dev only
- **Security concerns** - Sensitive config shouldn't be in plain text files
- **Deployment complexity** - Additional file to manage/deploy

### ❌ Avoid Custom Behaviors When:

- **Default behavior sufficient** - Most applications work fine with defaults
- **Premature optimization** - Profile first, optimize later
- **Unclear requirements** - Start with defaults, customize based on actual needs

**Start simple**:
```java
// This is often enough
Session session = cluster.createSession(Behavior.DEFAULT);
```

## Thread Safety

### Session Thread Safety

Sessions are generally thread-safe for reads:

```java
Session session = cluster.createSession(Behavior.DEFAULT);

// Safe: Multiple threads reading
CompletableFuture.allOf(
    CompletableFuture.runAsync(() -> session.query(users).execute()),
    CompletableFuture.runAsync(() -> session.query(products).execute())
).join();
```

**However**, operation builders are NOT thread-safe:

```java
// ❌ NOT Safe: Sharing builder across threads
OperationBuilder builder = session.upsert(key);
// Don't share builder between threads

// ✅ Safe: Each thread creates its own builder
CompletableFuture.runAsync(() -> 
    session.upsert(key1).bin("x").setTo(1).execute()
);
CompletableFuture.runAsync(() -> 
    session.upsert(key2).bin("x").setTo(2).execute()
);
```

## Complete Examples

### Development Configuration

```java
public class DevSession {
    public static Session create(Cluster cluster) {
        Behavior dev = Behavior.DEFAULT.deriveWithChanges("dev", builder ->
            builder.forAllOperations()
                .abandonCallAfter(Duration.ofSeconds(5))
                .maximumNumberOfCallAttempts(1)
            .done()
        );
        return cluster.createSession(dev);
    }
}
```

### Production Configuration

```yaml
# config/production-behaviors.yml
behaviors:
  - name: "production"
    parent: "default"
    allOperations:
      abandonCallAfter: "30s"
      delayBetweenRetries: "100ms"
    
    retryableWrites:
      maximumNumberOfCallAttempts: 3
      useDurableDelete: true
    
    query:
      recordQueueSize: 5000
      maxConcurrentServers: 8
    
    batchReads:
      maxConcurrentServers: 16
      allowInlineMemoryAccess: true
```

```java
public class ProductionSession {
    static {
        Behavior.startMonitoring("config/production-behaviors.yml", 5000);
    }
    
    public static Session create(Cluster cluster) {
        return cluster.createSession(Behavior.getBehavior("production"));
    }
}
```

## Best Practices

### ✅ DO

- Use YAML configuration for production
- Create behavior once, reuse for many sessions
- Use meaningful behavior names
- Document custom behaviors
- Test timeout values under load

### ❌ DON'T

- Don't create new behavior for every operation
- Don't use very short timeouts in production
- Don't forget to stop behavior monitoring
- Don't share operation builders across threads

## API Reference

For complete method documentation:
- [Session API](../api/connection/session.md)
- [Behavior API](../api/configuration/behavior.md)
- [BehaviorBuilder API](../api/configuration/behavior-builder.md)

## Next Steps

- **[DataSets & Keys](./datasets-and-keys.md)** - Learn about data organization
- **[Configuration Guide](../guides/configuration/behavior-java.md)** - Advanced configuration
- **[YAML Configuration](../guides/configuration/yaml-configuration.md)** - YAML details

---

**Questions?** Check the [FAQ](../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
