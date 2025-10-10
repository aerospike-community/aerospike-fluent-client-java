# `QueryBuilder`

The fluent builder for constructing read, scan, and query operations.

`com.aerospike.QueryBuilder`

## Overview

`QueryBuilder` is the primary tool for defining the details of a read operation. You obtain an instance of this builder by calling a `query()` method on a `Session`.

The builder uses a fluent API to chain methods for filtering, selecting bins, and controlling the output. The query is not sent to the cluster until a **terminal method** like `execute()` is called.

## Obtaining a `QueryBuilder`

```java
// For a single record (point read)
QueryBuilder builder = session.query(users.id("alice"));

// For multiple records (batch read)
QueryBuilder batchBuilder = session.query(users.ids("alice", "bob"));

// For a full set (scan or secondary index query)
QueryBuilder queryBuilder = session.query(users);
```

## Core Method Categories

### 1. Filtering

These methods define the criteria for selecting records.

- `where(String)`: Filters records using a string-based expression.
- `where(BooleanExpression)`: Filters records using a type-safe DSL expression.

### 2. Bin Selection (Projection)

These methods control which bins are returned in the result.

- `readingOnlyBins(String...)`: Specifies a list of bins to return.
- `withNoBins()`: Returns only record metadata (generation, TTL), no bin data.

### 3. Result Control

These methods control the size and order of the result set.

- `limit(long)`: Restricts the maximum number of records returned.
- `pageSize(int)`: For paginated queries.
- `sortReturnedSubsetBy(String, ...)`: Sorts the results (requires Aerospike Server 6.0+).

### 4. Terminal Methods

These methods trigger the execution of the query.

- `execute()`: Executes the query and returns a `RecordStream`.

---

## Key Methods

### `where(BooleanExpression)`

Filters records using a type-safe DSL expression. This is the recommended way to filter.

- **Parameters**:
    - `filter` (`BooleanExpression`): The DSL expression.

```java
import static com.aerospike.dsl.Dsl.*;

session.query(users)
    .where(
        and(
            longBin("age").gte(21),
            stringBin("country").eq("USA")
        )
    )
    .execute();
```

### `readingOnlyBins(String... binNames)`

Specifies that only the listed bins should be returned. This is highly recommended for performance as it reduces network traffic.

- **Parameters**:
    - `binNames` (`String...`): A varargs array of bin names.

```java
// Only fetch the name and email for each user
session.query(users)
    .where(...)
    .readingOnlyBins("name", "email")
    .execute();
```

### `withNoBins()`

Specifies that no bin data should be returned. This is useful for checking for the existence of records or reading their metadata.

```java
// Check if a user exists
boolean exists = session.query(users.id("alice"))
    .withNoBins()
    .execute()
    .hasNext();
```

### `limit(long maxRecords)`

Restricts the total number of records that will be returned by the query or scan.

- **Parameters**:
    - `maxRecords` (`long`): The maximum number of records to return.

```java
// Get a sample of 10 users
RecordStream sample = session.query(users)
    .limit(10)
    .execute();
```

### `execute()`

The terminal method that executes the defined query.

- **Returns**: `RecordStream` - A forward-only iterator for the results.

```java
RecordStream results = session.query(users)
    .where(longBin("age").gt(30))
    .execute();

results.forEach(record -> {
    // process each record
});
```

---

## Complete Example: Paginated User Search

This example demonstrates chaining multiple methods to create a complex query.

```java
public List<User> findActiveUsers(String country, int pageNumber, int pageSize) {
    UserMapper userMapper = ...;
    
    // Define the filter
    BooleanExpression filter = and(
        booleanBin("isActive").isTrue(),
        stringBin("country").eq(country)
    );
    
    // Build and execute the query
    RecordStream results = session.query(users)
        .where(filter)
        .readingOnlyBins("name", "email", "age")
        .sortReturnedSubsetBy("age", SortDir.SORT_DESC) // Requires server 6.0+
        .pageSize(pageSize)
        .limit(pageSize * pageNumber) // Simplified pagination limit
        .execute();
    
    // Skip to the desired page
    results.asResettablePagination().ifPresent(p -> p.setPageTo(pageNumber));
    
    // Convert results to objects
    return results.toObjectList(userMapper);
}
```

## Thread Safety

`QueryBuilder` is **not thread-safe**. It is a stateful builder that should be created, configured, and used within a single method scope. Do not share `QueryBuilder` instances across threads.

## Related Classes

- **`Session`**: The factory for `QueryBuilder` instances.
- **`RecordStream`**: The result of a `execute()` call.
- **`BooleanExpression`**: The DSL expression object used in `where()`.
