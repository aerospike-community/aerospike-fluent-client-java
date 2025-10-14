# Type-Safe Operations

Learn how the Fluent Client's type-safe, builder-based API makes your code safer and more maintainable.

## Overview

The Fluent Client uses **builder patterns** and **method chaining** to provide:

- **Compile-time safety**: Catch errors before runtime
- **IDE discoverability**: Autocomplete guides you
- **Fluent readability**: Code reads like natural language
- **Impossible states impossible**: Can't create invalid operations

```
Session
   ↓ .upsert(key)
OperationBuilder (Fluent API)
   ↓ .bin("name").setTo("Alice")
   ↓ .bin("age").setTo(30)
   ↓ .execute()
Result
```

## The Fluent Pattern

### Traditional vs Fluent

**Traditional Aerospike Client**:
```java
// Verbose, error-prone
WritePolicy policy = new WritePolicy();
policy.expiration = 3600;

Key key = new Key("test", "users", "alice");
Bin bin1 = new Bin("name", "Alice");
Bin bin2 = new Bin("age", 30);

client.put(policy, key, bin1, bin2);
```

**Fluent Client**:
```java
// Clean, readable
session.upsert(users.id("alice"))
    .bin("name").setTo("Alice")
    .bin("age").setTo(30)
    .expireRecordAfter(Duration.ofHours(1))
    .execute();
```

## Operation Builders

The Fluent Client provides specialized builders for different operations:

### OperationBuilder

For single-record operations:

```java
// Upsert (create or replace)
session.upsert(key)
    .bin("name").setTo("Alice")
    .execute();

// Insert (must not exist)
session.insert(key)
    .bin("name").setTo("Alice")
    .execute();

// Update (must exist)
session.update(key)
    .bin("age").add(1)
    .execute();

// Delete
session.delete(key)
    .execute();
```

### MultiValueBuilder

For batch operations:

```java
List<Key> keys = users.ids("alice", "bob", "carol");

// Same operation on multiple records
session.upsert(keys)
    .bin("status").setTo("active")
    .bin("updatedAt").setTo(System.currentTimeMillis())
    .execute();
```

### QueryBuilder

For queries:

```java
// Simple query
session.query(users)
    .where("$.age > 18")
    .limit(100)
    .execute();

// Complex query
session.query(users)
    .where("$.age > 18 and $.country == 'US'")
    .sortReturnedSubsetBy("age", SortDir.SORT_DESC)
    .pageSize(20)
    .execute();
```

## Type Safety in Action

### 1. Compile-Time Method Validation

```java
// ✅ Compiles: Valid method chain
session.upsert(key)
    .bin("name").setTo("Alice")
    .execute();

// ❌ Won't compile: Missing execute()
session.upsert(key)
    .bin("name").setTo("Alice");
// Result not used - code doesn't execute!

// ❌ Won't compile: Can't call execute() twice
var builder = session.upsert(key).bin("x").setTo(1);
builder.execute();
builder.execute();  // Type system prevents this
```

### 2. Type-Safe Bin Values

```java
// Type-safe value setting
session.upsert(key)
    .bin("name").setTo("Alice")          // String
    .bin("age").setTo(30)                // int
    .bin("balance").setTo(100.50)        // double
    .bin("active").setTo(true)           // boolean
    .bin("tags").setTo(List.of("a", "b")) // List
    .bin("metadata").setTo(Map.of("k", "v")) // Map
    .execute();

// ❌ Type mismatch caught by IDE/compiler
// session.upsert(key).bin("age").setTo("thirty");  // Wrong type
```

### 3. IDE Autocomplete

The fluent API provides excellent IDE support:

```java
session.  // ← IDE shows: upsert, insert, update, delete, query, info, truncate
    upsert(key).  // ← IDE shows: bin, expireRecordAfter, execute, etc.
        bin("name").  // ← IDE shows: setTo, add, append, prepend, get, etc.
            setTo("Alice").  // ← IDE shows: execute and more bin operations
```

## Common Operations

### Writing Data

```java
// Simple write
session.upsert(key)
    .bin("name").setTo("Alice")
    .execute();

// Multiple bins
session.upsert(key)
    .bin("name").setTo("Alice")
    .bin("age").setTo(30)
    .bin("email").setTo("alice@example.com")
    .execute();

// With expiration
session.upsert(key)
    .bin("session_token").setTo("abc123")
    .expireRecordAfter(Duration.ofHours(24))
    .execute();
```

### Updating Data

```java
// Increment
session.update(key)
    .bin("views").add(1)
    .execute();

// Append to string
session.update(key)
    .bin("log").append(" - new entry")
    .execute();

// Update with generation check
session.update(key)
    .bin("balance").add(100)
    .ensureGenerationIs(5)  // Only if generation == 5
    .execute();
```

### Reading Data

```java
// Read all bins
RecordStream result = session.query(key).execute();

// Read specific bins
RecordStream result = session.query(key)
    .readingOnlyBins("name", "age")
    .execute();

// Read with no bins (metadata only)
RecordStream result = session.query(key)
    .withNoBins()
    .execute();
```

## Advanced Type-Safe Features

### Expiration Control

```java
// Expire after duration
session.upsert(key)
    .bin("data").setTo("value")
    .expireRecordAfter(Duration.ofDays(7))
    .execute();

// Expire at specific time
LocalDateTime expiryTime = LocalDateTime.now().plusDays(30);
session.upsert(key)
    .bin("data").setTo("value")
    .expireRecordAt(expiryTime)
    .execute();

// Never expire
session.upsert(key)
    .bin("data").setTo("value")
    .neverExpire()
    .execute();

// Don't change current expiration
session.update(key)
    .bin("views").add(1)
    .withNoChangeInExpiration()
    .execute();
```

### Generation Control

```java
// Optimistic locking
RecordStream result = session.query(key).execute();
if (result.hasNext()) {
    KeyRecord record = result.next();
    int generation = record.record.generation;
    
    // Only update if generation hasn't changed
    session.update(key)
        .bin("balance").add(100)
        .ensureGenerationIs(generation)
        .execute();
}
```

### Complex Data Type Operations

```java
// List operations
session.update(key)
    .onListIndex(0).remove()          // Remove first element
    .execute();

session.update(key)
    .onListIndex(5).get()              // Get element at index 5
    .execute();

// Map operations
session.update(key)
    .onMapKey("field").remove()        // Remove by key
    .execute();

session.update(key)
    .onMapKey("field").get()           // Get by key
    .execute();
```

## Method Chaining Rules

### Order Independence (Within a Group)

```java
// These are equivalent
session.upsert(key)
    .bin("a").setTo(1)
    .bin("b").setTo(2)
    .expireRecordAfter(Duration.ofDays(1))
    .execute();

session.upsert(key)
    .expireRecordAfter(Duration.ofDays(1))
    .bin("a").setTo(1)
    .bin("b").setTo(2)
    .execute();
```

### Execute Must Be Last

```java
// ✅ Correct
session.upsert(key)
    .bin("x").setTo(1)
    .execute();

// ❌ Wrong: Can't chain after execute
session.upsert(key)
    .execute()
    .bin("x").setTo(1);  // Won't compile
```

## Error Handling

### Type-Safe Error Handling

```java
try {
    session.upsert(key)
        .bin("name").setTo("Alice")
        .execute();
} catch (AerospikeException.RecordExists e) {
    // Handle record exists error
} catch (AerospikeException.Timeout e) {
    // Handle timeout
} catch (AerospikeException e) {
    // Handle other Aerospike errors
}
```

### Builder Validation

The builder validates at build time:

```java
// ❌ Won't compile: Invalid expiration
// session.upsert(key)
//     .expireRecordAfter(Duration.ofSeconds(-10));  // Negative duration

// ❌ Won't compile: Empty bin name
// session.upsert(key).bin("").setTo("value");
```

## Best Practices

### ✅ DO

**Use clear, readable chains**
```java
// ✅ Good: Each operation on its own line
session.upsert(key)
    .bin("name").setTo("Alice")
    .bin("age").setTo(30)
    .expireRecordAfter(Duration.ofDays(365))
    .execute();
```

**Keep chains focused**
```java
// ✅ Good: One logical operation
session.update(key)
    .bin("loginCount").add(1)
    .bin("lastLogin").setTo(System.currentTimeMillis())
    .execute();
```

**Reuse builders when appropriate**
```java
// ✅ Good: Build once, execute multiple times with different data
DataSet users = DataSet.of("test", "users");

for (String userId : userIds) {
    session.upsert(users.id(userId))
        .bin("status").setTo("migrated")
        .execute();
}
```

### ❌ DON'T

**Don't create overly long chains**
```java
// ❌ Bad: Too many operations, hard to read
session.upsert(key)
    .bin("a").setTo(1).bin("b").setTo(2).bin("c").setTo(3)
    .bin("d").setTo(4).bin("e").setTo(5).execute();
```

**Don't forget to execute**
```java
// ❌ Bad: Builder created but never executed
var builder = session.upsert(key).bin("x").setTo(1);
// Nothing happens!

// ✅ Good: Always call execute()
builder.execute();
```

**Don't share builders across threads**
```java
// ❌ Bad: Not thread-safe
OperationBuilder builder = session.upsert(key);
CompletableFuture.allOf(
    CompletableFuture.runAsync(() -> builder.bin("x").setTo(1)),
    CompletableFuture.runAsync(() -> builder.bin("y").setTo(2))
).join();

// ✅ Good: Create new builder in each thread
CompletableFuture.allOf(
    CompletableFuture.runAsync(() -> 
        session.upsert(key1).bin("x").setTo(1).execute()
    ),
    CompletableFuture.runAsync(() -> 
        session.upsert(key2).bin("y").setTo(2).execute()
    )
).join();
```

## Complete Example

```java
public class UserService {
    private final Session session;
    private final DataSet users;
    
    public UserService(Session session) {
        this.session = session;
        this.users = DataSet.of("app", "users");
    }
    
    public void createUser(String userId, String name, String email) {
        session.insert(users.id(userId))
            .bin("name").setTo(name)
            .bin("email").setTo(email)
            .bin("createdAt").setTo(System.currentTimeMillis())
            .bin("loginCount").setTo(0)
            .expireRecordAfter(Duration.ofDays(365))
            .execute();
    }
    
    public void recordLogin(String userId) {
        session.update(users.id(userId))
            .bin("loginCount").add(1)
            .bin("lastLoginAt").setTo(System.currentTimeMillis())
            .withNoChangeInExpiration()  // Don't reset TTL
            .execute();
    }
    
    public void updateEmail(String userId, String newEmail, int expectedGeneration) {
        session.update(users.id(userId))
            .bin("email").setTo(newEmail)
            .bin("emailUpdatedAt").setTo(System.currentTimeMillis())
            .ensureGenerationIs(expectedGeneration)  // Optimistic locking
            .execute();
    }
    
    public void deactivateUsers(List<String> userIds) {
        List<Key> keys = users.ids(userIds);
        
        session.update(keys)
            .bin("status").setTo("inactive")
            .bin("deactivatedAt").setTo(System.currentTimeMillis())
            .execute();
    }
}
```

## API Reference

For complete documentation:
- [OperationBuilder API](../api/operations/operation-builder.md)
- [QueryBuilder API](../api/operations/query-builder.md)
- [Session API](../api/connection/session.md)

## Next Steps

- **[Object Mapping](./object-mapping.md)** - Work with POJOs instead of bins
- **[Creating Records](../guides/crud/creating-records.md)** - Apply these concepts
- **[Querying Data](../guides/querying/simple-queries.md)** - Advanced queries

---

**Questions?** Check the [FAQ](../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
