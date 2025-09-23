# Hierarchical YAML Configuration for Behavior Classes

This document explains how to configure Aerospike Behavior classes using hierarchical YAML files with dynamic reloading capabilities.

## Overview

The YAML configuration system allows you to define multiple Behavior configurations in a single YAML file, organized in a tree structure with inheritance. The system supports dynamic reloading when the YAML file changes, making it easy to modify behavior configurations without restarting the application.

## YAML Structure

The YAML configuration supports multiple behaviors in a hierarchical structure:

```yaml
behaviors:
  - name: "high-performance"
    parent: "default"
    exceptionPolicy: "RETURN_AS_MANY_RESULTS_AS_POSSIBLE"
    sendKey: true
    useCompression: true
    
    allOperations:
      abandonCallAfter: "10s"
      delayBetweenRetries: "5ms"
      maximumNumberOfCallAttempts: 2
      replicaOrder: ["MASTER", "ANY_REPLICA"]
      resetTtlOnReadAtPercent: 0
      sendKey: true
      useCompression: true
      waitForCallToComplete: "500ms"
      waitForConnectionToComplete: "100ms"
      waitForSocketResponseAfterCallFails: "50ms"
    
    retryableWrites:
      useDurableDelete: false
      maximumNumberOfCallAttempts: 2
      delayBetweenRetries: "5ms"
    
    query:
      recordQueueSize: 10000
      maxConcurrentServers: 4
      maximumNumberOfCallAttempts: 3

  - name: "batch-optimized"
    parent: "high-performance"
    
    batchReads:
      maxConcurrentServers: 8
      allowInlineMemoryAccess: true
      allowInlineSsdAccess: true
    
    batchWrites:
      maxConcurrentServers: 8
      allowInlineMemoryAccess: true
      allowInlineSsdAccess: true

  - name: "development"
    parent: "default"
    exceptionPolicy: "RETURN_AS_MANY_RESULTS_AS_POSSIBLE"
    sendKey: false
    useCompression: false
    
    allOperations:
      abandonCallAfter: "5s"
      delayBetweenRetries: "10ms"
      maximumNumberOfCallAttempts: 1
      replicaOrder: ["MASTER"]
      resetTtlOnReadAtPercent: 0
      sendKey: false
      useCompression: false
      waitForCallToComplete: "1s"
      waitForConnectionToComplete: "500ms"
      waitForSocketResponseAfterCallFails: "100ms"
```

## Configuration Sections

### Top-Level Structure
- `behaviors`: List of behavior configurations

### Individual Behavior Configuration
Each behavior in the list has these properties:
- `name`: The name of the behavior (required)
- `parent`: The name of the parent behavior (optional, defaults to "default")
- `exceptionPolicy`: Either "THROW_ON_ANY_ERROR" or "RETURN_AS_MANY_RESULTS_AS_POSSIBLE"
- `sendKey`: Global sendKey setting
- `useCompression`: Global compression setting

### allOperations
Contains settings that apply to all operation types. These settings are inherited by all specific operation types unless overridden.

### Operation-Specific Sections
Each section corresponds to a specific operation type and can override settings from `allOperations`:

- `consistencyModeReads`: Settings for consistency mode reads
- `availabilityModeReads`: Settings for availability mode reads  
- `retryableWrites`: Settings for retryable write operations
- `nonRetryableWrites`: Settings for non-retryable write operations
- `batchReads`: Settings for batch read operations
- `batchWrites`: Settings for batch write operations
- `query`: Settings for query operations
- `info`: Settings for info operations

## Duration Format

Duration values can be specified using ISO-8601 format:
- `"30s"` - 30 seconds
- `"1m"` - 1 minute
- `"100ms"` - 100 milliseconds
- `"2h"` - 2 hours

## NodeCategory Values

For `replicaOrder`, use these values:
- `MASTER`
- `MASTER_OR_REPLICA`
- `MASTER_OR_REPLICA_IN_RACK`
- `ANY_REPLICA`
- `REPLICA_IN_RACK`
- `RANDOM`
- `RANDOM_IN_RACK`

## Usage

### Dynamic File Monitoring (Recommended)

```java
import com.aerospike.policy.Behavior;

// Start monitoring a YAML file for changes
Behavior.startMonitoring("path/to/behaviors.yml", 2000); // 2 second reload delay

// Get behaviors by name
Behavior highPerf = Behavior.getBehavior("high-performance");
Behavior batchOpt = Behavior.getBehavior("batch-optimized");

// Use the behaviors
var writePolicy = highPerf.getSharedPolicy(Behavior.CommandType.WRITE_RETRYABLE);
var queryPolicy = batchOpt.getSharedPolicy(Behavior.CommandType.QUERY);

// Check if monitoring is active
if (Behavior.isMonitoring()) {
    System.out.println("File monitoring is active");
}

// Manually reload behaviors
Behavior.reloadBehaviors();

// Stop monitoring when done
Behavior.shutdownMonitor();
```

### Tree Traversal

```java
// Find behaviors in the tree
Behavior found = Behavior.DEFAULT.findBehavior("high-performance");
Behavior child = Behavior.DEFAULT.findBehavior("batch-optimized");

// Get children of a behavior
List<Behavior> children = Behavior.DEFAULT.getChildren();

// Check parent-child relationships
Behavior parent = child.getParent();
System.out.println("Parent: " + parent.getName());
```

### Loading from File (One-time)

```java
import com.aerospike.policy.BehaviorYamlLoader;
import java.io.File;

// Load behaviors from YAML file
File yamlFile = new File("path/to/behaviors.yml");
Map<String, Behavior> behaviors = BehaviorYamlLoader.loadBehaviorsFromFile(yamlFile);

// Register behaviors manually
for (Behavior behavior : behaviors.values()) {
    BehaviorRegistry.getInstance().registerBehavior(behavior);
}
```

## Inheritance and Tree Structure

The system supports hierarchical inheritance through a tree structure:

1. **DEFAULT Behavior**: Always at the root of the tree
2. **Parent-Child Relationships**: Each behavior can specify a parent, creating inheritance chains
3. **Setting Inheritance**: 
   - Settings from `allOperations` are applied to all operation types
   - Operation-specific settings override `allOperations` settings
   - Child behaviors inherit from their parent unless overridden
   - Only specified settings are applied; unspecified settings use defaults

### Example Inheritance Chain
```
DEFAULT
├── high-performance
│   ├── batch-optimized
│   └── query-optimized
├── high-reliability
└── development
```

In this example:
- `batch-optimized` inherits from `high-performance`, which inherits from `DEFAULT`
- `query-optimized` inherits from `high-performance`, which inherits from `DEFAULT`
- `high-reliability` inherits directly from `DEFAULT`
- `development` inherits directly from `DEFAULT`

## Example: Equivalent Builder and YAML

### Builder Pattern
```java
Behavior behavior = new BehaviorBuilder()
    .forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(30))
        .delayBetweenRetries(Duration.ofMillis(0))
        .maximumNumberOfCallAttempts(0)
        .replicaOrder(NodeCategory.SEQUENCE)
        .resetTtlOnReadAtPercent(0)
        .sendKey(false)
        .useCompression(false)
        .waitForCallToComplete(Duration.ofSeconds(1))
        .waitForConnectionToComplete(Duration.ofSeconds(0))
        .waitForSocketResponseAfterCallFails(Duration.ofSeconds(0))
    .done()
    .onRetryableWrites()
        .useDurableDelete(false)
        .maximumNumberOfCallAttempts(3)
        .delayBetweenRetries(Duration.ofMillis(25))
    .done()
    .build();
```

### Equivalent YAML
```yaml
name: "equivalent-behavior"
allOperations:
  abandonCallAfter: "30s"
  delayBetweenRetries: "0ms"
  maximumNumberOfCallAttempts: 0
  replicaOrder: ["MASTER", "ANY_REPLICA"]
  resetTtlOnReadAtPercent: 0
  sendKey: false
  useCompression: false
  waitForCallToComplete: "1s"
  waitForConnectionToComplete: "0s"
  waitForSocketResponseAfterCallFails: "0s"

retryableWrites:
  useDurableDelete: false
  maximumNumberOfCallAttempts: 3
  delayBetweenRetries: "25ms"
```

## Benefits

1. **Readability**: YAML is more readable than nested builder calls
2. **Maintainability**: Configuration can be stored in files and version controlled
3. **Runtime Configuration**: Behaviors can be loaded and modified without code changes
4. **Dynamic Reloading**: Changes to YAML files are automatically detected and applied
5. **Hierarchical Organization**: Tree structure allows for logical grouping and inheritance
6. **Name-based Lookup**: Easy to find behaviors by name using `Behavior.getBehavior(name)`
7. **Tree Traversal**: Navigate the behavior tree to find related behaviors
8. **Validation**: YAML structure provides clear validation of configuration
9. **Documentation**: Configuration files serve as self-documenting examples

## Dependencies

The YAML configuration system requires these Jackson dependencies:
- `jackson-databind`
- `jackson-dataformat-yaml`
- `jackson-datatype-jsr310` (for Duration serialization) 