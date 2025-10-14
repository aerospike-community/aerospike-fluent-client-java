# `Session`

The primary interface for performing database operations.

`com.aerospike.Session`

## Overview

The `Session` is the main entry point for all database interactions in the Fluent Client. It provides methods for creating operation builders (`upsert`, `update`, `query`, etc.) and executing database commands.

A `Session` is created from a `Cluster` object and is configured with a specific `Behavior` that defines the policies (timeouts, retries) for all operations performed within that session.

## Creating a `Session`

You don't create a `Session` directly. You obtain it from an active `Cluster` object.

```java
import com.aerospike.Cluster;
import com.aerospike.Session;
import com.aerospike.policy.Behavior;

// Assume cluster is an active connection
Cluster cluster = ...;

// Create a session with the default behavior
Session defaultSession = cluster.createSession(Behavior.DEFAULT);

// Create a session with a custom behavior
Behavior customBehavior = ...;
Session customSession = cluster.createSession(customBehavior);
```

## Core Method Categories

### 1. Write Operations

These methods return an `OperationBuilder` for constructing CUD (Create, Update, Delete) operations.

- `upsert(Key)` / `upsert(DataSet)` / `upsert(TypeSafeDataSet<T>)`
- `insertInto(Key)` / `insertInto(DataSet)` / `insertInto(TypeSafeDataSet<T>)`
- `update(Key)` / `update(DataSet)` / `update(TypeSafeDataSet<T>)`
- `touch(Key)` / `touch(DataSet)` / `touch(TypeSafeDataSet<T>)`
- `delete(Key)` / `delete(List<Key>)`

**Example**:
```java
session.upsert(users.id("alice"))
    .bin("age").setTo(31)
    .execute();
```

### 2. Read Operations

These methods return a `QueryBuilder` for constructing R (Read) operations.

- `query(Key)` / `query(List<Key>)`
- `query(DataSet)` / `query(TypeSafeDataSet<T>)`

**Example**:
```java
RecordStream results = session.query(users)
    .where("$.city == 'London'")
    .execute();
```

### 3. Administrative Operations

- `info()`: Returns an `InfoCommands` interface for executing info commands against the cluster.
- `truncate(DataSet)`: Deletes all records in a set.

**Example**:
```java
// Get all namespaces in the cluster
Set<String> namespaces = session.info().namespaces();

// Delete all records from the "users" set in the "test" namespace
session.truncate(DataSet.of("test", "users"));
```

## Key Methods

### `upsert(...)`

Creates a new record or replaces it if it already exists.

- **Returns**: `OperationBuilder`

```java
// For a single key
session.upsert(users.id("alice"));

// For multiple keys (batch operation)
session.upsert(users.ids("alice", "bob"));

// For a TypeSafeDataSet (for object mapping)
session.upsert(TypeSafeDataSet.of("test", "users", User.class));
```

### `query(...)`

Creates a builder for reading one or more records.

- **Returns**: `QueryBuilder`

```java
// Read a single record by key
session.query(users.id("alice"));

// Read multiple records by key (batch read)
session.query(users.ids("alice", "bob"));

// Scan or query an entire set
session.query(users);
```

### `info()`

Provides access to the cluster metadata and statistics API.

- **Returns**: `InfoCommands`

```java
// Get details for a specific namespace
Optional<NamespaceDetail> details = session.info().namespaceDetails("test");

// Get a list of all secondary indexes
List<Sindex> sindexes = session.info().secondaryIndexes();
```

### `truncate(DataSet)`

Permanently deletes all records within the specified set. This operation cannot be undone.

- **Parameters**:
    - `dataSet` (`DataSet`): The DataSet to truncate.

```java
// WARNING: This deletes all data in the 'users' set.
session.truncate(DataSet.of("test", "users"));
```

## Complete Example: Data Service

```java
import com.aerospike.Cluster;
import com.aerospike.Session;
import com.aerospike.policy.Behavior;
import java.util.Optional;

public class DataService {
    private final Session session;

    public DataService(Cluster cluster) {
        // This service uses a behavior with specific policies
        Behavior serviceBehavior = Behavior.DEFAULT.deriveWithChanges("data-service", builder ->
            builder.onWrites()
                .maximumNumberOfCallAttempts(3)
            .done()
        );
        this.session = cluster.createSession(serviceBehavior);
    }
    
    public void updateUser(User user) {
        TypeSafeDataSet<User> users = TypeSafeDataSet.of("app", "users", User.class);
        session.upsert(users).object(user).execute();
    }
    
    public boolean isClusterStable() {
        // Use the info() API to check cluster status
        return session.info().isClusterStable();
    }
}
```

## Thread Safety

A `Session` object is **thread-safe**. It can be safely shared and reused across multiple threads. It is common to create a small number of `Session` objects with different `Behaviors` at startup and inject them into various services.

## Related Classes

- **`Cluster`**: The factory for `Session` objects.
- **`Behavior`**: Defines the operational policies for a `Session`.
- **`OperationBuilder`**: Returned by write methods (`upsert`, `update`, etc.).
- **`QueryBuilder`**: Returned by read methods (`query`).
- **`InfoCommands`**: Returned by the `info()` method.
