# Deleting Records

Learn how to delete records from Aerospike using the Fluent Client.

## Goal

By the end of this guide, you'll know how to:
- Delete a single record by key
- Delete multiple records in a batch
- Handle deletion of non-existent records
- Truncate an entire set of records
- Use durable deletes for strong consistency

## Prerequisites

- [Core Concepts](../../concepts/README.md) completed
- Understanding of Aerospike's data hierarchy

---

## The `delete()` Method

All delete operations start with `session.delete()`.

### Deleting a Single Record

```java
DataSet users = DataSet.of("test", "users");
Key userKey = users.id("alice");

boolean deleted = session.delete(userKey).execute();

if (deleted) {
    System.out.println("Record deleted successfully.");
} else {
    System.out.println("Record did not exist.");
}
```

**Return Value**: The `execute()` method for a single delete returns a `boolean`:
- `true` if the record existed and was deleted.
- `false` if the record did not exist.

### Handling Non-Existent Records

Unlike `update()`, `delete()` does **not** throw an exception if the key is not found. It simply returns `false`.

```java
Key nonExistentKey = users.id("dave_not_exists");
boolean wasDeleted = session.delete(nonExistentKey).execute();

// wasDeleted will be false
```

---

## Batch Deletes

Delete multiple records efficiently in a single network call.

### 1. Set Up Keys

```java
DataSet users = DataSet.of("test", "users");
List<Key> userKeys = users.ids("alice", "bob", "charlie");
```

### 2. Perform Batch Delete

```java
session.delete(userKeys).execute();
```

**Return Value**: The `execute()` method for a batch delete is `void`. It does not indicate which records were deleted.

**Note**: This operation is idempotent. Deleting keys that don't exist has no effect and does not cause an error.

---

## Truncating a Set

To delete **all records** in a set, use `session.truncate()`.

> **Warning**: This is a destructive operation. Use with extreme caution.

```java
DataSet users = DataSet.of("test", "users");

// This will delete ALL records in the "users" set
session.truncate(users);
```

**When to use `truncate()`**:
- Clearing test data
- Resetting an environment
- Deleting temporary data sets

**Truncating a Namespace**:
To delete all data in a namespace, you can truncate each set within it, or use `asinfo` tools for a more direct approach on the server side.

---

## Durable Deletes

Durable deletes ensure that a delete operation is not lost during cluster changes. This is important for systems requiring strong consistency.

### How to Enable Durable Deletes

Configure this in your `Behavior`.

```java
Behavior durableDeleteBehavior = Behavior.DEFAULT.deriveWithChanges("durable-delete", builder ->
    builder.onRetryableWrites() // Deletes are considered retryable writes
        .useDurableDelete(true)
    .done()
);

Session durableSession = cluster.createSession(durableDeleteBehavior);

// This delete is now durable
durableSession.delete(userKey).execute();
```

**When to use**:
- When you absolutely must ensure a delete is permanent.
- In systems where "ghost" records (records that reappear after being deleted) are unacceptable.
- In conjunction with strong consistency features.

**Performance**: Durable deletes have a small performance overhead compared to standard deletes.

---

## Conditional Deletes (Tombstoning)

While Aerospike doesn't have a direct conditional delete, you can achieve similar behavior by "tombstoning" a record.

```java
// Instead of deleting, mark the record as deleted
session.update(userKey)
    .bin("status").setTo("DELETED")
    .bin("deletedAt").setTo(System.currentTimeMillis())
    .expireRecordAfter(Duration.ofDays(7)) // Let TTL clean it up later
    .execute();
```

**Benefits**:
- Allows for replication of the "deleted" state via XDR.
- Enables auditing and recovery.
- Can be combined with a generation check for conditional logic.

---

## Best Practices

### ✅ DO

**Use batch deletes for multiple records**
```java
// ✅ Good: Efficient
session.delete(userKeys).execute();
```

**Check the return value for single deletes if you need to know if it existed**
```java
if (session.delete(key).execute()) {
    // Logic for when the record was actually deleted
}
```

**Use `truncate()` for clearing sets, not loops**
```java
// ✅ Good: Fast and efficient
session.truncate(users);

// ❌ Bad: Slow and resource-intensive
session.query(users).execute().forEach(record -> {
    session.delete(record.key).execute();
});
```

### ❌ DON'T

**Don't assume `delete()` will throw an error for non-existent keys**
It's a "fire and forget" operation in many cases. The boolean return for single deletes is your main indicator.

**Don't use `truncate()` on production data without careful consideration**
It's irreversible.

---

## Complete Example: User Deactivation Service

```java
public class UserDeactivationService {
    private final Session session;
    private final DataSet users;
    
    public UserDeactivationService(Session session) {
        this.session = session;
        this.users = DataSet.of("app", "users");
    }
    
    // Soft delete: Mark for deletion
    public void softDelete(String userId) {
        session.update(users.id(userId))
            .bin("status").setTo("DELETED")
            .bin("deletedAt").setTo(System.currentTimeMillis())
            .expireRecordAfter(Duration.ofDays(30)) // Cleanup after 30 days
            .execute();
    }
    
    // Hard delete: Permanently remove
    public boolean hardDelete(String userId) {
        return session.delete(users.id(userId)).execute();
    }
    
    // Batch hard delete
    public void bulkDelete(List<String> userIds) {
        List<Key> keys = users.ids(userIds);
        session.delete(keys).execute();
    }
    
    // Clear all test users
    public void clearTestUsers() {
        DataSet testUsers = DataSet.of("test", "users");
        session.truncate(testUsers);
    }
}
```

---

## Next Steps

Congratulations! You've completed the core CRUD guides.

- **[Simple Queries](../querying/simple-queries.md)** - Learn how to filter and query your data.
- **[Object Mapping](../../concepts/object-mapping.md)** - Apply these concepts to your Java objects.
- **[Transactions Guide](../advanced/transactions.md)** - For multi-record atomic operations.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
