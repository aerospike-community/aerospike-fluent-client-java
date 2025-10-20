# Dynamic Reloading of Configuration

Learn how to configure the Fluent Client to automatically reload behavior configurations from a YAML file at runtime without restarting your application.

## Goal

By the end of this guide, you'll know how to:
- Enable and start the file monitoring service
- Understand how changes are detected and reloaded
- Manually trigger a configuration reload
- Gracefully shut down the monitoring service

## Prerequisites

- Understanding of [Sessions & Behavior](../../concepts/sessions-and-behavior.md)
- Familiarity with [YAML Configuration](./yaml-configuration.md)

---

## What is Dynamic Reloading?

Dynamic reloading is a powerful feature that allows your application to pick up changes to `behavior` configurations without requiring a restart. This is ideal for production environments where you may need to adjust timeouts, retry policies, or other operational parameters in response to changing conditions.

The Fluent Client provides a built-in file monitoring service that watches your YAML configuration file for modifications and automatically reloads the behaviors into the application.

---

## How It Works

1.  **Start Monitoring**: You start the monitoring service by pointing it to your `behavior-config.yml` file.
2.  **Initial Load**: The service performs an initial load of all behaviors defined in the file.
3.  **File Watcher**: A background thread uses Java's `WatchService` to listen for `ENTRY_MODIFY` events on the file.
4.  **Change Detection**: When the file is saved, the monitor detects the change.
5.  **Reload**: The monitor re-parses the entire YAML file and updates the internal `BehaviorRegistry`. Any new behaviors are added, and existing ones are updated with the new settings.
6.  **Automatic Updates**: Any part of your application that subsequently calls `Behavior.getBehavior("my-behavior")` will receive the newly reloaded configuration.

---

## Step-by-Step Implementation

### 1. Enable Monitoring

To enable dynamic reloading, call `Behavior.startMonitoring()` at the beginning of your application's lifecycle (e.g., in your `main` method or a Spring `@PostConstruct` block).

```java
import com.aerospike.policy.Behavior;

public class App {
    public static void main(String[] args) {
        try {
            // Start monitoring the YAML file for changes.
            // This will check for modifications every 5 seconds (5000 ms).
            Behavior.startMonitoring("config/behavior-config.yml", 5000);
            
            System.out.println("Behavior file monitoring started.");

            // ... your application logic ...

        } catch (Exception e) {
            System.err.println("Failed to start behavior monitoring: " + e.getMessage());
        } finally {
            // Ensure the monitor is shut down gracefully
            Behavior.shutdownMonitor();
            System.out.println("Behavior file monitoring stopped.");
        }
    }
}
```

**Parameters for `startMonitoring()`:**
-   `yamlFilePath` (String): The path to your YAML configuration file.
-   `reloadDelayMs` (long, optional): The interval in milliseconds at which to check for file changes. If not specified, it defaults to a reasonable value.

### 2. Retrieve Behaviors

In your application code, retrieve behaviors by name using `Behavior.getBehavior()`. This method always returns the most up-to-date version of the behavior from the registry.

```java
// This call will always get the latest version of the "high-performance" behavior
Behavior highPerfBehavior = Behavior.getBehavior("high-performance");

// Create a session with the dynamically loaded behavior
Session session = cluster.createSession(highPerfBehavior);

// If you update "high-performance" in the YAML file and save it,
// the next time you call Behavior.getBehavior(), you'll get the new settings.
```

### 3. Modifying the YAML File

Now, you can modify your `behavior-config.yml` at any time while the application is running.

**Original `behavior-config.yml`:**
```yaml
behaviors:
  - name: "dynamic-behavior"
    parent: "default"
    allOperations:
      abandonCallAfter: "10s"
```

**After modification (saved to disk):**
```yaml
behaviors:
  - name: "dynamic-behavior"
    parent: "default"
    allOperations:
      abandonCallAfter: "30s" # <-- Changed from 10s to 30s
      maximumNumberOfCallAttempts: 3 # <-- Added a new policy
```

Within the specified polling interval, the `BehaviorFileMonitor` will detect the change and reload the `dynamic-behavior`, which will now have a 30-second timeout and 3 retry attempts.

### 4. Manually Triggering a Reload

If you need to force an immediate reload of the configuration file, you can call `Behavior.reloadBehaviors()`.

```java
// Some external event triggers a need for immediate reload
System.out.println("Forcing behavior reload...");
Behavior.reloadBehaviors();
System.out.println("Behaviors reloaded.");
```

### 5. Shutting Down the Monitor

It's important to shut down the monitoring service gracefully when your application terminates to release file handles and stop the background thread.

Call `Behavior.shutdownMonitor()`. This is often done in a `finally` block or a shutdown hook.

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    System.out.println("Shutdown hook triggered. Stopping behavior monitor.");
    Behavior.shutdownMonitor();
}));
```

---

## Best Practices

- **Start Monitoring Early**: Initialize the file monitor at application startup to ensure behaviors are available from the beginning.
- **Use a Centralized Configuration File**: Manage all your behaviors in a single, version-controlled YAML file.
- **Graceful Shutdown**: Always ensure `Behavior.shutdownMonitor()` is called on application exit.
- **Immutable Behaviors**: Remember that `Behavior` objects are immutable. Reloading creates *new* internal policy objects. Any `Session` already created with an old `Behavior` will continue to use that old configuration. To use the new configuration, you must create a new `Session`.
- **Error Handling**: Wrap the `startMonitoring` call in a `try-catch` block to handle cases where the file might be missing or malformed at startup.

---

## Next Steps

- **[YAML Configuration](./yaml-configuration.md)**: Review the full syntax for defining behaviors in YAML.
- **[Sessions & Behavior](../../concepts/sessions-and-behavior.md)**: Revisit how sessions and behaviors are connected.
