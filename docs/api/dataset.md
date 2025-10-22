# `DataSet`

A factory for creating keys for a specific namespace and set combination.

`com.aerospike.DataSet`

## Overview

A `DataSet` is a lightweight, immutable object that represents a specific `namespace` and `set` within an Aerospike cluster. Its primary purpose is to act as a **factory for creating `Key` objects**.

By creating a `DataSet` for a given set (e.g., "users", "products"), you can easily generate keys for records within that set without repeatedly specifying the namespace and set name.

## Creating a `DataSet`

You create a `DataSet` using the static factory method `DataSet.of()`.

```java
import com.aerospike.DataSet;

// Create a DataSet for the "users" set in the "test" namespace
DataSet users = DataSet.of("test", "users");

// Create a DataSet for the "products" set in the "ecommerce" namespace
DataSet products = DataSet.of("ecommerce", "products");
```

## Key Factory Methods

### `id(Object userKey)`

Creates a single `Key` for a record.

- **Parameters**:
    - `userKey` (`Object`): The user-defined key. Can be a `String`, `Integer`, `Long`, or `byte[]`.
- **Returns**: `Key` - An Aerospike key object.

**Example**:
```java
// Key with a string value
Key userKey = users.id("user-123");

// Key with a long value
Key productKey = products.id(98765L);
```

### `ids(Object... userKeys)`

Creates a `List<Key>` for multiple records. This is useful for batch operations.

- **Parameters**:
    - `userKeys` (`Object...`): A varargs array of user-defined keys.
- **Returns**: `List<Key>`

**Example**:
```java
// Create a list of keys for a batch read
List<Key> userKeys = users.ids("alice", "bob", "charlie");
```

### `id(T element, RecordMapper<T> mapper)`

Extracts the key from a Java object using a provided `RecordMapper`.

- **Parameters**:
    - `element` (`T`): The Java object.
    - `mapper` (`RecordMapper<T>`): The mapper whose `id()` method will be used to extract the key.
- **Returns**: `Key`

**Example**:
```java
User alice = new User("alice-123", "Alice", 30);
UserMapper userMapper = new UserMapper();

// The mapper's id(alice) method will be called to get "alice-123"
Key userKey = users.id(alice, userMapper);
```
> **Note**: This method is less common. `TypeSafeDataSet` provides a more integrated way to work with objects.

## Using `DataSet` with `Session`

The primary use of `DataSet` is to provide keys to `Session` methods.

### Single Record Operations

```java
// Create a key for user "alice"
Key aliceKey = users.id("alice");

// Use the key to perform a write operation
session.upsert(aliceKey)
    - .bin("email").setTo("alice@example.com")
    - .execute();

// Use the key to perform a read operation
RecordStream result = session.query(aliceKey).execute();
```

### Batch Operations

```java
// Create a list of keys
List<Key> keysToDeactivate = users.ids("bob", "charlie", "diana");

// Use the list of keys to perform a batch update
session.update(keysToDeactivate)
    .bin("active").setTo(false)
    .execute();
```

### Scans and Queries

A `DataSet` can also be passed directly to `session.query()` to indicate that you want to scan or query the entire set.

```java
// Scan the entire "users" set
RecordStream allUsers = session.query(users).execute();
```

## Complete Example: Repository Pattern

```java
import com.aerospike.DataSet;
import com.aerospike.Key;
import com.aerospike.Session;
import java.util.List;

public class UserRepository {
    private final Session session;
    private final DataSet users = DataSet.of("app", "users");
    
    public UserRepository(Session session) {
        this.session = session;
    }
    
    public void save(String userId, String email) {
        Key userKey = users.id(userId);
        session.upsert(userKey)
            .bin("email").setTo(email)
            .execute();
    }
    
    public void deactivateUsers(List<String> userIds) {
        List<Key> keys = users.ids(userIds.toArray());
        session.update(keys)
            .bin("active").setTo(false)
            .execute();
    }
}
```

## Thread Safety

`DataSet` objects are **immutable and thread-safe**. It is standard practice to create them once at startup and reuse them throughout your application.

## Related Classes

- **`TypeSafeDataSet`**: The typed equivalent for working with Java objects.
- **`Key`**: The object created by the `DataSet` factory methods.
- **`Session`**: The class that consumes the `Key` objects produced by `DataSet`.
