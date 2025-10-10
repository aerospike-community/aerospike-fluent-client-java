# `DefaultRecordMappingFactory`

The default, map-based implementation of `RecordMappingFactory`.

`com.aerospike.DefaultRecordMappingFactory`

## Overview

`DefaultRecordMappingFactory` is the standard implementation of `RecordMappingFactory` provided by the Fluent Client. It uses an internal `Map` to store and look up `RecordMapper` instances based on their corresponding Java class.

This is the most common way to configure object mapping in your application. You create an instance of this class, pre-populated with your mappers, and register it with the `Cluster`.

## Creating a `DefaultRecordMappingFactory`

You can create an instance using its constructor or the convenient `of()` static factory methods.

### Using the Constructor

The constructor accepts a `Map<Class<?>, RecordMapper<?>>`.

```java
import com.aerospike.DefaultRecordMappingFactory;
import java.util.Map;

// Assume CustomerMapper and ProductMapper are defined
RecordMapper<Customer> customerMapper = new CustomerMapper();
RecordMapper<Product> productMapper = new ProductMapper();

// Create a map of classes to their mappers
Map<Class<?>, RecordMapper<?>> mappers = Map.of(
    Customer.class, customerMapper,
    Product.class, productMapper
);

// Create the factory
RecordMappingFactory factory = new DefaultRecordMappingFactory(mappers);

// Register with the cluster
cluster.setRecordMappingFactory(factory);
```

### Using Static `of()` Methods

For convenience, you can use the static `of()` methods for up to four mappers.

```java
// For a single mapper
RecordMappingFactory factory = DefaultRecordMappingFactory.of(Customer.class, new CustomerMapper());

// For two mappers
RecordMappingFactory factory2 = DefaultRecordMappingFactory.of(
    Customer.class, new CustomerMapper(),
    Product.class, new ProductMapper()
);

cluster.setRecordMappingFactory(factory2);
```

## Methods

This class implements the `getMapper(Class<T> clazz)` method from the `RecordMappingFactory` interface, looking up the mapper from its internal map.

## Complete Usage Example

```java
// 1. Define your POJO and Mapper
public class User {
    // ...
}
public class UserMapper implements RecordMapper<User> {
    // ...
}

// 2. Create and register the factory at application startup
try (Cluster cluster = new ClusterDefinition("localhost", 3000).connect()) {
    
    RecordMappingFactory mappingFactory = DefaultRecordMappingFactory.of(
        User.class, new UserMapper()
    );
    cluster.setRecordMappingFactory(mappingFactory);

    // 3. The factory is now ready to be used by TypeSafeDataSet operations
    Session session = cluster.createSession(Behavior.DEFAULT);
    TypeSafeDataSet<User> userDataSet = TypeSafeDataSet.of("test", "users", User.class);

    // This operation will now automatically find and use UserMapper
    session.insert(userDataSet).object(new User(...)).execute();
}
```

## Related Classes

- **[`RecordMappingFactory`](./record-mapping-factory.md)**: The interface this class implements.
- **[`RecordMapper`](./record-mapper.md)**: The objects stored and managed by this factory.
- **[`Cluster`](../connection/cluster.md)**: Where an instance of this factory is registered.

## See Also

- **[Guide: Object Mapping](../../concepts/object-mapping.md)**
- **[Guide: Creating Mappers](../../guides/object-mapping/creating-mappers.md)**
