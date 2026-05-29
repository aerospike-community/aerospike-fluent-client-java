# Overview

## What is the Aerospike Fluent Client?

The Aerospike Fluent Client for Java is a **modern, type-safe wrapper** around the traditional Aerospike Java client. It provides a fluent, chainable API that makes your code more readable, maintainable, and less error-prone.

> **Developer Preview**: Version 0.1.0 is a developer preview. Production readiness is planned for future releases.

## The Problem It Solves

### Traditional Aerospike Client Code

```java
// Traditional approach - verbose and error-prone
WritePolicy writePolicy = new WritePolicy();
writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
writePolicy.expiration = 3600;

Key key = new Key("test", "users", "user123");
Bin nameBin = new Bin("name", "Alice");
Bin ageBin = new Bin("age", 30);
Bin emailBin = new Bin("email", "alice@example.com");

client.put(writePolicy, key, nameBin, ageBin, emailBin);

// Query with filter - complex expression building
Statement stmt = new Statement();
stmt.setNamespace("test");
stmt.setSetName("users");
stmt.setFilter(Filter.equal("age", 30));

RecordSet rs = client.query(null, stmt);
while (rs.next()) {
    Record record = rs.getRecord();
    // Process record...
}
rs.close();
```

### Fluent Client Code

```java
// Fluent approach - clean, readable, type-safe
DataSet users = DataSet.of("test", "users");

session.upsert(users.id("user123"))
    .bin("name").setTo("Alice")
    .bin("age").setTo(30)
    .bin("email").setTo("alice@example.com")
    .expireRecordAfter(Duration.ofHours(1))
    .execute();

// Query with DSL - intuitive and type-safe
RecordStream results = session.query(users)
    .where("$.age == 30")
    .execute();

results.forEach(record -> {
    // Process record...
});
```

## Key Features

### ✨ Fluent, Chainable API

```java
session.upsert(key)
    .bin("counter").add(1)
    .bin("lastUpdate").setTo(LocalDateTime.now())
    .expireRecordAfter(Duration.ofDays(7))
    .execute();
```

**Benefits:**
- More readable than nested method calls
- IDE autocomplete guides you through options
- Clear method chains show intent

### 🛡️ Type Safety

```java
// Compile-time type checking
TypedDataSet<Customer> customers = 
    TypedDataSet.of("test", "customers", Customer.class);

// Type-safe operations
session.upsert(customers)
    .object(new Customer("Alice", 30))
    .execute();

// Type-safe results
List<Customer> results = session.query(customers)
    .execute()
    .toObjectList(customerMapper);
```

**Benefits:**
- Catch errors at compile time, not runtime
- Refactoring is safer
- Better IDE support

### 📝 Built-in DSL for Queries

```java
// String-based DSL
session.query(users)
    .where("$.age > 21 and $.country == 'US'")
    .execute();

// Type-safe DSL (coming soon)
import static com.aerospike.dsl.Dsl.*;

session.query(users)
    .where(and(
        $.bin("age").gt(21),
        $.bin("country").eq("US")
    ))
    .execute();
```

**Benefits:**
- Intuitive query syntax
- No manual filter building
- Compile-time validation

### ⚙️ Declarative Configuration

```java
// Java configuration
Behavior custom = Behavior.DEFAULT.deriveWithChanges("custom", builder ->
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(30))
        .maximumNumberOfCallAttempts(3)
    .done()
);

Session session = cluster.createSession(custom);
```

```yaml
# YAML configuration
behaviors:
  - name: "high-performance"
    parent: "default"
    allOperations:
      abandonCallAfter: "10s"
      maximumNumberOfCallAttempts: 2
```

**Benefits:**
- Configuration separate from code
- Runtime configuration changes
- Hierarchical behavior inheritance

### 🔄 Object Mapping

```java
// Define mapper once
public class CustomerMapper implements RecordMapper<Customer> {
    public Customer fromMap(Map<String, Object> map, Key key, int gen) {
        return new Customer(
            (Long) map.get("id"),
            (String) map.get("name"),
            (Integer) map.get("age")
        );
    }
    // ... toMap() and id() methods
}

// Use everywhere
TypedDataSet<Customer> customers = 
    TypedDataSet.of("test", "customers", Customer.class);

session.upsert(customers).object(customer).execute();
List<Customer> results = session.query(customers).execute()
    .toObjectList(mapper);
```

**Benefits:**
- Work with POJOs, not bins
- Automatic serialization/deserialization
- Consistent mapping logic

### 🔍 Advanced Query Features

```java
// Pagination
RecordStream results = session.query(users)
    .pageSize(100)
    .execute();

while (results.hasMorePages()) {
    List<Customer> page = results.toObjectList(mapper);
    // Process page...
}

// Sorting
RecordStream sorted = session.query(users)
    .sortReturnedSubsetBy("age", SortDir.SORT_DESC)
    .limit(10)
    .execute();

// Partition targeting for parallelization
RecordStream partition = session.query(users)
    .onPartitionRange(0, 1024)  // First quarter of partitions
    .execute();
```

**Benefits:**
- Built-in pagination support
- Server-side sorting
- Performance optimization tools

### 📊 Info Commands & Monitoring

```java
// High-level info API
Set<String> namespaces = session.info().namespaces();

Optional<NamespaceDetail> details = session.info()
    .namespaceDetails("test");

List<Sindex> indexes = session.info().secondaryIndexes();

// Automatic index monitoring (background thread)
IndexesMonitor monitor = cluster.getIndexesMonitor();
Set<Index> currentIndexes = monitor.getIndexes();
```

**Benefits:**
- Simple info command API
- Automatic index discovery
- Real-time cluster monitoring

### 🔐 Transactional Operations

```java
TransactionalSession txSession = 
    new TransactionalSession(cluster, behavior);

// Automatic retry on transient failures
txSession.doInTransaction(tx -> {
    Record record = tx.get(key);
    int balance = record.getInt("balance");
    
    tx.put(key, new Bin("balance", balance + 100));
});
```

**Benefits:**
- Automatic retry logic
- Clean transaction boundaries
- Built-in error handling

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│         Application Code                        │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│     Fluent Client API Layer                     │
│  - Type-safe operations                         │
│  - Object mapping                               │
│  - DSL queries                                  │
│  - Configuration management                     │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│   Traditional Aerospike Java Client             │
│  - Connection management                        │
│  - Protocol implementation                      │
│  - Network I/O                                  │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│         Aerospike Server                        │
└─────────────────────────────────────────────────┘
```

The Fluent Client wraps the traditional client, providing a higher-level API while maintaining full access to underlying functionality when needed.

### Component Flow Diagram

Here's how the main components work together in a typical application:

```
┌─────────────────────────────────────────────────────────────────┐
│                         Your Application                         │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│  Step 1: ClusterDefinition (Builder)                             │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ ClusterDefinition cluster =                                │  │
│  │   new ClusterDefinition("localhost", 3000)                 │  │
│  │     .withNativeCredentials("user", "pass")                 │  │
│  │     .withTlsConfigOf()...done()                            │  │
│  │     .validateClusterNameIs("prod")                         │  │
│  └───────────────────────────────────────────────────────────┘  │
│  Configures: host, port, auth, TLS, rack awareness             │
└────────────────────────┬────────────────────────────────────────┘
                         │ .connect()
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│  Step 2: Cluster (Connection Manager)                            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ try (Cluster cluster = definition.connect()) {             │  │
│  │     // Cluster manages connection lifecycle                │  │
│  │     // Monitors indexes automatically                      │  │
│  │ }                                                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│  Responsibilities: Connection, index monitoring, factory mgmt   │
└────────────────────────┬────────────────────────────────────────┘
                         │ .createSession(behavior)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│  Step 3: Session (Operation Interface)                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Session session = cluster.createSession(Behavior.DEFAULT); │  │
│  │                                                             │  │
│  │ session.insert(key)...execute();                           │  │
│  │ session.upsert(key)...execute();                           │  │
│  │ session.update(key)...execute();                           │  │
│  │ session.delete(key)...execute();                           │  │
│  │ session.query(dataSet)...execute();                        │  │
│  └───────────────────────────────────────────────────────────┘  │
│  Main API for database operations with specific Behavior        │
└────────────────────────┬────────────────────────────────────────┘
                         │ Uses DataSet for key creation
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│  Step 4: DataSet (Key Factory)                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ DataSet users = DataSet.of("test", "users");              │  │
│  │                                                             │  │
│  │ Key key1 = users.id("alice");      // String key          │  │
│  │ Key key2 = users.id(12345);        // Integer key         │  │
│  │ List<Key> keys = users.ids("a", "b", "c");               │  │
│  └───────────────────────────────────────────────────────────┘  │
│  Represents namespace + set, creates type-safe keys             │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
                  Aerospike Server
                  (namespace.set.key)
```

**Lifecycle Example:**

```java
// 1. Define connection
ClusterDefinition definition = new ClusterDefinition("localhost", 3000);

// 2. Connect (with auto-close)
try (Cluster cluster = definition.connect()) {
    
    // 3. Create session with behavior
    Session session = cluster.createSession(Behavior.DEFAULT);
    
    // 4. Define dataset
    DataSet users = DataSet.of("test", "users");
    
    // 5. Perform operations
    session.upsert(users.id("alice"))
        .bin("name").setTo("Alice")
        .execute();
        
} // Cluster automatically closed here
```

**Key Relationships:**
- **1 ClusterDefinition** → **1 Cluster** (via `.connect()`)
- **1 Cluster** → **N Sessions** (each with different Behavior)
- **1 Session** → **N DataSets** (for different namespace+set combinations)
- **1 DataSet** → **N Keys** (factory pattern)

## When to Use the Fluent Client

### ✅ Use the Fluent Client When:

- **Building new applications** - Start with modern, clean APIs
- **Readability matters** - Code will be maintained by teams
- **Type safety is important** - Catch errors at compile time
- **Working with objects** - Need POJO mapping
- **Complex queries** - Benefit from DSL
- **Configuration flexibility** - Need runtime config changes

### ⚠️ Consider Traditional Client When:

- **Maximum performance** - Need absolute minimal overhead (though difference is negligible)
- **Using advanced features** - Some advanced client features not yet wrapped
- **Production-critical systems** - Fluent client is still in developer preview (0.1.0)
- **Existing large codebase** - Migration cost may not justify benefits

## Comparison with Traditional Client

| Feature | Traditional Client | Fluent Client |
|---------|-------------------|---------------|
| **API Style** | Imperative | Fluent/Chainable |
| **Type Safety** | Minimal | Strong |
| **Query Building** | Manual filters | DSL |
| **Configuration** | Code only | Code + YAML |
| **Object Mapping** | Manual | Built-in |
| **Learning Curve** | Steeper | Gentler |
| **Boilerplate** | More | Less |
| **Production Ready** | Yes | Developer Preview |
| **Performance** | Baseline | ~Same (thin wrapper) |

## Version Status

**Current Version**: 0.1.0 (Developer Preview)

### What "Developer Preview" Means:

- ✅ **Stable APIs**: Core APIs unlikely to change significantly
- ✅ **Feedback Welcome**: We want your input on the design
- ⚠️ **Not Production Ready**: Some features still maturing
- ⚠️ **Breaking Changes Possible**: APIs may evolve based on feedback
- 📅 **Production Release**: Planned for future versions

### Compatibility

- **Java**: Requires Java 21 or higher
- **Aerospike Server**: Compatible with Aerospike Server 5.0+
- **Traditional Client**: Built on Aerospike Java Client 9.0.5

See the [Compatibility Matrix](../resources/compatibility-matrix.md) for detailed version information.

## What's Next?

Now that you understand what the Fluent Client is, let's get it running:

→ **[Quick Start Guide](./quickstart.md)** - Get your first program working in 10 minutes

Or dive deeper:

→ **[Core Concepts](../concepts/README.md)** - Understand the architecture
→ **[API Reference](../api/README.md)** - Detailed documentation
→ **[Migration Guide](../guides/migration/migrating-from-traditional.md)** - Moving from traditional client

---

**Have questions?** Check the [FAQ](../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
