# `RecordStream`

A forward-only iterator for processing query results.

`com.aerospike.RecordStream`

## Overview

All query operations in the Fluent Client return a `RecordStream`. It is a powerful, lazily evaluated iterator that allows you to process results from the database without loading them all into memory at once. It implements `Iterator<KeyRecord>` and `Closeable`, so it can be used in a `for-each` loop or a `try-with-resources` block.

The `RecordStream` is designed to be flexible, supporting simple iteration, conversion to Java Streams, and object mapping.

## Processing Results

A `RecordStream` is forward-only. Once you consume a record, you cannot go back.

### Using an Iterator

The most basic way to process results is by using the `hasNext()` and `next()` methods.

```java
try (RecordStream results = session.query(users.ids("a", "b", "c")).execute()) {
    while (results.hasNext()) {
        KeyRecord keyRecord = results.next();
        System.out.println("Key: " + keyRecord.key.userKey);
        System.out.println("Record Bins: " + keyRecord.record.bins);
    }
} // results.close() is automatically called here
```

### Using `forEach`

For simple operations on each record, you can use the built-in `forEach` method.

```java
session.query(users.ids("a", "b", "c"))
    .execute()
    .forEach(keyRecord -> {
        System.out.println("Processing user: " + keyRecord.key.userKey);
    });
```

### Using a Java `Stream`

For more complex processing like filtering, mapping, and collecting, convert the `RecordStream` to a Java `Stream`.

```java
import java.util.stream.Collectors;

List<String> activeUserNames = session.query(users)
    .where("$.status == 'active'")
    .readingOnlyBins("name")
    .execute()
    .stream()
    .map(kr -> kr.record.getString("name"))
    .filter(name -> name != null && !name.isEmpty())
    .collect(Collectors.toList());
```

## Methods

### Iteration

- **`boolean hasNext()`**: Returns `true` if the stream has more records.
- **`KeyRecord next()`**: Returns the next `KeyRecord` in the iteration.
- **`void forEach(Consumer<KeyRecord> consumer)`**: Performs the given action for each remaining element.

### Result Conversion

- **`Stream<KeyRecord> stream()`**: Converts the `RecordStream` to a standard Java `Stream<KeyRecord>`.
- **`List<KeyRecord> toList()`**: Consumes all records from the stream and collects them into a `List`. **Warning**: This can cause an `OutOfMemoryError` if the result set is large.
- **`<T> List<T> toObjectList()`**: If a `TypeSafeDataSet<T>` was used for the query, this method consumes all records and maps them to a `List<T>`.
- **`<T> List<T> toObjectList(RecordMapper<T> mapper)`**: Consumes all records and maps them to a `List<T>` using the provided mapper.

### Single Record Retrieval

- **`Optional<KeyRecord> getFirst()`**: Gets the first record in the stream and then closes it. Returns `Optional.empty()` if the stream is empty.
- **`<T> Optional<T> getFirst(RecordMapper<T> mapper)`**: Gets the first record, maps it to an object, and closes the stream.

### Pagination

- **`boolean hasMorePages()`**: Returns `true` if there are more pages of results to be fetched.
- **`Optional<ResettablePagination> asResettablePagination()`**: Returns an interface for controlling pagination if the stream is pageable.

### Sorting

- **`Optional<Sortable> asSortable()`**: Returns an interface for sorting the result set if the stream is sortable.

## Closing a `RecordStream`

The `RecordStream` holds underlying database resources (like a network connection and server-side cursors). It is **critical** to close the stream when you are finished with it to release these resources.

The recommended way to ensure this is with a `try-with-resources` block.

**✅ Correct:**
```java
try (RecordStream results = session.query(users).execute()) {
    results.forEach(this::process);
} // Stream is automatically closed
```

If you convert to a Java `Stream`, the `onClose` handler is automatically attached, so closing the Java `Stream` will also close the `RecordStream`.

**✅ Correct:**
```java
try (Stream<KeyRecord> stream = session.query(users).execute().stream()) {
    long count = stream.count();
} // Both streams are closed
```

**❌ Incorrect (Resource Leak):**
```java
// This leaks a connection if not all records are consumed!
RecordStream results = session.query(users).limit(10).execute();
if (results.hasNext()) {
    KeyRecord first = results.next();
    // The stream is never closed if there are more than 1 record
}
```

## Related Classes
- **[`KeyRecord`](https://www.aerospike.com/apidocs/java/com/aerospike/client/query/KeyRecord.html)**: The object returned by the iterator, containing the `Key` and `Record`.
- **[`QueryBuilder`](./query-builder.md)**: The builder that produces a `RecordStream`.

## See Also
- **[Guide: Reading Records](../../guides/crud/reading-records.md#working-with-recordstream)**
