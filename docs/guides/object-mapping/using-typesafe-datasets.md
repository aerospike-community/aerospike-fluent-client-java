# Using TypedDataSets

Learn how to perform type-safe, object-oriented database operations with `TypedDataSet`.

For the full typed query / stream / navigable / heterogeneous batch picture, see **[Typed query and mapping](./typed-query-and-mapping.md)**.

## Goal

By the end of this guide, you'll know how to:
- Create and use a `TypedDataSet` for your Java objects
- Perform type-safe CRUD (Create, Read, Update, Delete) operations
- Convert query results directly into lists of your objects
- Leverage your `RecordMapper` for seamless object persistence

## Prerequisites

- [Creating Mappers](./creating-mappers.md) completed
- A `RecordMapper` implementation for your POJO
- Your mapper registered with the `RecordMappingFactory`

---

## What is `TypedDataSet`?

`TypedDataSet` is a wrapper around `DataSet` that is strongly typed to your Java domain object. It acts as the primary entry point for all object-based database operations.

### Standard `DataSet` (Not Type-Safe)

```java
// Works with generic bins and values
DataSet users = DataSet.of("test", "users");
session.upsert(users.id("alice"))
    .bin("name").setTo("Alice")
    .bin("age").setTo(30)
    .execute();
```

### `TypedDataSet` (Type-Safe)

```java
// Works directly with your User objects
TypedDataSet<User> users = TypedDataSet.of("test", "users", User.class);

User alice = new User("alice", "Alice", 30);
session.insert(users)
    .object(alice)
    .execute();
```

---

## Getting Started

### 1. Define your POJO and Mapper

Ensure you have a `User` class and a `UserMapper` class, and that the mapper is registered.

```java
// From previous guide
public class User {
    private final String userId;
    // ...
}

public class UserMapper implements RecordMapper<User> {
    // ...
}

// In your application startup:
cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
    User.class, new UserMapper()
)));
```

### 2. Create a `TypedDataSet`

Instantiate it by providing the namespace, set name, and your POJO's `Class` object.

```java
TypedDataSet<User> users = TypedDataSet.of("test", "users", User.class);
```

---

## Type-Safe CRUD Operations

### Create (Insert/Upsert) an Object

Use `insert()` / `upsert()` with the `.object()` method. The client will automatically use your registered mapper to convert the object to Aerospike bins.

```java
User newUser = new User("bob-456", "Bob", 45);

// Create a new record from the object
session.insert(users)
    .object(newUser)
    .execute();
```

### Create Multiple Objects

You can persist a `List` of objects in a single batch operation.

```java
List<User> newUsers = List.of(
    new User("charlie-789", "Charlie", 28),
    new User("diana-012", "Diana", 52)
);

session.insert(users)
    .objects(newUsers)
    .execute();
```

### Read an object

Query by key and use `.toObjectList()` with **no arguments** so the client uses your registered `RecordMapper` and passes a `RecordReadContext` (session + type) into the mapper.

```java
// Find a single user by their ID (TypedRecordStream<User>)
Optional<User> user = session.query(users.id("bob-456"))
    .execute()
    .toObjectList()
    .stream()
    .findFirst();

user.ifPresent(u -> System.out.println("Found user: " + u.getName()));
```

You can still call `.toObjectList(userMapper)` when you need an explicit mapper instance.

### Read multiple objects

Use `queryTypedKeys` for a list of `TypedKey<User>` (same entity type), or pass multiple keys as varargs when convenient:

```java
List<User> userList = session.queryTypedKeys(users.ids("charlie-789", "diana-012"))
    .execute()
    .toObjectList();
```

### Update an Object

Use `update()` with the `.object()` method. This will replace all bins on the record with the fields from your object.

```java
User existingUser = new User("bob-456", "Robert", 46);

// This replaces the entire record for "bob-456"
session.update(users)
    .object(existingUser)
    .execute();
```

> **Warning**: This is a full replacement. If you want to update only specific fields, use the standard `update()` method with `.bin()` calls. See the [Updating Records](../crud/updating-records.md) guide.

### Delete an Object

You can get the key directly from the object using the `DataSet`.

```java
User userToDelete = new User("diana-012", "Diana", 52);

// The mapper's id() method is used to extract the key from the object
boolean deleted = session.delete(users.id(userToDelete)).execute();
```

---

## Type-safe queries

You can combine `TypedDataSet` with `where()` clauses and deserialize the results.

```java
// Find all users over 40
TypedRecordStream<User> results = session.query(users)
    .where(longBin("age").gt(40))
    .execute();

List<User> usersOver40 = results.toObjectList();

usersOver40.forEach(u -> System.out.println(u.getName()));
```

---

## Complete Example: `UserService`

This example shows a simple service layer that uses `TypedDataSet` to abstract away the database logic.

```java
import java.util.List;
import java.util.Optional;

public class UserService {
    private final Session session;
    private final TypedDataSet<User> users;
    public UserService(Cluster cluster, Session session) {
        this.session = session;
        this.users = TypedDataSet.of("app", "users", User.class);
        if (cluster.getRecordMappingFactory().getMapper(User.class) == null) {
            throw new IllegalStateException("UserMapper not registered!");
        }
    }
    
    public void save(User user) {
        session.upsert(users).object(user).execute();
    }
    
    public void saveAll(List<User> userList) {
        session.upsert(users).objects(userList).execute();
    }
    
    public Optional<User> findById(String userId) {
        return session.query(users.id(userId))
            .execute()
            .toObjectList()
            .stream()
            .findFirst();
    }
    
    public List<User> findByAge(int age) {
        return session.query(users)
            .where(longBin("age").eq(age))
            .execute()
            .toObjectList();
    }
    
    public boolean delete(User user) {
        return session.delete(users.id(user)).execute();
    }
}
```

---

## Best Practices

### ✅ DO

**Instantiate `TypedDataSet` once and reuse it.**
It's a lightweight, thread-safe object.

**Retrieve your mapper from the `RecordMappingFactory`.**
This ensures you're using the same instance that the client is configured with.

**Use `TypedDataSet` for all object-related operations.**
It improves readability and reduces the chance of errors.

### ❌ DON'T

**Don't forget to register the mapper.**
If you do, creating a `TypedDataSet` is fine, but any operation that relies on the mapper (like `.object()` or `.toObjectList()`) will fail.

**Don't mix `TypedDataSet<T>` with a `RecordMapper<U>` for a different type.**
This will lead to casting exceptions and runtime errors.

---

## Next Steps

You've now mastered object mapping!

- **[Typed query and mapping](./typed-query-and-mapping.md)** — streams, `RecordReadContext`, navigable typed streams, `mixedRead()`.
- **[Behavior Configuration (Java)](../configuration/behavior-java.md)** - Learn how to control policies like timeouts and retries.
- **[YAML Configuration](../configuration/yaml-configuration.md)** - Configure client behavior externally using YAML files.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
