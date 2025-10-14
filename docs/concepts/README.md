# Core Concepts

Understanding these fundamental concepts will help you build better applications with the Aerospike Fluent Client.

## Learning Path

We recommend reading these pages in order:

### 1. [Connection Management](./connection-management.md)
**Time**: 10 minutes

Learn how to connect to Aerospike clusters using `ClusterDefinition` and manage connections.

**Key Topics**:
- Creating connections
- Authentication
- TLS/SSL configuration
- Multi-node clusters
- Rack awareness

### 2. [Sessions & Behavior](./sessions-and-behavior.md)
**Time**: 15 minutes

Understand how to create sessions and configure operational behavior.

**Key Topics**:
- Creating sessions
- Default vs custom behaviors
- Configuring timeouts
- Retry policies
- YAML configuration

### 3. [DataSets & Keys](./datasets-and-keys.md)
**Time**: 10 minutes

Learn how to organize data and create keys for records.

**Key Topics**:
- Namespace and set organization
- DataSet abstraction
- Key generation
- Different key types
- Best practices

### 4. [Type-Safe Operations](./type-safe-operations.md)
**Time**: 15 minutes

Discover the fluent, type-safe API for database operations.

**Key Topics**:
- Fluent builders
- Operation chaining
- Type safety benefits
- Compile-time checking
- Common patterns

### 5. [Object Mapping](./object-mapping.md)
**Time**: 15 minutes

Work with Java objects instead of raw bins.

**Key Topics**:
- RecordMapper interface
- TypeSafeDataSet
- Automatic serialization
- Custom mappers
- Best practices

---

## Conceptual Overview

```
Application
     ↓
ClusterDefinition → Cluster (Connection Pool)
                        ↓
                    Session (+ Behavior Config)
                        ↓
                    DataSet/TypeSafeDataSet
                        ↓
                    OperationBuilder/QueryBuilder
                        ↓
                    Execute → RecordStream
                        ↓
                    Results (KeyRecord or Objects)
```

### The Flow

1. **Define Connection**: Use `ClusterDefinition` to configure connection details
2. **Connect**: Call `.connect()` to get a `Cluster` instance
3. **Create Session**: Get a `Session` from the cluster with a `Behavior`
4. **Define DataSet**: Specify namespace and set
5. **Build Operation**: Use fluent builders for operations
6. **Execute**: Call `.execute()` to send to database
7. **Process Results**: Handle `RecordStream` or mapped objects

---

## Quick Reference

### Common Imports

```java
import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.Session;
import com.aerospike.DataSet;
import com.aerospike.TypeSafeDataSet;
import com.aerospike.RecordStream;
import com.aerospike.KeyRecord;
import com.aerospike.policy.Behavior;
import com.aerospike.RecordMapper;
```

### Minimal Example

```java
try (Cluster cluster = new ClusterDefinition("localhost", 3000).connect()) {
    Session session = cluster.createSession(Behavior.DEFAULT);
    DataSet users = DataSet.of("test", "users");
    
    // Write
    session.upsert(users.id("alice"))
        .bin("name").setTo("Alice")
        .execute();
    
    // Read
    RecordStream result = session.query(users.id("alice")).execute();
}
```

---

## Key Principles

### 1. **Resource Management**

Always use try-with-resources for `Cluster`:

```java
try (Cluster cluster = ...) {
    // Use cluster
} // Automatically closed
```

### 2. **Immutability**

`DataSet`, `Behavior`, and keys are immutable. Builders create new instances:

```java
DataSet users = DataSet.of("test", "users");
// users is immutable, can be safely shared
```

### 3. **Fluent Chaining**

Operations use method chaining for readability:

```java
session.upsert(key)
    .bin("a").setTo(1)
    .bin("b").setTo(2)
    .execute();
```

### 4. **Explicit Execution**

Operations don't execute until you call `.execute()`:

```java
var builder = session.upsert(key).bin("x").setTo(1);
// Nothing sent to database yet

builder.execute();  // Now it executes
```

### 5. **Type Safety**

The API encourages type-safe operations:

```java
TypeSafeDataSet<Customer> customers = 
    TypeSafeDataSet.of("test", "customers", Customer.class);

// Compile-time checking
session.upsert(customers)
    .object(new Customer(...))  // Type-checked
    .execute();
```

---

## Design Philosophy

The Fluent Client is designed with these principles:

### Readability Over Brevity

```java
// Clear intent
session.upsert(users.id("alice"))
    .bin("age").add(1)
    .expireRecordAfter(Duration.ofDays(365))
    .execute();
```

### Fail Fast

Errors are caught at compile time when possible:

```java
// Compile error: can't call execute() twice
builder.execute().execute();  // Won't compile
```

### Discoverability

IDE autocomplete guides you through the API:

```java
session.  // IDE shows: upsert, insert, update, delete, query
    upsert(key).  // IDE shows: bin, expireRecordAfter, execute, etc.
```

### Consistency

Similar operations have similar APIs:

```java
session.upsert(key).bin("x").setTo(1).execute();
session.insert(key).bin("x").setTo(1).execute();
session.update(key).bin("x").setTo(1).execute();
```

---

## Common Patterns

### Pattern 1: Read-Modify-Write

```java
DataSet users = DataSet.of("test", "users");
Key key = users.id("alice");

// Read
RecordStream result = session.query(key).execute();
if (result.hasNext()) {
    KeyRecord record = result.next();
    int currentAge = record.record.getInt("age");
    
    // Modify
    int newAge = currentAge + 1;
    
    // Write
    session.upsert(key)
        .bin("age").setTo(newAge)
        .execute();
}
```

### Pattern 2: Batch Operations

```java
List<Key> keys = users.ids("alice", "bob", "carol");

session.upsert(keys)
    .bin("status").setTo("active")
    .execute();
```

### Pattern 3: Query and Process

```java
RecordStream results = session.query(users)
    .where("$.age > 18")
    .execute();

results.forEach(record -> {
    System.out.println(record.record.getString("name"));
});
```

### Pattern 4: Object Mapping

```java
TypeSafeDataSet<Customer> customers = 
    TypeSafeDataSet.of("test", "customers", Customer.class);

Customer alice = new Customer("Alice", 30);
session.upsert(customers).object(alice).execute();

List<Customer> results = session.query(customers)
    .execute()
    .toObjectList(customerMapper);
```

---

## Next Steps

### Start Learning

Begin with [Connection Management](./connection-management.md) to understand how to connect to Aerospike.

### After Core Concepts

Once you understand these concepts, move on to:

- **[CRUD Operations](../guides/crud/creating-records.md)** - Apply your knowledge
- **[Querying Data](../guides/querying/simple-queries.md)** - Learn to query
- **[API Reference](../api/README.md)** - Detailed method documentation

### Get Hands-On

Try the [Examples & Recipes](../examples/README.md) to see these concepts in action.

---

**Questions?** Check the [FAQ](../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
