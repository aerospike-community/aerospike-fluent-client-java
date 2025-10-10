# Creating Records

Learn how to insert new records into Aerospike using the Fluent Client.

## Goal

By the end of this guide, you'll know how to:
- Insert records with `insert()` (fail if exists)
- Upsert records with `upsert()` (create or replace)
- Set individual and multiple bins
- Work with different data types
- Set expiration times
- Perform batch operations
- Handle errors properly

## Prerequisites

- [Quick Start](../../getting-started/quickstart.md) completed
- Understanding of [DataSets & Keys](../../concepts/datasets-and-keys.md)

---

## Insert vs Upsert

### `insert()` - Fail if Record Exists

Use when you want to **ensure** the record doesn't already exist:

```java
try {
    session.insert(users.id("alice"))
        .bin("name").setTo("Alice")
        .execute();
} catch (AerospikeException.RecordExists e) {
    System.err.println("Record 'alice' already exists. Insert failed as expected.");
    // This is the expected behavior for insert() on an existing key
}
// Throws AerospikeException.RecordExists if record exists
```

**Use cases:**
- Creating unique user accounts
- Preventing duplicate orders
- Creating unique session tokens

### `upsert()` - Create or Replace

Use when you want to **create or update** the record:

```java
session.upsert(users.id("alice"))
    .bin("name").setTo("Alice")
    .execute();
// Creates record or updates if exists
```

**Use cases:**
- Caching (replace stale data)
- Idempotent operations
- Configuration storage

---

## Step-by-Step: Creating Your First Record

### 1. Set Up DataSet and Key

```java
DataSet users = DataSet.of("test", "users");
Key userKey = users.id("alice");
```

### 2. Insert with Single Bin

```java
session.insert(userKey)
    .bin("name").setTo("Alice")
    .execute();
```

### 3. Insert with Multiple Bins

```java
session.insert(userKey)
    .bin("name").setTo("Alice Johnson")
    .bin("age").setTo(30)
    .bin("email").setTo("alice@example.com")
    .bin("active").setTo(true)
    .execute();
```

### 4. Insert with Expiration

```java
import java.time.Duration;

session.insert(userKey)
    .bin("session_token").setTo("abc123")
    .expireRecordAfter(Duration.ofHours(24))
    .execute();
```

---

## Working with Data Types

### Primitive Types

```java
session.upsert(key)
    .bin("name").setTo("Alice")           // String
    .bin("age").setTo(30)                 // int
    .bin("height").setTo(165.5)           // double
    .bin("active").setTo(true)            // boolean
    .bin("balance").setTo(1000L)          // long
    .execute();
```

### Collections

```java
import java.util.List;
import java.util.Map;

session.upsert(key)
    .bin("tags").setTo(List.of("java", "aerospike", "developer"))
    .bin("metadata").setTo(Map.of(
        "created", "2025-01-01",
        "department", "engineering",
        "level", 3
    ))
    .execute();
```

### Binary Data

```java
import java.nio.file.Files;
import java.nio.file.Path;

byte[] imageData = Files.readAllBytes(Path.of("avatar.jpg"));
session.upsert(key)
    .bin("avatar").setTo(imageData)
    .execute();
```

### Dates and Times

```java
import java.time.LocalDateTime;
import java.time.Instant;

session.upsert(key)
    // As ISO-8601 string
    .bin("createdAt").setTo(LocalDateTime.now().toString())
    
    // As epoch milliseconds
    .bin("timestamp").setTo(System.currentTimeMillis())
    
    // As Instant
    .bin("instant").setTo(Instant.now().toEpochMilli())
    .execute();
```

---

## Batch Operations

### Creating Multiple Records

```java
DataSet users = DataSet.of("test", "users");

// Create multiple records with same bins
session.upsert(users.ids(1, 2, 3, 4, 5))
    .bin("status").setTo("pending")
    .bin("createdAt").setTo(System.currentTimeMillis())
    .execute();
```

### Creating Different Records in Loop

```java
List<User> usersToCreate = Arrays.asList(
    new User("alice", "Alice Johnson", 30),
    new User("bob", "Bob Smith", 25),
    new User("carol", "Carol Davis", 35)
);

for (User user : usersToCreate) {
    session.insert(users.id(user.getId()))
        .bin("name").setTo(user.getName())
        .bin("age").setTo(user.getAge())
        .execute();
}
```

---

## Complete Example: User Registration

```java
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;

public class UserRegistration {
    private final Session session;
    private final DataSet users;
    
    public UserRegistration(Session session) {
        this.session = session;
        this.users = DataSet.of("app", "users");
    }
    
    public boolean registerUser(String userId, String name, String email, 
                               String password) {
        try {
            String passwordHash = hashPassword(password);
            
            session.insert(users.id(userId))
                .bin("userId").setTo(userId)
                .bin("name").setTo(name)
                .bin("email").setTo(email)
                .bin("passwordHash").setTo(passwordHash)
                .bin("createdAt").setTo(LocalDateTime.now().toString())
                .bin("loginCount").setTo(0)
                .bin("active").setTo(true)
                .bin("roles").setTo(List.of("user"))
                .bin("metadata").setTo(Map.of(
                    "source", "web",
                    "emailVerified", false,
                    "twoFactorEnabled", false
                ))
                .expireRecordAfter(Duration.ofDays(365))  // Inactive user cleanup
                .execute();
            
            System.out.println("✅ User registered: " + userId);
            return true;
            
        } catch (AerospikeException.RecordExists e) {
            System.out.println("❌ User already exists: " + userId);
            return false;
        } catch (Exception e) {
            System.err.println("❌ Error registering user: " + e.getMessage());
            throw e;
        }
    }
    
    private String hashPassword(String password) {
        // Use proper password hashing (BCrypt, Argon2, etc.)
        return "hashed_" + password;  // Placeholder
    }
}
```

---

## Common Pitfalls

### ❌ Forgetting to Call `.execute()`

```java
// This does NOTHING!
session.insert(key)
    .bin("name").setTo("Alice");
// Missing .execute()
```

### ❌ Using insert() When You Mean upsert()

```java
// Will fail if record exists
session.insert(key)
    .bin("lastLogin").setTo(System.currentTimeMillis())
    .execute();

// Better: Use upsert() for idempotent operations
session.upsert(key)
    .bin("lastLogin").setTo(System.currentTimeMillis())
    .execute();
```

### ❌ Setting Wrong Expiration

```java
// DON'T set short expiration on permanent data
session.insert(key)
    .bin("userId").setTo(123)
    .expireRecordAfter(Duration.ofMinutes(5))  // ⚠️ User deleted after 5 min!
    .execute();
```

---

## Error Handling

When creating records, several issues can occur. It's crucial to handle them gracefully.

### Record Already Exists

This is the most common "error" when using `insert()`. It's a feature, not a bug.

```java
import com.aerospike.client.AerospikeException;

try {
    session.insert(users.id("new-user"))
        .bin("name").setTo("New User")
        .execute();
} catch (AerospikeException.RecordExists e) {
    // The user ID is already taken.
    // Return an error message to the client or try a different ID.
    System.err.println("User ID 'new-user' is already taken.");
}
```

### Timeout Error

If the operation takes too long due to network or server load, a `Timeout` exception is thrown.

```java
try {
    session.insert(users.id("another-user"))
        .bin("name").setTo("Another User")
        .execute();
} catch (AerospikeException.Timeout e) {
    System.err.println("Operation timed out. The record may or may not have been written.");
    // This is an "in-doubt" situation.
    // You may need to read back the record to confirm its status or implement retry logic.
}
```

### Namespace Not Found

If the namespace you are writing to is not configured on the server.

```java
DataSet wrongDataSet = DataSet.of("nonexistent_namespace", "users");
try {
    session.insert(wrongDataSet.id("some-user"))
        .bin("name").setTo("Some User")
        .execute();
} catch (AerospikeException e) {
    if (e.getResultCode() == com.aerospike.client.ResultCode.INVALID_NAMESPACE) {
        System.err.println("The namespace 'nonexistent_namespace' does not exist on the server.");
        // Check your client and server configurations.
    }
}
```

> See the **[Common Errors & Solutions](../../troubleshooting/common-errors.md)** guide for a more comprehensive list.

---

## Best Practices

### ✅ DO

**Use clear, descriptive bin names**
```java
session.insert(key)
    .bin("firstName").setTo("Alice")
    .bin("lastName").setTo("Johnson")
    .bin("email").setTo("alice@example.com")
    .execute();
```

**Set appropriate TTLs**
```java
// Session data
session.upsert(sessionKey)
    .bin("token").setTo(token)
    .expireRecordAfter(Duration.ofHours(24))
    .execute();

// Permanent data
session.insert(userKey)
    .bin("userId").setTo(id)
    .neverExpire()
    .execute();
```

**Use batch operations for multiple records**
```java
// ✅ Good: Single round-trip
session.upsert(keys)
    .bin("status").setTo("active")
    .execute();
```

### ❌ DON'T

**Don't create records in loops without batching**
```java
// ❌ Bad: Multiple round-trips
for (int i = 0; i < 1000; i++) {
    session.insert(users.id(i))
        .bin("value").setTo(i)
        .execute();
}
```

**Don't store sensitive data unencrypted**
```java
// ❌ Bad
session.insert(key)
    .bin("password").setTo(plainTextPassword)
    .execute();

// ✅ Good
session.insert(key)
    .bin("passwordHash").setTo(hashedPassword)
    .execute();
```

---

## Performance Considerations

### Batch Operations Are Faster

```java
// ❌ Slow: 1000 round-trips
for (int i = 0; i < 1000; i++) {
    session.insert(users.id(i))
        .bin("value").setTo(i)
        .execute();
}

// ✅ Fast: 1 round-trip
List<Key> keys = users.ids(IntStream.range(0, 1000).boxed().toList());
session.upsert(keys)
    .bin("value").setTo(0)
    .execute();
```

### Minimize Bin Count

- Each bin adds overhead
- Keep under 32 bins per record
- Consider combining related data into collections

---

## Next Steps

- **[Reading Records](./reading-records.md)** - Learn to read data
- **[Updating Records](./updating-records.md)** - Learn to update data
- **[Object Mapping](../object-mapping/creating-mappers.md)** - Work with POJOs
- **[Batch Operations](../performance/batch-operations.md)** - Optimize throughput

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
