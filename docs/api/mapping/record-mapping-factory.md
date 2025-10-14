# `RecordMappingFactory`

An interface for a factory that provides `RecordMapper` instances.

`com.aerospike.RecordMappingFactory`

## Overview

The `RecordMappingFactory` is a simple factory interface used by the `Cluster` to look up the correct `RecordMapper` for a given Java class. Its single purpose is to centralize the management of your application's mappers.

You can create your own implementation of this interface, but for most use cases, the provided `DefaultRecordMappingFactory` is sufficient.

You register your factory with the `Cluster` instance, typically once at application startup.

## Interface Definition

```java
public interface RecordMappingFactory {
    <T> RecordMapper<T> getMapper(Class<T> clazz);
}
```

## Methods

### `getMapper(Class<T> clazz)`

Retrieves the `RecordMapper` for the specified class.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `clazz` | `Class<T>` | The class for which to find a mapper. |

**Returns:** `RecordMapper<T>` - The corresponding mapper instance, or `null` if no mapper is registered for the class.

## Example Implementation

While you can use `DefaultRecordMappingFactory`, here is how you might create a custom factory:

```java
import com.aerospike.RecordMapper;
import com.aerospike.RecordMappingFactory;
import java.util.HashMap;
import java.util.Map;

public class MyCustomMappingFactory implements RecordMappingFactory {
    private final Map<Class<?>, RecordMapper<?>> mappers = new HashMap<>();
    
    public MyCustomMappingFactory() {
        // Register mappers at startup
        mappers.put(Customer.class, new CustomerMapper());
        mappers.put(Product.class, new ProductMapper());
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> RecordMapper<T> getMapper(Class<T> clazz) {
        return (RecordMapper<T>) mappers.get(clazz);
    }
}

// Then, register it with the cluster:
// cluster.setRecordMappingFactory(new MyCustomMappingFactory());
```

## Related Classes

- **[`DefaultRecordMappingFactory`](./default-record-mapping-factory.md)**: The default, map-based implementation.
- **[`RecordMapper`](./record-mapper.md)**: The interface this factory produces instances of.
- **[`Cluster`](../connection/cluster.md)**: Where the factory is registered using `setRecordMappingFactory()`.

## See Also

- **[Guide: Object Mapping](../../concepts/object-mapping.md)**
