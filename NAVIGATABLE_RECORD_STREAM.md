# NavigatableRecordStream - Implementation Summary

## Overview

`NavigatableRecordStream` is a new class that provides in-memory sorting and pagination capabilities for any `RecordStream`. It loads records into memory and allows dynamic re-sorting and flexible pagination without requiring additional database queries.

## Key Features

### 1. **In-Memory Data Management**
- Loads all records from a `RecordStream` into memory
- Optional limit parameter to control memory usage
- Records stored in an array for fast access and sorting

### 2. **Multi-Column Sorting**
- Sort by multiple fields with different directions (ascending/descending)
- Case-sensitive and case-insensitive sorting options
- Dynamic re-sorting without re-querying the database
- Two sorting APIs:
  - **Builder-style** (cumulative): Chain `sortBy()` calls to add sort criteria
  - **Sortable interface** (replacement): Replace entire sort with single call

### 3. **Flexible Pagination**
- Configurable page size
- Forward pagination with `hasMorePages()`
- Jump to specific pages with `setPageTo()`
- Reset iteration with `reset()`
- Navigate backward by jumping to earlier pages

### 4. **Integration with Existing APIs**
- Implements `Sortable` interface
- Implements `ResettablePagination` interface
- Implements `Closeable` interface
- Provides familiar methods like `toObjectList()`, `stream()`, `forEach()`

## Files Created/Modified

### New Files

1. **`src/main/java/com/aerospike/NavigatableRecordStream.java`** (464 lines)
   - Main implementation class
   - Comprehensive JavaDoc documentation
   - Builder-style API for sorting and pagination

2. **`src/main/java/com/example/NavigatableRecordStreamExample.java`** (477 lines)
   - 6 comprehensive examples demonstrating all features
   - Extensive inline documentation
   - Ready-to-run demonstration code

### Modified Files

1. **`src/main/java/com/aerospike/RecordStream.java`**
   - Added `asNavigatableStream()` method
   - Added `asNavigatableStream(long limit)` method
   - Complete JavaDoc documentation with examples

## API Reference

### Creating a NavigatableRecordStream

```java
// From any RecordStream
RecordStream results = session.query(dataSet).execute();

// Load all records
NavigatableRecordStream nav = results.asNavigatableStream();

// Load with limit (recommended for large datasets)
NavigatableRecordStream nav = results.asNavigatableStream(1000);
```

### Sorting

#### Builder-Style API (Cumulative)

```java
// Add multiple sort criteria
nav.sortBy("name")                              // Primary sort
   .sortBy("age", SortDir.SORT_DESC);          // Secondary sort

// Replace sort criteria
nav.clearSort().sortBy("age");                 // Only sort by age

// Full control
nav.sortBy("name", SortDir.SORT_ASC, false);  // Case-insensitive
```

#### Sortable Interface (Replacement)

```java
// Replace with single criterion
nav.sortBy(new SortProperties("age", SortDir.SORT_DESC, true));

// Replace with multiple criteria
nav.sortBy(List.of(
    new SortProperties("name", SortDir.SORT_ASC, true),
    new SortProperties("age", SortDir.SORT_DESC, true)
));
```

### Pagination

```java
// Set page size
nav.pageSize(20);

// Forward pagination
while (nav.hasMorePages()) {
    while (nav.hasNext()) {
        RecordResult record = nav.next();
        // Process record
    }
}

// Jump to specific page
nav.setPageTo(3);  // Jump to page 3

// Get current page info
int current = nav.currentPage();  // 1-based
int total = nav.maxPages();

// Reset to beginning
nav.reset();
```

### Data Access

```java
// Convert current page to objects
List<Customer> customers = nav.toObjectList(mapper);

// Stream current page
Stream<RecordResult> stream = nav.stream();

// Iterate current page
nav.forEach(record -> { /* process */ });

// Get first record
Optional<RecordResult> first = nav.getFirst();

// Get total record count
int size = nav.size();
```

## Sorting Behavior Details

### Important: Cumulative vs Replacement

The builder-style `sortBy(String, ...)` methods are **cumulative**, meaning each call adds a new sort criterion. This design matches `QueryBuilder` and allows natural multi-column sorting:

```java
nav.sortBy("lastName", SortDir.SORT_ASC)
   .sortBy("firstName", SortDir.SORT_ASC)
   .sortBy("age", SortDir.SORT_DESC);
```

To replace the sort criteria entirely, use one of these approaches:

**Option 1: Clear first**
```java
nav.clearSort().sortBy("age");
```

**Option 2: Use Sortable interface**
```java
nav.sortBy(new SortProperties("age", SortDir.SORT_DESC, true));
```

**Option 3: Use Sortable interface with list**
```java
nav.sortBy(List.of(new SortProperties("age", SortDir.SORT_DESC, true)));
```

### Sort Order Precedence

Sorts are applied in the order they are added:
1. First `sortBy()` call = primary sort
2. Second `sortBy()` call = secondary sort
3. Third `sortBy()` call = tertiary sort
4. And so on...

## Example Scenarios

### Scenario 1: User-Driven Sort

```java
// Initial query
RecordStream results = session.query(customerDataSet).limit(500).execute();
NavigatableRecordStream nav = results.asNavigatableStream()
    .pageSize(25)
    .sortBy("name");

// User changes sort order (no database query!)
nav.clearSort().sortBy("age", SortDir.SORT_DESC);
nav.reset();  // Start from beginning with new sort
```

### Scenario 2: Compare Multiple Sort Orders

```java
RecordStream results = session.query(customerDataSet).execute();
NavigatableRecordStream nav = results.asNavigatableStream().pageSize(20);

// View sorted by name
nav.sortBy("name");
processFirstPage(nav);

// Compare with sort by age (no re-query!)
nav.clearSort().sortBy("age", SortDir.SORT_DESC);
nav.reset();
processFirstPage(nav);

// Compare with sort by name then age
nav.clearSort()
   .sortBy("name")
   .sortBy("age", SortDir.SORT_DESC);
nav.reset();
processFirstPage(nav);
```

### Scenario 3: Large Dataset with Memory Control

```java
// Query might return millions of records, but only load top 1000
RecordStream results = session.query(dataSet)
    .where("$.active == true")
    .execute();

NavigatableRecordStream nav = results.asNavigatableStream(1000)  // Limit memory
    .pageSize(50)
    .sortBy("lastModified", SortDir.SORT_DESC);

// Process only recent records with controlled memory usage
while (nav.hasMorePages()) {
    processPage(nav);
}
```

## Performance Considerations

### Memory Usage

- All records are stored in memory as `RecordResult[]` array
- Memory usage = (record count) × (record size)
- **Recommendation**: Use `asNavigatableStream(limit)` for large datasets
- **Best practice**: Set a reasonable limit (e.g., 1000-10000 records)

### When to Use NavigatableRecordStream

**Use NavigatableRecordStream when:**
- Sort criteria determined by user input after initial query
- Need to view data in multiple sort orders
- Want to paginate backward or jump to specific pages
- Database queries are expensive
- Working with a bounded result set that fits in memory

**Don't use NavigatableRecordStream when:**
- Working with millions of records
- Sort criteria known upfront (use `QueryBuilder.sortReturnedSubsetBy()`)
- Only need forward-only pagination
- Memory is constrained

### Performance Characteristics

- **Initial load**: O(n) - reads all records from source stream
- **Sorting**: O(n log n) - uses Java's `Arrays.sort()`
- **Re-sorting**: O(n log n) - no database query
- **Page jumping**: O(1) - direct array access
- **Iteration**: O(1) per record

## Comparison with QueryBuilder Sorting

### QueryBuilder Approach (Database-Side)

```java
RecordStream results = session.query(customerDataSet)
    .sortReturnedSubsetBy("age", SortDir.SORT_DESC)
    .limit(1000)  // Required for sorting
    .pageSize(20)
    .execute();
```

**Pros:**
- Works with any size dataset
- Memory efficient

**Cons:**
- Sort criteria must be known upfront
- Cannot change sort without re-querying
- Requires a limit
- Cannot navigate backward

### NavigatableRecordStream Approach (In-Memory)

```java
RecordStream results = session.query(customerDataSet)
    .limit(1000)
    .execute();

NavigatableRecordStream nav = results.asNavigatableStream()
    .pageSize(20)
    .sortBy("age", SortDir.SORT_DESC);

// Later, change sort without re-querying
nav.clearSort().sortBy("name");
```

**Pros:**
- Dynamic re-sorting without database queries
- Can change sort order multiple times
- Forward and backward navigation
- Jump to any page
- Re-iterate with different sorts

**Cons:**
- Loads all records into memory
- Not suitable for very large datasets without limit

## Testing the Examples

To run the comprehensive examples:

```bash
# Make sure Aerospike is running on localhost:3100
# with credentials admin/password123

cd /Users/tfaulkes/Programming/Aerospike/git/aerospike-fluent-client-java
javac -d bin src/main/java/com/example/NavigatableRecordStreamExample.java
java -cp bin com.example.NavigatableRecordStreamExample
```

The example will:
1. Create 30 test customer records
2. Demonstrate 6 different usage scenarios
3. Show forward/backward pagination
4. Show multi-column sorting
5. Show dynamic re-sorting
6. Clean up test data

## Implementation Notes

### Thread Safety

`NavigatableRecordStream` is **not thread-safe**. If multiple threads need to access the same instance, external synchronization is required. However, each thread can safely have its own instance reading from the same data.

### Closing Resources

`NavigatableRecordStream` implements `Closeable` but doesn't hold any resources that need cleanup (data is already in memory). The `close()` method is a no-op but is provided for consistency with `RecordStream`.

### Integration with Existing Code

The implementation is designed to integrate seamlessly with existing code:
- Uses existing `RecordComparator` for sorting
- Uses existing `SortProperties` and `SortDir` classes
- Implements existing `Sortable` and `ResettablePagination` interfaces
- Follows the same patterns as `FixedSizeRecordStream`

## Future Enhancements (Optional)

Potential future improvements could include:
1. Lazy sorting (sort only when needed)
2. Incremental loading (load records on demand)
3. Filtering support (similar to `failures()` method)
4. Statistics (min, max, average, etc.)
5. Custom comparators for complex sorting logic


