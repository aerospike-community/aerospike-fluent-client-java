# Updating Records

Learn how to update existing records in Aerospike using the Fluent Client.

## Goal

By the end of this guide, you'll know how to:
- Replace bin values with `update()`
- Perform atomic operations (`add`, `append`, `prepend`)
- Use optimistic locking with generation checks
- Perform batch updates on multiple records
- Update complex data types (lists and maps)
- Control record expiration on update

## Prerequisites

- [Reading Records](./reading-records.md) completed
- Understanding of atomic operations in databases

---

## The `update()` Method

All update operations start with `session.update()`. This method requires the record to exist; it will fail if the key is not found.

```java
// Throws AerospikeException.KeyNotFound if key does not exist
session.update(users.id("alice"))
    .bin("age").setTo(31)
    .execute();
```

> **Note**: If you want to create the record if it doesn't exist, use `session.upsert()` instead.

---

## Basic Updates

### 1. Set Up DataSet and Key

```java
DataSet users = DataSet.of("test", "users");
Key userKey = users.id("alice");
```

### 2. Replacing a Bin's Value

Use `setTo()` to overwrite the value of a bin.

```java
// Update Alice's email
session.update(userKey)
    .bin("email").setTo("alice.johnson@newdomain.com")
    .execute();
```

### 3. Updating Multiple Bins

Chain `.bin()` calls to update multiple bins in a single operation.

```java
session.update(userKey)
    .bin("age").setTo(31)
    .bin("lastLogin").setTo(System.currentTimeMillis())
    .bin("city").setTo("New York")
    .execute();
```

---

## Atomic Operations

These operations are performed atomically on the server, making them safe for concurrent use without a read-modify-write cycle.

### Incrementing/Decrementing Numbers

Use `add()` for atomic addition and subtraction.

```java
// Increment login count
session.update(userKey)
    .bin("loginCount").add(1)
    .execute();

// Decrement inventory
session.update(productKey)
    .bin("stock").add(-5)
    .execute();
```

### String Operations

Use `append()` and `prepend()` for atomic string manipulation.

```java
// Append to a log
session.update(logKey)
    .bin("events").append(" | user logged in")
    .execute();

// Prepend to a path
session.update(pathKey)
    .bin("breadcrumbs").prepend("home > ")
    .execute();
```

---

## Conditional Updates (Optimistic Locking)

Use `ensureGenerationIs()` to prevent overwriting concurrent changes (the "lost update" problem).

### The Read-Modify-Write Problem

1.  **Client A** reads a record (generation is 5).
2.  **Client B** reads the same record (generation is 5).
3.  **Client B** updates the record (generation becomes 6).
4.  **Client A** tries to update the record based on its old data, overwriting Client B's change.

### The Solution: Generation Check

```java
// 1. Read the record to get its current generation
RecordStream result = session.query(userKey).execute();

if (result.hasNext()) {
    KeyRecord record = result.next();
    int currentGeneration = record.record.generation;
    int currentBalance = record.record.getInt("balance");

    // 2. Perform update, providing the generation number
    try {
        session.update(userKey)
            .bin("balance").setTo(currentBalance + 100)
            .ensureGenerationIs(currentGeneration)
            .execute();
            
        System.out.println("Update successful!");
        
    } catch (AerospikeException.Generation e) {
        System.err.println("Update failed: Record was modified by another process. Please retry.");
        // Implement retry logic here
    }
}
```

**How it works**: The `ensureGenerationIs()` call tells the server: "Only perform this update if the record's generation is still `currentGeneration`." If it has changed, the operation fails with `AerospikeException.Generation`.

---

## Batch Updates

Update multiple records with the same operation in a single network call.

### 1. Set Up Keys

```java
DataSet users = DataSet.of("test", "users");
List<Key> userKeys = users.ids("alice", "bob", "charlie");
```

### 2. Perform Batch Update

```java
// Deactivate multiple users at once
session.update(userKeys)
    .bin("active").setTo(false)
    .bin("deactivatedAt").setTo(System.currentTimeMillis())
    .execute();
```

---

## Updating Complex Data Types (CDT)

The Fluent Client provides powerful methods for updating lists and maps.

### List Updates

```java
// Append an item to a list
session.update(userKey)
    .onList("roles").append("admin")
    .execute();

// Set an item at a specific index
session.update(userKey)
    .onListIndex("roles", 0).setTo("superadmin")
    .execute();

// Remove an item by index
session.update(userKey)
    .onListIndex("roles", 1).remove()
    .execute();
```

### Map Updates

```java
// Add or update a key-value pair in a map
session.update(userKey)
    .onMapKey("preferences", "theme").setTo("dark")
    .execute();

// Increment a value within a map
session.update(userKey)
    .onMapKey("stats", "logins").add(1)
    .execute();

// Remove a key from a map
session.update(userKey)
    .onMapKey("preferences", "old_setting").remove()
    .execute();
```

See the [Complex Data Types Guide](../cdt/README.md) for more details.

---

## Controlling Expiration on Update

### Resetting the TTL

By default, an update resets the record's Time-To-Live (TTL) based on the namespace default. Use `expireRecordAfter()` to set a new TTL.

```java
// Update the session data and extend its validity for 24 hours
session.update(sessionKey)
    .bin("lastAccessed").setTo(System.currentTimeMillis())
    .expireRecordAfter(Duration.ofHours(24))
    .execute();
```

### Keeping the Existing TTL

Use `withNoChangeInExpiration()` to prevent the update from affecting the record's current TTL.

```java
// Record a page view without extending the user's session
session.update(sessionKey)
    .bin("pageViews").add(1)
    .withNoChangeInExpiration()
    .execute();
```

### Making a Record Permanent

Use `neverExpire()` to remove the TTL and make the record permanent.

```java
// A user just upgraded to a paid plan, so make their record permanent
session.update(userKey)
    .bin("plan").setTo("premium")
    .neverExpire()
    .execute();
```

---

## Best Practices

### ✅ DO

**Use atomic operations for counters**
```java
// ✅ Good: Safe for concurrent updates
session.update(key).bin("views").add(1).execute();

// ❌ Bad: Prone to race conditions
int views = session.query(key).execute().getFirst().get().record.getInt("views");
session.update(key).bin("views").setTo(views + 1).execute();
```

**Use generation checks for critical updates**
```java
session.update(key)
    .bin("balance").setTo(newBalance)
    .ensureGenerationIs(readGeneration)
    .execute();
```

**Use batch updates for multiple records**
```java
session.update(keys).bin("status").setTo("archived").execute();
```

### ❌ DON'T

**Don't use `update()` if the record might not exist**
```java
// ❌ Bad: Throws KeyNotFound exception if user is new
session.update(key).bin("lastLogin").setTo(now).execute();

// ✅ Good: Creates or updates
session.upsert(key).bin("lastLogin").setTo(now).execute();
```

**Don't perform read-modify-write without a generation check**
This is a classic race condition. Always use atomic operations or generation checks for concurrent updates.

---

## Next Steps

- **[Deleting Records](./deleting-records.md)** - Learn to delete data
- **[Transactions Guide](../advanced/transactions.md)** - For multi-record atomic operations
- **[Complex Data Types Guide](../cdt/README.md)** - For advanced list/map operations

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
