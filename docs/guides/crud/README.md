# CRUD Operations

Learn how to Create, Read, Update, and Delete records in Aerospike using the Fluent Client.

## Guides

### [Creating Records](./creating-records.md)
Learn how to insert new records into Aerospike.

**You'll learn:**
- Difference between `insert()` and `upsert()`
- Setting bin values (all data types)
- Batch create operations
- Setting expiration times
- Error handling

**Time**: 15 minutes

---

### [Reading Records](./reading-records.md)
Learn how to read records from Aerospike.

**You'll learn:**
- Reading single records
- Reading specific bins
- Batch read operations
- Scanning entire sets
- Handling missing records

**Time**: 15 minutes

---

### [Updating Records](./updating-records.md)
Learn how to update existing records.

**You'll learn:**
- Updating bin values
- Atomic operations (add, append, prepend)
- Conditional updates (generation checking)
- Batch update operations
- Partial updates

**Time**: 15 minutes

---

### [Deleting Records](./deleting-records.md)
Learn how to delete records from Aerospike.

**You'll learn:**
- Deleting single records
- Batch delete operations
- Durable deletes
- Truncating sets
- Handling non-existent records

**Time**: 10 minutes

---

## Quick Reference

### Create
```java
session.insert(key).bin("name").setTo("Alice").execute();     // Insert (fail if exists)
session.upsert(key).bin("name").setTo("Alice").execute();     // Upsert (create or replace)
```

### Read
```java
RecordStream result = session.query(key).execute();                          // Read all bins
RecordStream result = session.query(key).readingOnlyBins("name").execute();  // Read specific bins
```

### Update
```java
session.update(key).bin("age").add(1).execute();                  // Atomic increment
session.update(key).bin("name").setTo("Bob").execute();           // Replace value
```

### Delete
```java
session.delete(key).execute();                                    // Delete record
session.truncate(DataSet.of("test", "users"));                    // Delete all in set
```

---

## Common Patterns

### Pattern: Read-Modify-Write
```java
// Read current value
RecordStream result = session.query(key).execute();
if (result.hasNext()) {
    int currentValue = result.next().recordOrThrow().getInt("counter");
    
    // Update
    session.update(key).bin("counter").setTo(currentValue + 1).execute();
}
```

### Pattern: Upsert with TTL
```java
session.upsert(key)
    .bin("data").setTo("value")
    .expireRecordAfter(Duration.ofDays(7))
    .execute();
```

### Pattern: Batch Operations
```java
List<Key> keys = users.ids("alice", "bob", "carol");

// Batch read
RecordStream results = session.query(keys).execute();

// Batch update
session.update(keys).bin("status").setTo("active").execute();

// Batch delete
session.delete(keys).execute();
```

---

## Best Practices

### ✅ DO

- Use `upsert()` when you don't care if record exists
- Use `insert()` when record must not exist
- Use `update()` when record must exist
- Use batch operations for multiple records
- Set appropriate TTLs for temporary data
- Use atomic operations (`add`, `append`) for concurrent updates

### ❌ DON'T

- Don't use `insert()` when you mean `upsert()`
- Don't perform individual operations in loops (use batch)
- Don't forget to call `.execute()`
- Don't ignore error handling
- Don't set very short TTLs on permanent data

---

## Error Handling

### Common Errors

**RecordExists** - Using `insert()` on existing record
```java
try {
    session.insert(key).bin("name").setTo("Alice").execute();
} catch (AerospikeException.RecordExists e) {
    // Handle: use upsert() or update existing
}
```

**RecordNotFound** - Using `update()` on missing record
```java
try {
    session.update(key).bin("age").add(1).execute();
} catch (AerospikeException.KeyNotFound e) {
    // Handle: use upsert() or insert new
}
```

**Generation** - Concurrent modification detected
```java
try {
    session.update(key).bin("balance").add(100).ensureGenerationIs(5).execute();
} catch (AerospikeException.Generation e) {
    // Handle: retry with new generation
}
```

---

## Next Steps

After mastering CRUD operations:

- **[Querying Data](../querying/simple-queries.md)** - Learn to query and filter
- **[Object Mapping](../object-mapping/creating-mappers.md)** - Work with POJOs
- **[Performance](../performance/batch-operations.md)** - Optimize throughput

---

**Need help?** Check the [FAQ](../../troubleshooting/faq.md) or [Troubleshooting](../../troubleshooting/common-errors.md)
