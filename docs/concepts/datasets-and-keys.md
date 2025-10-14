# DataSets & Keys

Learn how to organize data and create keys using the Fluent Client's `DataSet` abstraction.

## Overview

In Aerospike, data is organized in a three-level hierarchy:

```
Namespace (like a database)
   ↓
Set (like a table)
   ↓
Record (identified by a unique key)
```

The **`DataSet`** class represents a namespace + set combination and provides convenient methods for creating keys.

## Creating a DataSet

### Basic DataSet

```java
DataSet users = DataSet.of("test", "users");
```

**Parameters**:
- First: Namespace name (`"test"`)
- Second: Set name (`"users"`)

**Returns**: Immutable `DataSet` instance

### Reusing DataSets

DataSets are immutable and safe to reuse:

```java
public class DataSets {
    public static final DataSet USERS = DataSet.of("app", "users");
    public static final DataSet PRODUCTS = DataSet.of("app", "products");
    public static final DataSet SESSIONS = DataSet.of("app", "sessions");
}

// Use anywhere
session.upsert(DataSets.USERS.id("alice")).bin("name").setTo("Alice").execute();
```

## Creating Keys

The primary purpose of `DataSet` is to create Aerospike keys.

### String Keys

Most common key type:

```java
DataSet users = DataSet.of("test", "users");

// Single key
Key aliceKey = users.id("alice");
Key bobKey = users.id("bob");

// Multiple keys
List<Key> userKeys = users.ids("alice", "bob", "carol");
```

**When to use**: User IDs, session tokens, email addresses, UUIDs

### Integer Keys

For numeric identifiers:

```java
DataSet products = DataSet.of("test", "products");

// Single key
Key product1 = products.id(12345);

// Multiple keys
List<Key> productKeys = products.ids(1, 2, 3, 4, 5);
```

**When to use**: Auto-increment IDs, product codes, numeric identifiers

### Long Keys

For large numeric identifiers:

```java
DataSet sessions = DataSet.of("test", "sessions");

// Single key
Key sessionKey = sessions.id(9876543210L);

// Multiple keys
List<Key> sessionKeys = sessions.ids(100L, 200L, 300L);
```

**When to use**: Timestamps, large IDs, hash values

### Byte Array Keys

For binary or composite keys:

```java
DataSet data = DataSet.of("test", "data");

// From byte array
byte[] keyBytes = "unique-identifier".getBytes();
Key key = data.id(keyBytes);

// From partial array
byte[] largeArray = new byte[100];
Key key = data.id(largeArray, 0, 20);  // Use first 20 bytes

// Multiple keys
byte[][] keyArrays = {key1Bytes, key2Bytes, key3Bytes};
List<Key> keys = data.ids(keyArrays);
```

**When to use**: Hash values, encrypted IDs, composite keys

### Object Keys (Dynamic Type)

When key type is determined at runtime:

```java
DataSet users = DataSet.of("test", "users");

// Automatically determines type
Key key1 = users.idForObject("alice");        // String key
Key key2 = users.idForObject(12345);          // Integer key
Key key3 = users.idForObject(9876543210L);    // Long key
Key key4 = users.idForObject(new byte[]{1,2,3}); // Byte array key
```

**Supported Types**:
- `String`
- `Integer`, `Long`, `Byte`, `Short` (converted to Long)
- `byte[]`

**Throws**: `IllegalArgumentException` for unsupported types

## Using Keys with Operations

### Single Record Operations

```java
DataSet users = DataSet.of("test", "users");

// Write
session.upsert(users.id("alice"))
    .bin("name").setTo("Alice")
    .execute();

// Read
RecordStream result = session.query(users.id("alice"))
    .execute();

// Delete
session.delete(users.id("alice"))
    .execute();
```

### Batch Operations

```java
DataSet users = DataSet.of("test", "users");

// Create multiple keys
List<Key> keys = users.ids("alice", "bob", "carol");

// Batch write
session.upsert(keys)
    .bin("status").setTo("active")
    .execute();

// Batch read
RecordStream results = session.query(keys)
    .execute();
```

### Range-Based Keys

```java
DataSet products = DataSet.of("test", "products");

// Generate range of numeric keys
List<Key> productKeys = products.ids(
    IntStream.range(1, 101)  // IDs 1-100
        .boxed()
        .toList()
);

// Query all
RecordStream results = session.query(productKeys).execute();
```

## Advanced Patterns

### Pattern 1: Key Generation from Domain Objects

```java
public class UserDataSet {
    private static final DataSet USERS = DataSet.of("app", "users");
    
    public static Key keyFor(User user) {
        return USERS.id(user.getId());
    }
    
    public static Key keyFor(String userId) {
        return USERS.id(userId);
    }
}

// Usage
User user = new User("alice", "Alice Johnson");
session.upsert(UserDataSet.keyFor(user))
    .bin("name").setTo(user.getName())
    .execute();
```

### Pattern 2: Composite Keys

For multi-part keys, combine into a single string:

```java
public class CompositeKey {
    public static Key create(DataSet dataSet, String... parts) {
        String composite = String.join(":", parts);
        return dataSet.id(composite);
    }
}

// Usage
DataSet orders = DataSet.of("test", "orders");
Key orderKey = CompositeKey.create(orders, "user123", "2025", "order456");
// Results in key: "user123:2025:order456"
```

### Pattern 3: Hash-Based Keys

For very long identifiers:

```java
import java.security.MessageDigest;
import java.util.Base64;

public class HashKey {
    public static Key create(DataSet dataSet, String longIdentifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(longIdentifier.getBytes());
            String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(hash);
            return dataSet.id(encoded);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Pattern 4: UUID Keys

```java
import java.uuid.UUID;

DataSet entities = DataSet.of("test", "entities");

// Generate UUID key
UUID uuid = UUID.randomUUID();
Key key = entities.id(uuid.toString());

// Or use byte representation
Key key = entities.id(uuidToBytes(uuid));

private static byte[] uuidToBytes(UUID uuid) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
}
```

## TypeSafeDataSet

For working with Java objects, use `TypeSafeDataSet`:

```java
TypeSafeDataSet<Customer> customers = 
    TypeSafeDataSet.of("test", "customers", Customer.class);

// Automatically extracts ID from object
session.upsert(customers)
    .object(new Customer("alice", "Alice Johnson"))
    .execute();
```

See [Object Mapping](./object-mapping.md) for details.

## Namespace and Set Organization

### Namespace Guidelines

**What are namespaces?**
- Similar to databases in relational systems
- Configured on Aerospike server
- Define storage, replication, and retention policies

**Naming conventions**:
- Use lowercase
- Keep short (for efficiency)
- Common patterns: `prod`, `staging`, `test`, `cache`

**Example organization**:
```java
// Separate namespaces by environment
DataSet prodUsers = DataSet.of("prod", "users");
DataSet stagingUsers = DataSet.of("staging", "users");
DataSet testUsers = DataSet.of("test", "users");
```

### Set Guidelines

**What are sets?**
- Similar to tables in relational systems
- Group related records
- Optional (records can exist without a set)

**Naming conventions**:
- Use plural nouns: `users`, `products`, `orders`
- Max 63 characters
- Use underscores for multi-word: `user_sessions`

**Example organization**:
```java
// Organize by entity type
DataSet users = DataSet.of("app", "users");
DataSet userProfiles = DataSet.of("app", "user_profiles");
DataSet userSessions = DataSet.of("app", "user_sessions");

// Or by access pattern
DataSet hotData = DataSet.of("app", "hot_cache");
DataSet coldData = DataSet.of("app", "cold_storage");
```

## Key Best Practices

### ✅ DO

**Use meaningful keys**
```java
// ✅ Good: Clear intent
DataSet users = DataSet.of("app", "users");
Key key = users.id("user:alice:profile");
```

**Keep keys reasonably short**
```java
// ✅ Good: Concise
users.id("u:12345")

// ⚠️ Acceptable but verbose
users.id("user:12345:profile:extended:details")
```

**Use consistent key patterns**
```java
// ✅ Good: Consistent pattern
users.id("user:alice")
products.id("product:12345")
orders.id("order:ORD-98765")
```

**Reuse DataSet instances**
```java
// ✅ Good: Define once, use everywhere
public static final DataSet USERS = DataSet.of("app", "users");
```

### ❌ DON'T

**Don't use extremely long keys**
```java
// ❌ Bad: Too verbose
users.id("user:alice:profile:extended:details:with:lots:of:nested:information")
// Consider hashing or restructuring
```

**Don't embed data in keys unnecessarily**
```java
// ❌ Bad: Data in key
users.id("alice:30:female:USA")
// Use bins instead
```

**Don't use random keys for entities that need lookups**
```java
// ❌ Bad: Can't look up by user ID
users.id(UUID.randomUUID().toString())
// Unless you maintain a separate index
```

## Accessors

Get namespace and set names from DataSet:

```java
DataSet users = DataSet.of("test", "users");

String namespace = users.getNamespace();  // "test"
String set = users.getSet();              // "users"
```

## Complete Example

```java
public class DataOrganization {
    // Define DataSets as constants
    public static final DataSet USERS = DataSet.of("app", "users");
    public static final DataSet PRODUCTS = DataSet.of("app", "products");
    public static final DataSet ORDERS = DataSet.of("app", "orders");
    
    public static void example(Session session) {
        // User operations
        session.upsert(USERS.id("alice"))
            .bin("name").setTo("Alice")
            .bin("email").setTo("alice@example.com")
            .execute();
        
        // Product operations
        session.upsert(PRODUCTS.id(12345))
            .bin("name").setTo("Widget")
            .bin("price").setTo(29.99)
            .execute();
        
        // Order with composite key
        String orderKey = "alice:2025-01-15:ORD-001";
        session.upsert(ORDERS.id(orderKey))
            .bin("userId").setTo("alice")
            .bin("productId").setTo(12345)
            .bin("quantity").setTo(2)
            .execute();
        
        // Batch operations
        List<Key> userKeys = USERS.ids("alice", "bob", "carol");
        session.upsert(userKeys)
            .bin("lastLogin").setTo(System.currentTimeMillis())
            .execute();
    }
}
```

## API Reference

See [DataSet API Reference](../api/operations/dataset.md) for complete documentation.

## Next Steps

- **[Type-Safe Operations](./type-safe-operations.md)** - Learn about fluent operations
- **[Creating Records](../guides/crud/creating-records.md)** - Use DataSets in practice
- **[Querying Data](../guides/querying/simple-queries.md)** - Query with DataSets

---

**Questions?** Check the [FAQ](../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
