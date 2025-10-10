# `TypeSafeDataSet<T>`

A `DataSet` that is strongly typed to a specific Java class, enabling compile-time safe object mapping operations.

`com.aerospike.TypeSafeDataSet<T>`

## Overview

A `TypeSafeDataSet<T>` extends the standard `DataSet` by associating it with a specific Java class (`<T>`). This allows the Fluent Client to provide enhanced type safety for operations involving object mapping.

When you use a `TypeSafeDataSet`, you can pass your Plain Old Java Objects (POJOs) directly to the session methods (`insert`, `upsert`, `update`), and the client will automatically use the configured `RecordMapper` to convert the object to Aerospike bins. This eliminates the need for manual bin creation and reduces the risk of runtime type errors.

## Creating a `TypeSafeDataSet`

You create a `TypeSafeDataSet` using the static factory method `TypeSafeDataSet.of()`, providing the namespace, set name, and the `.class` of your model.

```java
import com.aerospike.TypeSafeDataSet;
import com.example.model.Customer; // Your POJO class

// Create a TypeSafeDataSet for the "customers" set, typed to the Customer class
TypeSafeDataSet<Customer> customerDataSet = TypeSafeDataSet.of("test", "customers", Customer.class);
```

## Usage with Object Mapping

The primary use of `TypeSafeDataSet` is to perform type-safe CRUD operations with your Java objects.

### Prerequisites

Before using `TypeSafeDataSet`, you must have:
1.  A POJO class (e.g., `Customer`).
2.  A `RecordMapper<Customer>` implementation for that class.
3.  The `RecordMapper` registered with the `Cluster`'s `RecordMappingFactory`.

**Example Setup:**
```java
// Assume CustomerMapper is a class that implements RecordMapper<Customer>
RecordMapper<Customer> customerMapper = new CustomerMapper();

// Register the mapper with the cluster
cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
    Customer.class, customerMapper
)));
```

### Writing an Object

Instead of setting bins manually, you can pass the entire object.

```java
Customer newCustomer = new Customer(1L, "John Doe", 30);

// The client will use the registered CustomerMapper to convert the customer object to bins
session.insert(customerDataSet)
    .object(newCustomer)
    .execute();
```

### Reading an Object

When you query using a `TypeSafeDataSet`, you can easily convert the results back into a list of your objects.

```java
// Query by the customer's key
RecordStream results = session.query(customerDataSet.id(1L)).execute();

// Use the stream to get a list of Customer objects
// The client automatically finds the correct mapper for Customer.class
List<Customer> customers = results.toObjectList(); 

if (!customers.isEmpty()) {
    Customer retrievedCustomer = customers.get(0);
    System.out.println("Retrieved: " + retrievedCustomer.getName());
}
```

## Methods

### `getClazz()`

Returns the `Class<T>` object associated with this `TypeSafeDataSet`.

**Returns:** `Class<T>` - The class of the generic type.

## Related Classes

- **[`DataSet`](./dataset.md)**: The parent class.
- **[`RecordMapper`](../mapping/record-mapper.md)**: The interface used to map objects to records.
- **[`Session`](./session.md)**: Used to perform operations with the `TypeSafeDataSet`.

## See Also

- **[Guide: Object Mapping](../../concepts/object-mapping.md)**
- **[Guide: Using TypeSafeDataSets](../../guides/object-mapping/using-typesafe-datasets.md)**
