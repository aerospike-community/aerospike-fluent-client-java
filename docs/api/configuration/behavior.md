# `Behavior`

An immutable object that defines the policies for Aerospike operations.

`com.aerospike.policy.Behavior`

## Overview

A `Behavior` object encapsulates all the underlying client policies (e.g., `WritePolicy`, `QueryPolicy`) that govern how a database operation is executed. This includes settings for timeouts, retry attempts, consistency levels, and much more.

Behaviors are a central concept in the Fluent Client, allowing you to define reusable sets of policies for different use cases, such as "high-throughput writes" or "latency-sensitive reads."

Key characteristics of `Behavior` objects:
- **Immutability**: Once created, a `Behavior` object is immutable.
- **Inheritance**: Behaviors can be derived from other behaviors, inheriting and overriding settings. This creates a powerful hierarchy.
- **Named and Reusable**: Behaviors can be named and retrieved from a central registry.
- **Dynamic Reloading**: Behaviors can be defined in a YAML file and reloaded at runtime without restarting the application.

Every `Session` is created with a `Behavior`, which applies to all operations performed within that session.

## `Behavior.DEFAULT`

The Fluent Client provides a static `Behavior.DEFAULT` object with sensible default settings suitable for most common use cases. It's a great starting point for creating your own custom behaviors.

**Usage:**
```java
Session session = cluster.createSession(Behavior.DEFAULT);
```

## Creating Custom Behaviors

You create a custom behavior by calling `deriveWithChanges()` on an existing behavior (usually `Behavior.DEFAULT`). This method takes a name for the new behavior and a lambda expression that uses a `BehaviorBuilder` to define the custom settings.

```java
import com.aerospike.policy.Behavior;
import java.time.Duration;

// Create a custom behavior for critical read operations
Behavior criticalRead = Behavior.DEFAULT.deriveWithChanges("critical-read", builder ->
    builder.onConsistencyModeReads()
        .abandonCallAfter(Duration.ofMillis(500)) // 500ms total timeout
        .maximumNumberOfCallAttempts(2) // Only one retry
    .done()
);

// Use the new behavior to create a session
Session session = cluster.createSession(criticalRead);

// All read operations in this session will now use the 500ms timeout
session.query(users.id("alice")).execute();
```

## Methods

### `deriveWithChanges(String newName, BehaviorChanger changer)`

Creates a new child behavior that inherits settings from the current behavior and applies custom overrides.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `newName` | `String` | A unique name for the new behavior. |
| `changer` | `BehaviorChanger` | A lambda expression that receives a `BehaviorBuilder` to define the changes. |

**Returns:** `Behavior` - The new, immutable `Behavior` instance.

---

### `getBehavior(String name)`

Retrieves a named behavior from the central `BehaviorRegistry`.

**Returns:** `Behavior` - The named behavior, or `Behavior.DEFAULT` if not found.

---

### `startMonitoring(String yamlFilePath)`

Starts a background thread to monitor a YAML configuration file for changes to behaviors. When the file is modified, all behaviors are reloaded dynamically.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `yamlFilePath` | `String` | The path to the YAML file. |

---

### `stopMonitoring()`

Stops the background file monitoring thread.

## Dynamic Configuration from YAML

Behaviors can be defined in an external YAML file, allowing you to tune policies without recompiling your code.

**`behavior-config.yaml` example:**
```yaml
behaviors:
  - name: high-performance-write
    parent: default
    policies:
      onRetryableWrites:
        maximumNumberOfCallAttempts: 5
        delayBetweenRetries: 20ms
      forAllOperations:
        abandonCallAfter: 5s
        useCompression: true

  - name: quick-read
    parent: default
    policies:
      onConsistencyModeReads:
        abandonCallAfter: 200ms
```

**Loading the YAML file:**
```java
import com.aerospike.policy.Behavior;
import java.io.IOException;

try {
    Behavior.startMonitoring("path/to/behavior-config.yaml");
} catch (IOException e) {
    System.err.println("Failed to start behavior monitoring.");
}

// Now you can retrieve behaviors defined in the file by name
Behavior fastWriteBehavior = Behavior.getBehavior("high-performance-write");
Session fastWriteSession = cluster.createSession(fastWriteBehavior);
```

## Related Classes

- **[`BehaviorBuilder`](./behavior-builder.md)**: The builder used to define custom behaviors.
- **[`Session`](../connection/session.md)**: Requires a `Behavior` for its creation.

## See Also

- **[Guide: Behavior Configuration (Java)](../../guides/configuration/behavior-java.md)**
- **[Guide: YAML Configuration](../../guides/configuration/yaml-configuration.md)**
