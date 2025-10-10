# Creating Mappers

Learn how to create `RecordMapper` implementations to map your Java objects (POJOs) to and from Aerospike records.

## Goal

By the end of this guide, you'll know how to:
- Implement the `RecordMapper` interface
- Handle serialization (`toMap`) and deserialization (`fromMap`)
- Specify the record's key from an object (`id`)
- Map various data types, including collections and dates
- Register your mappers with the `RecordMappingFactory`

## Prerequisites

- [Object Mapping Concepts](../../concepts/object-mapping.md)
- A Java domain object (POJO) you want to persist

---

## The `RecordMapper` Interface

The `RecordMapper` interface has three methods you need to implement:

```java
public interface RecordMapper<T> {
    // Deserialization: Aerospike Record -> Java Object
    T fromMap(Map<String, Object> map, Key recordKey, int generation);

    // Serialization: Java Object -> Aerospike Record
    Map<String, Value> toMap(T element);

    // Key Extraction: Get the Aerospike key from the Java Object
    Object id(T element);
}
```

---

## Step-by-Step: Creating a Simple Mapper

### 1. The Domain Object (POJO)

Let's use a simple `Product` class.

```java
public class Product {
    private final String productId; // This will be our Aerospike key
    private String name;
    private double price;
    private int stock;
    private boolean isActive;

    public Product(String productId, String name, double price, int stock, boolean isActive) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.isActive = isActive;
    }
    
    // Getters and setters...
}
```

### 2. Implementing `RecordMapper`

Create a `ProductMapper` class.

```java
import com.aerospike.RecordMapper;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import java.util.HashMap;
import java.util.Map;

public class ProductMapper implements RecordMapper<Product> {

    @Override
    public Object id(Product product) {
        // Return the field that should be used as the Aerospike key
        return product.getProductId();
    }

    @Override
    public Map<String, Value> toMap(Product product) {
        // Convert the Product object to a Map of bin names to Aerospike Values
        Map<String, Value> bins = new HashMap<>();
        
        bins.put("productId", Value.get(product.getProductId()));
        bins.put("name", Value.get(product.getName()));
        bins.put("price", Value.get(product.getPrice()));
        bins.put("stock", Value.get(product.getStock()));
        bins.put("isActive", Value.get(product.isActive()));
        
        return bins;
    }

    @Override
    public Product fromMap(Map<String, Object> bins, Key recordKey, int generation) {
        // Convert a Map of bins from Aerospike to a Product object
        return new Product(
            (String) bins.get("productId"),
            (String) bins.get("name"),
            ((Number) bins.get("price")).doubleValue(), // Handle numeric types carefully
            ((Number) bins.get("stock")).intValue(),
            (Boolean) bins.get("isActive")
        );
    }
}
```

### 3. Registering the Mapper

In your application's startup logic, register the mapper with the `Cluster` instance.

```java
ProductMapper productMapper = new ProductMapper();

cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
    Product.class, productMapper
)));
```

### 4. Using the Mapper

Now you can use `TypeSafeDataSet` to perform object-oriented operations.

```java
TypeSafeDataSet<Product> products = 
    TypeSafeDataSet.of("ecommerce", "products", Product.class);

// Create a new product object
Product newProduct = new Product("prod-123", "Laptop", 1299.99, 50, true);

// The client will use your ProductMapper automatically
session.insertInto(products)
    .object(newProduct)
    .execute();

// Reading the object back
List<Product> results = session.query(products.id("prod-123"))
    .execute()
    .toObjectList(productMapper); // Provide the mapper for deserialization
```

---

## Handling Complex Data Types

### Collections (Lists and Maps)

```java
public class User {
    private String userId;
    private List<String> roles;
    private Map<String, Object> preferences;
    // ...
}

public class UserMapper implements RecordMapper<User> {
    // ... id() method ...

    @Override
    public Map<String, Value> toMap(User user) {
        Map<String, Value> bins = new HashMap<>();
        bins.put("userId", Value.get(user.getUserId()));
        bins.put("roles", Value.get(user.getRoles())); // Lists are supported directly
        bins.put("preferences", Value.get(user.getPreferences())); // Maps are supported directly
        return bins;
    }

    @Override
    public User fromMap(Map<String, Object> bins, Key recordKey, int generation) {
        User user = new User();
        user.setUserId((String) bins.get("userId"));
        
        // Cast collections
        user.setRoles((List<String>) bins.get("roles"));
        user.setPreferences((Map<String, Object>) bins.get("preferences"));
        
        return user;
    }
}
```

### Dates and Times

Aerospike doesn't have a native date type, so you need to choose a representation.

**Recommended**: Store dates as either **epoch milliseconds (long)** or **ISO-8601 strings**.

```java
public class Event {
    private String eventId;
    private LocalDateTime eventTime; // Using java.time
    // ...
}

public class EventMapper implements RecordMapper<Event> {
    // ... id() method ...

    @Override
    public Map<String, Value> toMap(Event event) {
        Map<String, Value> bins = new HashMap<>();
        bins.put("eventId", Value.get(event.getEventId()));
        // Store as ISO-8601 String for readability
        bins.put("eventTime", Value.get(event.getEventTime().toString()));
        return bins;
    }

    @Override
    public Event fromMap(Map<String, Object> bins, Key recordKey, int generation) {
        Event event = new Event();
        event.setEventId((String) bins.get("eventId"));
        // Parse from ISO-8601 String
        event.setEventTime(LocalDateTime.parse((String) bins.get("eventTime")));
        return event;
    }
}
```

### Nested Objects

To map nested objects, you can either:
1.  Map the nested object to a `Map` within the parent mapper.
2.  Use a separate `RecordMapper` for the nested object (advanced).

**Example: Nested object as a Map**

```java
public class User {
    private String userId;
    private Address shippingAddress;
    // ...
}

public class Address {
    private String street;
    private String city;
    // ...
}

public class UserMapper implements RecordMapper<User> {
    // ... id() method ...

    @Override
    public Map<String, Value> toMap(User user) {
        Map<String, Value> bins = new HashMap<>();
        bins.put("userId", Value.get(user.getUserId()));
        
        // Convert Address object to a Map
        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("street", user.getShippingAddress().getStreet());
        addressMap.put("city", user.getShippingAddress().getCity());
        bins.put("shippingAddress", Value.get(addressMap));
        
        return bins;
    }

    @Override
    public User fromMap(Map<String, Object> bins, Key recordKey, int generation) {
        User user = new User();
        user.setUserId((String) bins.get("userId"));
        
        // Convert Map back to Address object
        Map<String, Object> addressMap = (Map<String, Object>) bins.get("shippingAddress");
        Address address = new Address();
        address.setStreet((String) addressMap.get("street"));
        address.setCity((String) addressMap.get("city"));
        user.setShippingAddress(address);
        
        return user;
    }
}
```

---

## Best Practices

### ✅ DO

**Handle `null` values gracefully**
In `fromMap`, a bin might not exist. Use `getOrDefault` or check for `null`.

```java
// Safe way to get a potentially missing value
String name = (String) bins.getOrDefault("name", "Default Name");
```

**Be mindful of numeric types**
Aerospike stores all integers as `long` and all floats as `double`. Your `fromMap` implementation must handle this.

```java
// ✅ Safe: Cast to Number first, then get the specific type
int stock = ((Number) bins.get("stock")).intValue();
double price = ((Number) bins.get("price")).doubleValue();
```

**Keep Mappers Stateless and Thread-Safe**
Mappers are often singletons. Do not store any state within them.

**Consider a Base Mapper**
If you have common fields like `createdAt` or `updatedAt`, create a base mapper class to handle them.

### ❌ DON'T

**Don't put business logic in your mappers**
Mappers should only be responsible for data conversion.

**Don't forget to register your mappers**
If you forget, the `DefaultRecordMappingFactory` will throw an exception when you try to use the corresponding `TypeSafeDataSet`.

---

## Next Steps

- **[Using TypeSafeDataSets](./typesafe-datasets.md)** - Learn how to use your mappers with type-safe operations.
- **[Object Mapping Concepts](../../concepts/object-mapping.md)** - Revisit the core concepts.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
