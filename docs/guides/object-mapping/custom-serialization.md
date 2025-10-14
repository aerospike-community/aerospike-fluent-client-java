# Custom Serialization & Advanced Object Mapping

Learn how to create custom mappers for complex Java objects and handle advanced serialization scenarios.

## Goal

By the end of this guide, you'll know how to:
- Implement a `RecordMapper` for a custom POJO
- Handle complex data types like nested objects, `enum`s, and `UUID`s
- Use a `RecordMappingFactory` to provide your mappers to the client
- Implement a versioning strategy for your objects to handle schema evolution

## Prerequisites

- Understanding of [Object Mapping](../object-mapping/creating-mappers.md)
- Familiarity with Java data types and serialization concepts

---

## The Need for Custom Serialization

The default object mapping is powerful, but sometimes you need more control over how your Java objects are stored in Aerospike.

**Common scenarios for custom serialization:**
- **Complex Data Types**: Your object contains fields that don't map directly to Aerospike's supported types (e.g., `UUID`, `URL`, custom value objects).
- **Nested Objects**: You want to store a child object as a blob (e.g., JSON string) in a single bin instead of a map.
- **Schema Evolution**: You need to handle different versions of an object to ensure backward and forward compatibility.
- **Performance Optimization**: You want to use a more compact serialization format (like MessagePack or Protobuf) instead of the default map representation.
- **Data Anonymization**: You need to encrypt or anonymize certain fields before they are written to the database.

---

## Implementing a Custom `RecordMapper`

The key to custom serialization is implementing the `RecordMapper<T>` interface for your specific class.

Let's consider a `User` object with a `UUID` and an `enum` for the status.

```java
public class User {
    private final UUID id;
    private final String name;
    private final UserStatus status;
    private final Instant createdAt;
    // constructor, getters...
}

public enum UserStatus {
    PENDING, ACTIVE, INACTIVE;
}
```

### 1. Create the Mapper Class

Create a `UserMapper` that implements `RecordMapper<User>`.

```java
import com.aerospike.RecordMapper;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

public class UserMapper implements RecordMapper<User> {
    // ... implementation from next steps ...
}
```

### 2. Implement `toMap()`

This method serializes your Java object into a `Map<String, Value>` that Aerospike can store. Here, we'll convert the `UUID` and `Instant` to strings and the `enum` to its name.

```java
@Override
public Map<String, Value> toMap(User user) {
    Map<String, Value> map = new HashMap<>();
    map.put("id", Value.get(user.getId().toString()));
    map.put("name", Value.get(user.getName()));
    map.put("status", Value.get(user.getStatus().name())); // Store enum as string
    map.put("createdAt", Value.get(user.getCreatedAt().toString())); // Store Instant as ISO-8601 string
    return map;
}
```

### 3. Implement `fromMap()`

This method deserializes the data from Aerospike back into your Java object.

```java
@Override
public User fromMap(Map<String, Object> map, Key recordKey, int generation) {
    UUID id = UUID.fromString((String) map.get("id"));
    String name = (String) map.get("name");
    UserStatus status = UserStatus.valueOf((String) map.get("status")); // Convert string back to enum
    Instant createdAt = Instant.parse((String) map.get("createdAt")); // Convert string back to Instant

    return new User(id, name, status, createdAt);
}
```

### 4. Implement `id()`

This method tells the client which field to use for the record's key.

```java
@Override
public Object id(User user) {
    return user.getId().toString(); // Use the UUID as the key
}
```

---

## Handling Schema Evolution

What happens when you need to change your object? For example, adding a `lastName` field. If you deploy new code, you need to be able to read old records that don't have this field.

You can handle this by adding a schema version to your record and handling it in your mapper.

### 1. Add Version to `toMap()`

```java
@Override
public Map<String, Value> toMap(User user) {
    Map<String, Value> map = new HashMap<>();
    map.put("schemaVersion", Value.get(2)); // Current version is 2
    map.put("id", Value.get(user.getId().toString()));
    map.put("firstName", Value.get(user.getFirstName()));
    map.put("lastName", Value.get(user.getLastName())); // New field
    // ... other fields
    return map;
}
```

### 2. Handle Versions in `fromMap()`

```java
@Override
public User fromMap(Map<String, Object> map, Key recordKey, int generation) {
    int version = ((Number) map.getOrDefault("schemaVersion", 1)).intValue();

    if (version == 1) {
        // Handle old version
        UUID id = UUID.fromString((String) map.get("id"));
        String fullName = (String) map.get("name");
        String[] names = fullName.split(" ", 2);
        String firstName = names[0];
        String lastName = (names.length > 1) ? names[1] : "";
        
        // ... map other old fields
        return new User(id, firstName, lastName, ...);

    } else if (version == 2) {
        // Handle current version
        UUID id = UUID.fromString((String) map.get("id"));
        String firstName = (String) map.get("firstName");
        String lastName = (String) map.get("lastName");
        
        // ... map other current fields
        return new User(id, firstName, lastName, ...);

    } else {
        throw new IllegalStateException("Unknown user schema version: " + version);
    }
}
```

This pattern ensures your application remains backward-compatible as your data model evolves.

---

## Using an Alternative Serialization Format (e.g., JSON)

If you prefer to store your object as a JSON string in a single bin, you can use a library like Jackson or Gson inside your mapper.

### Example with Jackson

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class UserJsonMapper implements RecordMapper<User> {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Override
    public Map<String, Value> toMap(User user) {
        try {
            String json = objectMapper.writeValueAsString(user);
            // Store the entire object as a JSON string in a single bin called "data"
            return Map.of("data", Value.get(json));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize user to JSON", e);
        }
    }

    @Override
    public User fromMap(Map<String, Object> map, Key recordKey, int generation) {
        try {
            String json = (String) map.get("data");
            return objectMapper.readValue(json, User.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize user from JSON", e);
        }
    }

    @Override
    public Object id(User user) {
        return user.getId().toString();
    }
}
```

**Pros of this approach:**
-   **Schema Flexibility**: Easier to add/remove fields without breaking deserialization (if the library is configured to ignore unknown properties).
-   **Portability**: The data is stored in a standard format that can be easily read by other applications.

**Cons:**
-   **Performance**: JSON serialization/deserialization can be slower than the default map approach.
-   **Bin Operations**: You cannot use Aerospike's atomic operations (like `add()`, `append()`) on individual fields within the JSON blob. The entire object must be read, modified, and written back.
-   **Secondary Indexes**: You cannot create a secondary index on a field inside the JSON blob.

---

## Registering Your Custom Mapper

Once you've created your `RecordMapper`, you need to tell the Fluent Client how to find it. This is done by providing a `RecordMappingFactory`.

### 1. Create a `DefaultRecordMappingFactory`

The `DefaultRecordMappingFactory` is a simple implementation that uses a `Map` to look up mappers by their class.

```java
import com.aerospike.DefaultRecordMappingFactory;
import com.aerospike.RecordMappingFactory;

// Create instances of your mappers
UserMapper userMapper = new UserMapper();
ProductMapper productMapper = new ProductMapper();

// Create the factory and register your mappers
RecordMappingFactory factory = DefaultRecordMappingFactory.of(
    User.class, userMapper,
    Product.class, productMapper
);
```

### 2. Set the Factory on the `Cluster`

You must set the factory on the `Cluster` instance *before* you create any sessions that will use it.

```java
ClusterDefinition definition = new ClusterDefinition("localhost", 3000);

try (Cluster cluster = definition.connect()) {
    
    // Set the factory on the cluster
    cluster.setRecordMappingFactory(factory);

    // Now, create your session
    Session session = cluster.createSession(Behavior.DEFAULT);

    // The session will now automatically use your custom mappers
    TypeSafeDataSet<User> users = TypeSafeDataSet.of("test", "users", User.class);
    
    User newUser = new User(...);
    session.upsert(users).object(newUser).execute(); // Uses your UserMapper

}
```

---

## Next Steps

- **[Creating Mappers](./creating-mappers.md)**: Review the basics of creating simple mappers.
- **[Using TypeSafeDataSets](./using-typesafe-datasets.md)**: See how `TypeSafeDataSet` leverages these mappers for compile-time safety.
