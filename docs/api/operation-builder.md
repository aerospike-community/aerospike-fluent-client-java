# `OperationBuilder`

The fluent builder for constructing CUD (Create, Update, Delete) and CDT operations.

`com.aerospike.OperationBuilder`

## Overview

`OperationBuilder` is the primary tool for defining the details of a write operation. You obtain an instance of this builder by calling a write method on a `Session` (e.g., `session.upsert(key)`).

The builder uses a fluent API, allowing you to chain methods to define which bins to modify, what operations to perform, and what policies to apply. The operation is not sent to the cluster until a **terminal method** like `execute()` is called.

## Obtaining an `OperationBuilder`

```java
// From a single key
OperationBuilder builder = session.upsert(users.id("alice"));

// From a list of keys (for a batch operation)
OperationBuilder batchBuilder = session.update(users.ids("alice", "bob"));

// From a TypeSafeDataSet (for an object operation)
OperationBuilder objectBuilder = session.insertInto(users).object(myUser);
```

## Core Method Categories

### 1. Bin Modification

These methods define the core CUD operations on a bin.

- `bin("name").setTo(value)`: Sets or overwrites a bin's value.
- `bin("name").add(number)`: Atomically adds a number to a bin.
- `bin("name").append(string)`: Atomically appends a string to a bin.
- `bin("name").prepend(string)`: Atomically prepends a string to a bin.

### 2. Complex Data Type (CDT) Operations

These methods allow for operations on Lists and Maps within a bin.

- `onList("name").append(value)`: Appends an item to a list.
- `onMapKey("name", "key").setTo(value)`: Sets a value for a key in a map.

### 3. Record-Level Policies

These methods control the behavior of the entire record operation.

- `expireRecordAfter(Duration)`: Sets the Time-To-Live (TTL) for the record.
- `ensureGenerationIs(int)`: Enables optimistic locking for the operation.

### 4. Terminal Methods

These methods trigger the execution of the defined operation.

- `execute()`: Executes the operation.
- `get()`: Executes the operation and returns specified bins.

---

## Key Methods

### `bin(String binName)`

Specifies the bin to operate on. This is the entry point for most bin modifications.

- **Returns**: `BinBuilder` - A builder for defining the operation on that bin.

```java
// Chain setTo() after bin()
session.upsert(key).bin("name").setTo("Alice");

// Chain add() after bin()
session.update(key).bin("loginCount").add(1);
```

### `expireRecordAfter(Duration ttl)`

Sets the TTL for the record. After the duration expires, the record will be automatically deleted by the server.

- **Parameters**:
    - `ttl` (`Duration`): The duration from now after which the record should expire.

```java
session.upsert(sessionKey)
    .bin("data").setTo(...)
    .expireRecordAfter(Duration.ofHours(24))
    .execute();
```

### `neverExpire()`

Removes the TTL from a record, making it permanent until explicitly deleted.

```java
session.update(userKey)
    .bin("plan").setTo("premium")
    .neverExpire()
    .execute();
```

### `ensureGenerationIs(int generation)`

Specifies a generation value for an optimistic lock. The operation will only succeed if the record's current generation on the server matches the provided value.

- **Parameters**:
    - `generation` (`int`): The expected generation of the record.
- **Throws**: `AerospikeException.Generation` if the generation check fails.

```java
int knownGeneration = ...; // From a previous read
session.update(key)
    .bin("balance").setTo(newBalance)
    .ensureGenerationIs(knownGeneration)
    .execute();
```

### `execute()`

Executes the operation. For write operations, this is often a "fire and forget" call, though it will throw an exception on failure.

- **Returns**: `RecordStream` (may be empty for pure write operations).

```java
session.update(key).bin("status").setTo("inactive").execute();
```

### `get()`

Executes a read operation on the specified bins. This is useful for read-modify-write scenarios where you want the new value back.

- **Returns**: The value of the requested bin.

```java
// Atomically add 1 and get the new value
long newLoginCount = (long) session.update(key)
    .bin("loginCount").add(1).get();
```

---

## Complete Example: User Profile Update

This example demonstrates chaining multiple operations and policies.

```java
public void updateUserLogin(String userId, int knownGeneration) {
    Key userKey = users.id(userId);
    
    try {
        session.update(userKey)
            // Atomically increment the login counter
            .bin("loginCount").add(1)
            
            // Update the last login timestamp
            .bin("lastLogin").setTo(System.currentTimeMillis())
            
            // Append an event to a log list
            .onList("loginHistory").append(System.currentTimeMillis())

            // Ensure we don't overwrite a concurrent update
            .ensureGenerationIs(knownGeneration)

            // Extend the user's record expiration by 30 days
            .expireRecordAfter(Duration.ofDays(30))

            // Execute the operation
            .execute();
            
        System.out.println("User login updated successfully.");

    } catch (AerospikeException.Generation e) {
        System.err.println("Conflict: User profile was updated by another process. Please retry.");
    }
}
```

## Thread Safety

`OperationBuilder` is **not thread-safe**. It is a stateful builder that should be created, configured, and used within a single method scope. Do not share `OperationBuilder` instances across threads.

## Related Classes

- **`Session`**: The factory for `OperationBuilder` instances.
- **`BinBuilder`**: The builder returned by `bin()` for specifying operations.
- **`ListBuilder` / `MapBuilder`**: Builders for CDT operations.
