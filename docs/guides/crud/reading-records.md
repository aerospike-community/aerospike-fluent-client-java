# Reading Records

Learn how to read records from Aerospike using the Fluent Client.

## Goal

By the end of this guide, you'll know how to:
- Read a single record by key
- Read specific bins of a record
- Read multiple records in a batch
- Handle records that don't exist
- Scan all records in a set
- Process results using `RecordStream`

## Prerequisites

- [Creating Records](./creating-records.md) completed
- Understanding of [DataSets & Keys](../../concepts/datasets-and-keys.md)

---

## The `query()` Method

All read operations start with the `session.query()` method. The Fluent Client automatically uses the most efficient method based on what you provide:

- **Single Key**: `session.query(users.id("alice"))` → **Point Read** (fastest)
- **List of Keys**: `session.query(users.ids("a", "b"))` → **Batch Read** (very fast)
- **DataSet**: `session.query(users)` → **Scan** (slower, reads all records)

---

## Reading a Single Record

### 1. Set Up DataSet and Key

```java
DataSet users = DataSet.of("test", "users");
Key userKey = users.id("alice");
```

### 2. Read All Bins

```java
import com.aerospike.RecordStream;
import com.aerospike.KeyRecord;
import java.util.Optional;

RecordStream result = session.query(userKey).execute();

if (result.hasNext()) {
    KeyRecord record = result.next();
    String name = record.record.getString("name");
    int age = record.record.getInt("age");
    
    System.out.println("Name: " + name + ", Age: " + age);
} else {
    System.out.println("Record not found");
}

// Or using Optional for cleaner handling
Optional<KeyRecord> optionalRecord = result.getFirst();
optionalRecord.ifPresent(record -> {
    System.out.println("Name: " + record.record.getString("name"));
});
```

### 3. Read Specific Bins

For efficiency, only read the bins you need:

```java
RecordStream result = session.query(userKey)
    .readingOnlyBins("name", "email")
    .execute();

if (result.hasNext()) {
    KeyRecord record = result.next();
    String name = record.record.getString("name");
    String email = record.record.getString("email");
    
    // Note: age will be null
    // int age = record.record.getInt("age"); // This would throw NullPointerException
    
    System.out.println("Name: " + name + ", Email: " + email);
}
```

### 4. Read Only Record Metadata

To check if a record exists without fetching data:

```java
RecordStream result = session.query(userKey)
    .withNoBins()
    .execute();

if (result.hasNext()) {
    KeyRecord record = result.next();
    int generation = record.record.generation;
    int ttl = record.record.expiration;
    
    System.out.println("Record exists with generation: " + generation);
} else {
    System.out.println("Record does not exist");
}
```

---

## Reading Multiple Records (Batch)

### 1. Set Up Keys

```java
DataSet users = DataSet.of("test", "users");
List<Key> keys = users.ids("alice", "bob", "charlie");
```

### 2. Batch Read All Bins

```java
RecordStream results = session.query(keys).execute();

results.forEach(record -> {
    System.out.println("User: " + record.record.getString("name"));
});

// Note: The order of results matches the order of keys provided
```

### 3. Batch Read Specific Bins

```java
RecordStream results = session.query(keys)
    .readingOnlyBins("name", "lastLogin")
    .execute();

results.forEach(record -> {
    System.out.println(
        "User: " + record.record.getString("name") +
        ", Last Login: " + record.record.getLong("lastLogin")
    );
});
```

### 4. Handling Missing Records in Batch

If a key is not found, it's omitted from the `RecordStream`:

```java
List<Key> keys = users.ids("alice", "dave_not_exists", "charlie");
RecordStream results = session.query(keys).execute();

int count = 0;
while (results.hasNext()) {
    count++;
    results.next();
}
// count will be 2
```

---

## Scanning All Records in a Set

Use a `DataSet` to scan all records.

> **Warning**: Scans can be resource-intensive on large datasets. Use with caution in production.

### 1. Set Up DataSet

```java
DataSet users = DataSet.of("test", "users");
```

### 2. Scan All Bins

```java
RecordStream allUsers = session.query(users).execute();

allUsers.forEach(record -> {
    System.out.println("User: " + record.record.getString("name"));
});
```

### 3. Scan with Bin Selection

```java
RecordStream allUserEmails = session.query(users)
    .readingOnlyBins("email")
    .execute();
```

### 4. Limiting Scan Results

Use `limit()` to stop after N records:

```java
RecordStream first10Users = session.query(users)
    .limit(10)
    .execute();
```

---

## Working with `RecordStream`

The `RecordStream` is a powerful, forward-only iterator for results.

### Checking for Results

```java
RecordStream result = session.query(key).execute();

if (result.hasNext()) {
    // Process results
} else {
    // Handle no results
}
```

### Getting the First Record

```java
Optional<KeyRecord> optionalRecord = session.query(key)
    .execute()
    .getFirst();

optionalRecord.ifPresentOrElse(
    record -> System.out.println("Found: " + record.record),
    () -> System.out.println("Not found")
);
```

### Iterating with `forEach`

```java
session.query(keys)
    .execute()
    .forEach(record -> {
        // Process each record
    });
```

### Converting to Java Stream

For advanced stream operations (map, filter, collect):

```java
List<String> names = session.query(users)
    .readingOnlyBins("name")
    .execute()
    .stream()
    .map(kr -> kr.record.getString("name"))
    .filter(name -> name != null && name.startsWith("A"))
    .collect(Collectors.toList());
```

### Converting to List

> **Warning**: This loads all results into memory. Use with caution on large result sets.

```java
List<KeyRecord> records = session.query(keys)
    .execute()
    .toList();
```

---

## Complete Example: User Service

```java
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserService {
    private final Session session;
    private final DataSet users;
    
    public UserService(Session session) {
        this.session = session;
        this.users = DataSet.of("app", "users");
    }
    
    public Optional<User> findById(String userId) {
        return session.query(users.id(userId))
            .execute()
            .getFirst()
            .map(this::mapToUser);
    }
    
    public List<User> findByIds(List<String> userIds) {
        List<Key> keys = users.ids(userIds);
        
        return session.query(keys)
            .execute()
            .stream()
            .map(this::mapToUser)
            .collect(Collectors.toList());
    }
    
    public List<String> getAllUserEmails() {
        return session.query(users)
            .readingOnlyBins("email")
            .execute()
            .stream()
            .map(kr -> kr.record.getString("email"))
            .collect(Collectors.toList());
    }
    
    private User mapToUser(KeyRecord record) {
        return new User(
            (String) record.key.userKey.getObject(),
            record.record.getString("name"),
            record.record.getInt("age"),
            record.record.getString("email")
        );
    }
}
```

---

## Error Handling

While a missing record is not an exception, other errors can occur during read operations.

### Timeout Error

A `Timeout` exception can occur on slow networks or overloaded servers.

```java
import com.aerospike.client.AerospikeException;

try {
    // This operation might be slow
    RecordStream allUsers = session.query(users).execute();
    allUsers.forEach(record -> {
        // process
    });
} catch (AerospikeException.Timeout e) {
    System.err.println("Read operation timed out. The results are incomplete.");
    // Consider increasing the timeout in your Behavior configuration for long scans.
}
```

### Connection Error

If the connection to the cluster is lost during the operation.

```java
try {
    RecordStream results = session.query(keys).execute();
    List<KeyRecord> records = results.toList(); // Network error could happen here
} catch (AerospikeException.Connection e) {
    System.err.println("Connection to the cluster was lost: " + e.getMessage());
    // Implement logic to handle reconnection.
}
```

> See the **[Common Errors & Solutions](../../troubleshooting/common-errors.md)** guide for a more comprehensive list.

---

## Best Practices

### ✅ DO

**Select only necessary bins**
```java
// ✅ Good: Efficient
session.query(key)
    .readingOnlyBins("name", "email")
    .execute();
```

**Use batch reads for multiple keys**
```java
// ✅ Good: Single round trip
session.query(users.ids("a", "b", "c")).execute();
```

**Handle empty results**
```java
session.query(key).execute().getFirst().ifPresent(this::process);
```

**Use Java streams for complex processing**
```java
long activeUsers = session.query(users)
    .readingOnlyBins("active")
    .execute()
    .stream()
    .filter(kr -> kr.record.getBoolean("active"))
    .count();
```

### ❌ DON'T

**Don't fetch all bins if you only need one**
```java
// ❌ Bad: Inefficient
session.query(key).execute(); // Fetches everything
```

**Don't read multiple keys in a loop**
```java
// ❌ Bad: Multiple round trips
for (String id : ids) {
    session.query(users.id(id)).execute();
}
```

**Don't load large scans into memory**
```java
// ❌ Bad: Potential OutOfMemoryError
List<KeyRecord> all = session.query(users).execute().toList();
```

---

## Performance Considerations

### Point Read vs Batch Read vs Scan

| Operation | Speed | Use Case |
|-----------|-------|----------|
| **Point Read** | Fastest (μs) | Single key lookup |
| **Batch Read** | Very Fast (ms) | Multiple key lookups |
| **Scan** | Slow (sec-min) | Full set processing |

### Bin Selection Impact

Fewer bins = less network traffic = faster reads.

- **`withNoBins()`**: Fastest (metadata only)
- **`readingOnlyBins(...)`**: Fast (specific data)
- **(no selection)**: Slowest (all data)

---

## Next Steps

- **[Updating Records](./updating-records.md)** - Learn to update records
- **[Simple Queries](../querying/simple-queries.md)** - Learn to filter and query
- **[Object Mapping](../../concepts/object-mapping.md)** - Work with POJOs

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
