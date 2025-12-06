# API Comparison: Traditional vs. Fluent Client

This guide provides a side-by-side comparison of the traditional Aerospike Java client and the new Fluent Client, helping you understand the differences and benefits of the new API.

## Goal

By the end of this guide, you'll be able to:
- See the direct equivalent of common traditional client operations in the Fluent Client
- Understand the design philosophy and improvements of the fluent API
- Recognize the patterns for writing cleaner, more readable, and type-safe Aerospike code

## Prerequisites

- Basic familiarity with the traditional Aerospike Java client is helpful but not required.
- Understanding of the [Fluent Client's Core Concepts](../../concepts/README.md).

---

## Core Philosophy: From Verbose to Fluent

The traditional client is powerful and flexible, but it can be verbose. It often requires you to manually create `Key`, `Bin`, and `Policy` objects, and the code can sometimes be difficult to read.

The Fluent Client is designed to be:
- **More Readable**: The fluent, chainable API reads like a sentence.
- **Less Boilerplate**: It reduces the amount of code needed for common operations.
- **Type-Safe**: It leverages generics to provide compile-time safety, especially when working with objects.
- **Discoverable**: The fluent methods make it easier to discover available options in your IDE.

Let's see how this plays out in common scenarios.

---

## Side-by-Side Comparison

### 1. Connecting to the Cluster

**Traditional:**
```java
import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.ClientPolicy;

ClientPolicy policy = new ClientPolicy();
policy.user = "user";
policy.password = "pass";

IAerospikeClient client = new AerospikeClient(policy, "localhost", 3000);
```

**Fluent:**
```java
import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;

try (Cluster cluster = new ClusterDefinition("localhost", 3000)
                        .withNativeCredentials("user", "pass")
                        .connect()) {
    // ... use the cluster ...
}
```
**Key Differences:**
- The Fluent Client uses a `ClusterDefinition` builder for a cleaner, more readable configuration.
- It introduces a `Cluster` object that manages the connection and implements `Closeable` for easy use with `try-with-resources`.

### 2. Writing a Record

**Traditional:**
```java
import com.aerospike.client.Key;
import com.aerospike.client.Bin;
import com.aerospike.client.policy.WritePolicy;

Key key = new Key("test", "users", "user123");

Bin bin1 = new Bin("name", "Alice");
Bin bin2 = new Bin("age", 30);
Bin bin3 = new Bin("email", "alice@example.com");

WritePolicy writePolicy = new WritePolicy();
writePolicy.expiration = 3600; // 1 hour

client.put(writePolicy, key, bin1, bin2, bin3);
```

**Fluent:**
```java
import com.aerospike.DataSet;
import com.aerospike.Session;
import java.time.Duration;

DataSet users = DataSet.of("test", "users");
Session session = cluster.createSession(Behavior.DEFAULT);

session.upsert(users.id("user123"))
    .bin("name").setTo("Alice")
    .bin("age").setTo(30)
    .bin("email").setTo("alice@example.com")
    .expireRecordAfter(Duration.ofHours(1))
    .execute();
```
**Key Differences:**
- The Fluent Client uses a `DataSet` to represent the namespace/set combination, reducing redundancy.
- The `upsert()` method starts a fluent chain that is highly readable.
- `Bin` objects are created implicitly via the `.bin("name").setTo(...)` syntax.
- Policies are configured directly on the operation chain (e.g., `expireRecordAfter()`).

### 3. Reading a Record

**Traditional:**
```java
import com.aerospike.client.Record;

Key key = new Key("test", "users", "user123");
Record record = client.get(null, key);

if (record != null) {
    String name = record.getString("name");
    int age = record.getInt("age");
}
```

**Fluent:**
```java
import com.aerospike.RecordStream;
import com.aerospike.RecordResult;

RecordStream results = session.query(users.id("user123")).execute();

if (results.hasNext()) {
    RecordResult record = results.next();
    String name = record.recordOrThrow().getString("name");
    int age = record.recordOrThrow().getInt("age");
}

// Or, using Optional for a more modern Java style:
Optional<RecordResult> resultOpt = session.query(users.id("user123")).execute().getFirst();
resultOpt.ifPresent(keyRecord -> {
    String name = record.recordOrThrow().getString("name");
    // ...
});
```
**Key Differences:**
- All read operations in the Fluent Client, even for a single key, return a `RecordStream`, providing a consistent result type.
- The Fluent Client encourages a more functional style with methods like `getFirst()` returning an `Optional`.

### 4. Reading Specific Bins

**Traditional:**
```java
Record record = client.get(null, key, "name", "email");
```

**Fluent:**
```java
RecordStream results = session.query(users.id("user123"))
    .readingOnlyBins("name", "email")
    .execute();
```
**Key Differences:**
- The Fluent Client's `.readingOnlyBins()` method is more descriptive and fits naturally into the query chain.

### 5. Querying with a Secondary Index

**Traditional:**
```java
import com.aerospike.client.query.Statement;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.RecordSet;

Statement stmt = new Statement();
stmt.setNamespace("test");
stmt.setSetName("users");
stmt.setFilter(Filter.equal("age", 30));

try (RecordSet rs = client.query(null, stmt)) {
    while (rs.next()) {
        Record record = rs.getRecord();
        // process record...
    }
}
```

**Fluent:**
```java
RecordStream results = session.query(users)
    .where("$.age == 30")
    .execute();

results.forEach(keyRecord -> {
    // process record...
});
```
**Key Differences:**
- The Fluent Client dramatically simplifies queries. `Statement` and `Filter` objects are no longer needed.
- A simple `where()` clause with a DSL string is used for filtering.
- The `RecordStream` is an `Iterator` and also supports `forEach`, making it easy to integrate with Java Streams and modern iteration patterns.

### 6. Working with Objects (POJOs)

**Traditional:**
*(Requires manual mapping or a third-party library)*
```java
// Manual mapping
User user = new User("user123", "Alice", 30);
Bin[] bins = new Bin[] {
    new Bin("name", user.getName()),
    new Bin("age", user.getAge())
};
client.put(null, new Key("test", "users", user.getId()), bins);

// Manual de-mapping
Record record = client.get(null, new Key("test", "users", "user123"));
User fetchedUser = new User(
    "user123",
    record.getString("name"),
    record.getInt("age")
);
```

**Fluent:**
*(Built-in, type-safe object mapping)*
```java
import com.aerospike.TypeSafeDataSet;

// Assumes a UserMapper is registered
TypeSafeDataSet<User> typedUsers = TypeSafeDataSet.of("test", "users", User.class);

// Writing an object
User user = new User("user123", "Alice", 30);
session.upsert(typedUsers).object(user).execute();

// Reading an object
Optional<User> fetchedUser = session.query(typedUsers.id("user123"))
    .execute()
    .getFirst(userMapper); // or getFirst() if using a registered mapper
```
**Key Differences:**
- The Fluent Client has a first-class, built-in object mapping system.
- `TypeSafeDataSet` provides compile-time safety.
- The `.object()` method makes writing POJOs a single, clean operation.
- Reading objects back is equally simple, with automatic deserialization.

---

## Next Steps

- **[Migrating from Traditional Client](./migrating-from-traditional.md)**: A step-by-step guide to help you convert your existing codebase to the new Fluent Client.
- **[Quick Start](../../getting-started/quickstart.md)**: Jump in and write your first Fluent Client application.
