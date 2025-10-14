# `RecordMapper<T>`

An interface for converting between Aerospike records (maps) and Java objects (POJOs).

`com.aerospike.RecordMapper<T>`

## Overview

The `RecordMapper` interface is the core of the Fluent Client's object mapping functionality. You implement this interface for each of your domain objects (e.g., `Customer`, `Product`) to define the logic for serialization and deserialization.

An implementation of `RecordMapper` is responsible for three key tasks:
1.  **`fromMap`**: Converting a `Map<String, Object>` (representing an Aerospike record's bins) into an instance of your Java object.
2.  **`toMap`**: Converting an instance of your Java object into a `Map<String, Value>` to be stored as bins in an Aerospike record.
3.  **`id`**: Extracting the unique identifier from your Java object, which will be used as the Aerospike record's key.

## Interface Definition

```java
public interface RecordMapper<T> {
    T fromMap(Map<String, Object> map, Key recordKey, int generation);
    Map<String, Value> toMap(T element);
    Object id(T element);
}
```

## Methods

### `fromMap(Map<String, Object> map, Key recordKey, int generation)`

Deserializes a map of bin data into a Java object.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `map` | `Map<String, Object>` | The bins of the Aerospike record. |
| `recordKey` | `Key` | The key of the record, for context. |
| `generation` | `int` | The generation of the record. |

**Returns:** `T` - A new instance of your Java object populated with data from the map.

---

### `toMap(T element)`

Serializes a Java object into a map of bins to be stored in Aerospike.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `element` | `T` | The Java object instance to serialize. |

**Returns:** `Map<String, Value>` - A map where keys are bin names and values are `com.aerospike.client.Value` objects.

---

### `id(T element)`

Extracts the unique key value from a Java object instance.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `element` | `T` | The Java object instance. |

**Returns:** `Object` - The value to be used for the Aerospike key (e.g., a `String`, `long`, or `byte[]`).

## Complete Example

Let's assume you have a `Customer` POJO:
```java
public class Customer {
    private final long id;
    private final String name;
    private final int age;
    // Constructor, getters...
}
```

Here is a complete `RecordMapper` implementation for it:
```java
import com.aerospike.RecordMapper;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import java.util.HashMap;
import java.util.Map;

public class CustomerMapper implements RecordMapper<Customer> {

    @Override
    public Customer fromMap(Map<String, Object> map, Key recordKey, int generation) {
        return new Customer(
            (long) map.get("id"),
            (String) map.get("name"),
            ((Number) map.get("age")).intValue()
        );
    }

    @Override
    public Map<String, Value> toMap(Customer customer) {
        Map<String, Value> map = new HashMap<>();
        map.put("id", Value.get(customer.getId()));
        map.put("name", Value.get(customer.getName()));
        map.put("age", Value.get(customer.getAge()));
        return map;
    }

    @Override
    public Object id(Customer customer) {
        return customer.getId();
    }
}
```

## Related Classes

- **[`RecordMappingFactory`](./record-mapping-factory.md)**: A factory for providing `RecordMapper` instances.
- **[`TypeSafeDataSet`](../operations/typesafe-dataset.md)**: A `DataSet` that uses a `RecordMapper` for type-safe operations.

## See Also

- **[Guide: Object Mapping](../../concepts/object-mapping.md)**
- **[Guide: Creating Mappers](../../guides/object-mapping/creating-mappers.md)**
