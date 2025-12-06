# Object Mapping

Learn how to work with Java objects (POJOs) instead of raw bins using the Fluent Client's object mapping framework.

## Overview

Object mapping lets you:
- **Work with POJOs** instead of bins and values
- **Automate serialization** to/from Aerospike records
- **Type-safe operations** with compile-time checking
- **Clean separation** between domain and persistence

```
Java Object (Customer)
        ↓ RecordMapper
Aerospike Record (bins)
        ↓ Database
Storage
```

## Basic Concepts

### Without Object Mapping

```java
// Manual bin management
session.upsert(users.id("alice"))
    .bin("id").setTo("alice")
    .bin("name").setTo("Alice Johnson")
    .bin("age").setTo(30)
    .bin("email").setTo("alice@example.com")
    .execute();

// Manual deserialization
RecordStream result = session.query(users.id("alice")).execute();
if (result.hasNext()) {
    RecordResult kr = result.next();
    String id = (String) kr.key().userKey.getObject();
    String name = kr.recordOrThrow().getString("name");
    int age = kr.recordOrThrow().getInt("age");
    String email = kr.recordOrThrow().getString("email");
    
    Customer customer = new Customer(id, name, age, email);
}
```

### With Object Mapping

```java
// Automatic serialization
Customer customer = new Customer("alice", "Alice Johnson", 30, "alice@example.com");
session.upsert(customers).object(customer).execute();

// Automatic deserialization
List<Customer> results = session.query(customers)
    .execute()
    .toObjectList(customerMapper);
```

## Core Components

### 1. Domain Object (POJO)

Your business object:

```java
public class Customer {
    private String id;
    private String name;
    private int age;
    private String email;
    
    public Customer(String id, String name, int age, String email) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.email = email;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
    
    // Setters (optional)
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setEmail(String email) { this.email = email; }
}
```

### 2. RecordMapper

Defines serialization/deserialization logic:

```java
import com.aerospike.RecordMapper;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import java.util.Map;
import java.util.HashMap;

public class CustomerMapper implements RecordMapper<Customer> {
    
    @Override
    public Customer fromMap(Map<String, Object> map, Key recordKey, int generation) {
        return new Customer(
            (String) map.get("id"),
            (String) map.get("name"),
            (Integer) map.get("age"),
            (String) map.get("email")
        );
    }
    
    @Override
    public Map<String, Value> toMap(Customer customer) {
        Map<String, Value> map = new HashMap<>();
        map.put("id", Value.get(customer.getId()));
        map.put("name", Value.get(customer.getName()));
        map.put("age", Value.get(customer.getAge()));
        map.put("email", Value.get(customer.getEmail()));
        return map;
    }
    
    @Override
    public Object id(Customer customer) {
        return customer.getId();
    }
}
```

### 3. RecordMappingFactory

Manages mappers for different types:

```java
import com.aerospike.DefaultRecordMappingFactory;
import java.util.Map;

// Register mappers
cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
    Customer.class, new CustomerMapper(),
    Address.class, new AddressMapper(),
    Order.class, new OrderMapper()
)));
```

### 4. TypeSafeDataSet

Type-safe dataset bound to a class:

```java
TypeSafeDataSet<Customer> customers = 
    TypeSafeDataSet.of("app", "customers", Customer.class);
```

## Complete Setup Example

### Step 1: Define Domain Object

```java
public class User {
    private final Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    private boolean active;
    
    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }
    
    // Getters and setters...
}
```

### Step 2: Create Mapper

```java
public class UserMapper implements RecordMapper<User> {
    
    @Override
    public User fromMap(Map<String, Object> map, Key recordKey, int generation) {
        User user = new User(
            (Long) map.get("id"),
            (String) map.get("name"),
            (String) map.get("email")
        );
        
        // Handle optional fields
        if (map.containsKey("createdAt")) {
            String timestamp = (String) map.get("createdAt");
            user.setCreatedAt(LocalDateTime.parse(timestamp));
        }
        
        if (map.containsKey("active")) {
            user.setActive((Boolean) map.get("active"));
        }
        
        return user;
    }
    
    @Override
    public Map<String, Value> toMap(User user) {
        Map<String, Value> map = new HashMap<>();
        map.put("id", Value.get(user.getId()));
        map.put("name", Value.get(user.getName()));
        map.put("email", Value.get(user.getEmail()));
        map.put("createdAt", Value.get(user.getCreatedAt().toString()));
        map.put("active", Value.get(user.isActive()));
        return map;
    }
    
    @Override
    public Object id(User user) {
        return user.getId();
    }
}
```

### Step 3: Register Mapper

```java
// During application initialization
cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
    User.class, new UserMapper()
)));
```

### Step 4: Use Object Operations

```java
// Create TypeSafeDataSet
TypeSafeDataSet<User> users = 
    TypeSafeDataSet.of("app", "users", User.class);

// Write object
User alice = new User(1L, "Alice Johnson", "alice@example.com");
session.upsert(users).object(alice).execute();

// Read objects
UserMapper mapper = new UserMapper();
List<User> results = session.query(users)
    .where("$.active == true")
    .execute()
    .toObjectList(mapper);

for (User user : results) {
    System.out.println(user.getName());
}
```

## Advanced Mapping

### Handling Complex Types

```java
public class Order {
    private String orderId;
    private List<OrderItem> items;
    private Map<String, String> metadata;
    private BigDecimal total;
}

public class OrderMapper implements RecordMapper<Order> {
    
    @Override
    public Order fromMap(Map<String, Object> map, Key recordKey, int generation) {
        Order order = new Order();
        order.setOrderId((String) map.get("orderId"));
        
        // Deserialize list
        List<Map<String, Object>> itemMaps = 
            (List<Map<String, Object>>) map.get("items");
        List<OrderItem> items = itemMaps.stream()
            .map(this::mapToOrderItem)
            .collect(Collectors.toList());
        order.setItems(items);
        
        // Deserialize map
        Map<String, String> metadata = 
            (Map<String, String>) map.get("metadata");
        order.setMetadata(metadata);
        
        // Deserialize BigDecimal
        double totalValue = (Double) map.get("total");
        order.setTotal(BigDecimal.valueOf(totalValue));
        
        return order;
    }
    
    @Override
    public Map<String, Value> toMap(Order order) {
        Map<String, Value> map = new HashMap<>();
        map.put("orderId", Value.get(order.getOrderId()));
        
        // Serialize list
        List<Map<String, Object>> itemMaps = order.getItems().stream()
            .map(this::orderItemToMap)
            .collect(Collectors.toList());
        map.put("items", Value.get(itemMaps));
        
        // Serialize map
        map.put("metadata", Value.get(order.getMetadata()));
        
        // Serialize BigDecimal
        map.put("total", Value.get(order.getTotal().doubleValue()));
        
        return map;
    }
    
    private OrderItem mapToOrderItem(Map<String, Object> map) {
        // Implementation...
    }
    
    private Map<String, Object> orderItemToMap(OrderItem item) {
        // Implementation...
    }
    
    @Override
    public Object id(Order order) {
        return order.getOrderId();
    }
}
```

### Handling Dates and Times

```java
public class EventMapper implements RecordMapper<Event> {
    
    @Override
    public Event fromMap(Map<String, Object> map, Key recordKey, int generation) {
        Event event = new Event();
        event.setEventId((String) map.get("eventId"));
        
        // Parse ISO-8601 timestamp
        String timestampStr = (String) map.get("timestamp");
        event.setTimestamp(LocalDateTime.parse(timestampStr));
        
        // Parse epoch milliseconds
        Long epochMillis = (Long) map.get("createdAt");
        event.setCreatedAt(Instant.ofEpochMilli(epochMillis));
        
        return event;
    }
    
    @Override
    public Map<String, Value> toMap(Event event) {
        Map<String, Value> map = new HashMap<>();
        map.put("eventId", Value.get(event.getEventId()));
        
        // Store as ISO-8601 string
        map.put("timestamp", Value.get(event.getTimestamp().toString()));
        
        // Store as epoch milliseconds
        map.put("createdAt", Value.get(event.getCreatedAt().toEpochMilli()));
        
        return map;
    }
    
    @Override
    public Object id(Event event) {
        return event.getEventId();
    }
}
```

### Versioning and Migration

```java
public class UserMapperV2 implements RecordMapper<User> {
    
    @Override
    public User fromMap(Map<String, Object> map, Key recordKey, int generation) {
        // Check version field
        Integer version = (Integer) map.getOrDefault("_version", 1);
        
        if (version == 1) {
            return fromMapV1(map);
        } else if (version == 2) {
            return fromMapV2(map);
        }
        
        throw new IllegalStateException("Unknown version: " + version);
    }
    
    private User fromMapV1(Map<String, Object> map) {
        // Old schema: single "name" field
        User user = new User();
        user.setName((String) map.get("name"));
        return user;
    }
    
    private User fromMapV2(Map<String, Object> map) {
        // New schema: "firstName" and "lastName"
        User user = new User();
        user.setFirstName((String) map.get("firstName"));
        user.setLastName((String) map.get("lastName"));
        return user;
    }
    
    @Override
    public Map<String, Value> toMap(User user) {
        Map<String, Value> map = new HashMap<>();
        map.put("_version", Value.get(2));  // Always write latest version
        map.put("firstName", Value.get(user.getFirstName()));
        map.put("lastName", Value.get(user.getLastName()));
        return map;
    }
    
    @Override
    public Object id(User user) {
        return user.getId();
    }
}
```

## Common Patterns

### Pattern 1: Repository Pattern

```java
public class CustomerRepository {
    private final Session session;
    private final TypeSafeDataSet<Customer> customers;
    private final CustomerMapper mapper;
    
    public CustomerRepository(Session session) {
        this.session = session;
        this.customers = TypeSafeDataSet.of("app", "customers", Customer.class);
        this.mapper = new CustomerMapper();
    }
    
    public void save(Customer customer) {
        session.upsert(customers)
            .object(customer)
            .execute();
    }
    
    public Optional<Customer> findById(String id) {
        List<Customer> results = session.query(customers.id(id))
            .execute()
            .toObjectList(mapper);
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public List<Customer> findByAge(int minAge) {
        return session.query(customers)
            .where("$.age >= " + minAge)
            .execute()
            .toObjectList(mapper);
    }
    
    public void delete(String id) {
        session.delete(customers.id(id)).execute();
    }
}
```

### Pattern 2: Generic Repository

```java
public abstract class GenericRepository<T> {
    protected final Session session;
    protected final TypeSafeDataSet<T> dataSet;
    protected final RecordMapper<T> mapper;
    
    public GenericRepository(Session session, String namespace, String set, 
                           Class<T> clazz, RecordMapper<T> mapper) {
        this.session = session;
        this.dataSet = TypeSafeDataSet.of(namespace, set, clazz);
        this.mapper = mapper;
    }
    
    public void save(T entity) {
        session.upsert(dataSet).object(entity).execute();
    }
    
    public Optional<T> findById(Object id) {
        List<T> results = session.query(dataSet.idForObject(id))
            .execute()
            .toObjectList(mapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public List<T> findAll() {
        return session.query(dataSet)
            .execute()
            .toObjectList(mapper);
    }
    
    public void deleteById(Object id) {
        session.delete(dataSet.idForObject(id)).execute();
    }
}

// Usage
public class UserRepository extends GenericRepository<User> {
    public UserRepository(Session session) {
        super(session, "app", "users", User.class, new UserMapper());
    }
    
    // Add custom methods
    public List<User> findActiveUsers() {
        return session.query(dataSet)
            .where("$.active == true")
            .execute()
            .toObjectList(mapper);
    }
}
```

## Best Practices

### ✅ DO

**Use immutable objects when possible**
```java
public record Customer(String id, String name, int age) {
    // Immutable by default
}
```

**Handle null values carefully**
```java
@Override
public User fromMap(Map<String, Object> map, Key recordKey, int generation) {
    return new User(
        (String) map.get("id"),
        (String) map.getOrDefault("name", "Unknown"),  // Default for null
        (Integer) map.getOrDefault("age", 0)
    );
}
```

**Version your schema**
```java
map.put("_version", Value.get(2));
map.put("_schemaDate", Value.get(LocalDate.now().toString()));
```

### ❌ DON'T

**Don't store sensitive data unencrypted**
```java
// ❌ Bad: Plain text password
map.put("password", Value.get(user.getPassword()));

// ✅ Good: Hash before storing
map.put("passwordHash", Value.get(hashPassword(user.getPassword())));
```

**Don't ignore mapper errors**
```java
// ❌ Bad: Silent failure
try {
    return fromMap(map, key, gen);
} catch (Exception e) {
    return null;  // Swallows error
}

// ✅ Good: Propagate or handle explicitly
try {
    return fromMap(map, key, gen);
} catch (Exception e) {
    throw new MappingException("Failed to map record", e);
}
```

## API Reference

For complete documentation:
- [RecordMapper API](../api/mapping/record-mapper.md)
- [TypeSafeDataSet API](../api/operations/typesafe-dataset.md)
- [RecordMappingFactory API](../api/mapping/record-mapping-factory.md)

## Next Steps

- **[Creating Mappers Guide](../guides/object-mapping/creating-mappers.md)** - Detailed mapping examples
- **[Type-Safe Operations](./type-safe-operations.md)** - Use with type-safe operations
- **[CRUD Operations](../guides/crud/creating-records.md)** - Apply object mapping

---

**Questions?** Check the [FAQ](../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
