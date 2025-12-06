# YAML Configuration

Learn how to define and dynamically reload `Behavior` policies from external YAML files.

## Goal

By the end of this guide, you'll know how to:
- Structure a YAML file for `Behavior` definitions
- Load behaviors from a file at application startup
- Dynamically monitor a file for changes and reload behaviors at runtime
- Understand the benefits of externalizing configuration

## Prerequisites

- [Behavior Configuration (Java)](./behavior-java.md) completed
- Basic understanding of YAML syntax

---

## Why Use YAML for Configuration?

Externalizing your `Behavior` definitions into YAML files provides several advantages over configuring them in Java code:

- **Dynamic Reloading**: Change timeouts, retries, and other policies without recompiling or restarting your application.
- **Separation of Concerns**: Decouples operational policies from application logic.
- **Easier Management**: Operations teams can tune performance by editing a simple text file.
- **Readability**: YAML's hierarchical structure is easy to read and understand.

---

## YAML File Structure

A behavior configuration file has a top-level `behaviors` key, which is a list of behavior definitions.

### Basic Structure

```yaml
behaviors:
  - name: "app-default"
    parent: "default" # Inherits from the client's built-in default
    # ... policies for app-default ...

  - name: "read-only-api"
    parent: "app-default" # Inherits from our custom default
    # ... policies for read-only-api ...
```

### Policy Configuration

Within each behavior definition, you can specify policies for the same categories available in the Java builder.

```yaml
behaviors:
  - name: "app-default"
    parent: "default"
    allOperations:
      abandonCallAfter: "2s"
      
  - name: "read-only-api"
    parent: "app-default"
    reads:
      maximumNumberOfCallAttempts: 4
      replicaOrder: "MASTER_PROLES"
    scans:
      waitForCallToComplete: "5s"

  - name: "critical-write-api"
    parent: "app-default"
    writes:
      maximumNumberOfCallAttempts: 2
      waitForCallToComplete: "200ms"
```

### Duration Formats

Durations can be specified in a human-readable format.

- `10s` (10 seconds)
- `500ms` (500 milliseconds)
- `1m` (1 minute)
- `2h` (2 hours)

See the `DURATION_FORMATS.md` file for a full list of supported units.

---

## Loading Behaviors from YAML

The `Behavior` class provides static methods for loading and monitoring YAML configuration files.

### 1. One-Time Load at Startup

Use `BehaviorYamlLoader.loadBehaviorsFromFile()` to load the file once when your application starts.

```java
import com.aerospike.policy.BehaviorYamlLoader;
import com.aerospike.policy.Behavior;
import java.io.File;

// ... in your application startup
File configFile = new File("config/aerospike-behaviors.yaml");
BehaviorYamlLoader.loadBehaviorsFromFile(configFile);

// Now you can get the loaded behaviors by name
Behavior readOnlyBehavior = Behavior.getBehavior("read-only-api");

Session readSession = cluster.createSession(readOnlyBehavior);
// Use the session...
```
If the file is not found or is invalid, this method will throw an exception.

### 2. Dynamic Monitoring and Reloading

Use `Behavior.startMonitoring()` to have the client watch the file for changes and automatically reload the behaviors when the file is saved. This method also performs the initial load, so you don't need to call `loadBehaviorsFromFile()` separately.

```java
import com.aerospike.policy.Behavior;

String configPath = "config/aerospike-behaviors.yaml";

// Start monitoring the file. This also loads the behaviors immediately.
Behavior.startMonitoring(configPath);

// You can now get behaviors by name. Behavior.getBehavior() will always
// return the most recently loaded version.
Behavior criticalBehavior = Behavior.getBehavior("critical-write-api");

// Later, in your application shutdown hook:
Behavior.stopMonitoring();
```

For use with try-with-resources:

```java
import com.aerospike.policy.Behavior;
import java.io.Closeable;

try (Closeable monitor = Behavior.startMonitoringWithResource("config/aerospike-behaviors.yaml")) {
    Cluster cluster = new ClusterDefinition("localhost", 3000).connect();
    // ... use cluster with dynamically reloadable behaviors
} // Monitor automatically stopped when exiting the block
```

**How it works**:
1. You edit and save the `aerospike-behaviors.yaml` file.
2. The file monitor detects the change.
3. It re-reads the file and replaces the in-memory `Behavior` definitions.
4. Any subsequent call to `Behavior.getBehavior("behavior-name")` will return the new, updated behavior.

> **Important**: `Session` objects are created with a specific `Behavior` instance. They will **not** be updated automatically. To use the new policies, you must create a **new** `Session` after the file has been reloaded.

---

## Complete Example: Dynamic `UserService`

This example demonstrates a service that can dynamically update its operational behavior.

### `aerospike-behaviors.yaml`

```yaml
behaviors:
  - name: "user-service-behavior"
    parent: "default"
    reads:
      abandonCallAfter: "1s"
      maximumNumberOfCallAttempts: 3
    writes:
      abandonCallAfter: "500ms"
      maximumNumberOfCallAttempts: 2
```

### `UserService.java`

```java
import com.aerospike.policy.Behavior;

public class UserService {
    private final Cluster cluster;
    private static final String BEHAVIOR_NAME = "user-service-behavior";
    
    public UserService(Cluster cluster) {
        this.cluster = cluster;
    }
    
    public void performReadOperation() {
        // Get the LATEST behavior every time
        Session session = cluster.createSession(getLatestBehavior());
        // ... perform read ...
    }
    
    public void performWriteOperation() {
        Session session = cluster.createSession(getLatestBehavior());
        // ... perform write ...
    }
    
    private Behavior getLatestBehavior() {
        // Get the most recent version of the behavior from Behavior.getBehavior()
        return Behavior.getBehavior(BEHAVIOR_NAME);
    }
}
```

**Scenario**:
1. The application starts, and `Behavior.startMonitoring()` is called.
2. A `UserService` instance is created.
3. Initially, reads have a 1-second timeout.
4. An operator edits `aerospike-behaviors.yaml` and changes `reads.abandonCallAfter` to `"500ms"`.
5. The file monitor reloads the file.
6. The very next call to `userService.performReadOperation()` will create a new session with the new 500ms timeout. No restart was needed.

---

## Best Practices

### ✅ DO

**Use dynamic monitoring in long-running applications.**
This is one of the most powerful features of the client, allowing you to tune performance on the fly.

**Structure your `Session` management to fetch the latest `Behavior`.**
A simple dependency injection framework or a factory pattern can help with this.

**Use a base behavior in your YAML file.**
Just like with the Java builder, a parent behavior helps maintain consistency.

**Validate your YAML.**
Use a linter or an IDE plugin to ensure your YAML is well-formed before deploying it.

### ❌ DON'T

**Don't cache `Behavior` objects in your services if you are using monitoring.**
Always get the latest version using `Behavior.getBehavior()`.

**Don't put sensitive information in your YAML files.**
These files are for operational policies, not credentials.

---

## Next Steps

You've now completed the core guides for the Fluent Client!

- **[Advanced Topics](../advanced/README.md)** - Explore more advanced features like transactions.
- **[API Reference](../../api/README.md)** - Browse the complete API documentation.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
