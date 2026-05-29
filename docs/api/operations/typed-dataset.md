# `TypedDataSet<T>`

A `DataSet` that is strongly typed to a specific Java class, enabling compile-time safe object mapping operations.

`com.aerospike.TypedDataSet<T>`

## Overview

A `TypedDataSet<T>` extends the standard `DataSet` by associating it with a specific Java class (`<T>`). This allows the Fluent Client to provide enhanced type safety for operations involving object mapping.

When you use a `TypedDataSet`, you can pass your Plain Old Java Objects (POJOs) directly to the session methods (`insert`, `upsert`, `update`), and the client will automatically use the configured `RecordMapper` to convert the object to Aerospike bins. This eliminates the need for manual bin creation and reduces the risk of runtime type errors.

## Creating a `TypedDataSet`

You create a `TypedDataSet` using the static factory method `TypedDataSet.of()`, providing the namespace, set name, and the `.class` of your model.

```java
import com.aerospike.TypedDataSet;
import com.example.model.Customer; // Your POJO class

TypedDataSet<Customer> customerDataSet = TypedDataSet.of("test", "customers", Customer.class);
```

## Usage with object mapping

### Prerequisites

Before using `TypedDataSet`, you must have:

1. A POJO class (e.g., `Customer`).
2. A `RecordMapper<Customer>` implementation for that class.
3. The `RecordMapper` registered with the `Cluster`'s `RecordMappingFactory`.

**Example setup:**

```java
RecordMapper<Customer> customerMapper = new CustomerMapper();

cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
    Customer.class, customerMapper
)));
```

### Writing an object

Instead of setting bins manually, you can pass the entire object.

```java
Customer newCustomer = new Customer(1L, "John Doe", 30);

session.insert(customerDataSet)
    .object(newCustomer)
    .execute();
```

### Reading objects (typed query stream)

Queries against a `TypedDataSet` return a `TypedRecordStream<T>`. Use `toObjectList()` with no arguments so the client resolves the mapper from the `RecordMappingFactory` and passes a `RecordReadContext` (session + entity type) into `RecordMapper.fromMap(..., ctx)` for dependent loads.

```java
TypedRecordStream<Customer> stream = session.query(customerDataSet.id(1L)).execute();

List<Customer> customers = stream.toObjectList();

if (!customers.isEmpty()) {
    Customer retrievedCustomer = customers.get(0);
    System.out.println("Retrieved: " + retrievedCustomer.getName());
}
```

For secondary-index scans over the whole set, `session.query(customerDataSet)` also returns `TypedQueryBuilder<Customer>` and `.execute()` yields `TypedRecordStream<Customer>`.

You can still pass an explicit mapper with `toObjectList(RecordMapper<T> mapper)` when needed.

## Methods

### `getClazz()`

Returns the `Class<T>` object associated with this `TypedDataSet`.

**Returns:** `Class<T>` — the class of the generic type.

## Related classes

- **[`DataSet`](./dataset.md)**: The parent class.
- **[`RecordMapper`](../mapping/record-mapper.md)**: The interface used to map objects to records.
- **[`Session`](./session.md)**: Used to perform operations with the `TypedDataSet`.
- **[Typed query and mapping](../../guides/object-mapping/typed-query-and-mapping.md)**: API matrix, `TypedKey`, `queryTypedKeys`, navigable streams, heterogeneous batch reads.

## See also

- **[Object mapping](../../concepts/object-mapping.md)**
- **[Using TypedDataSets](../../guides/object-mapping/using-typesafe-datasets.md)** (CRUD-focused guide)
