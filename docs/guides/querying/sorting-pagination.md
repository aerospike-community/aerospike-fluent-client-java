# Sorting & Pagination

Learn how to manage large result sets by sorting and paginating query results.

## Goal

By the end of this guide, you'll know how to:
- Sort query results by one or more bin values
- Control sort direction (ascending/descending)
- Paginate through a large number of records efficiently
- Understand the performance implications of sorting and pagination

## Prerequisites

- [Quick Start](../../getting-started/quickstart.md) completed
- Understanding of [Querying Data](../querying/simple-queries.md)

---

## Sorting

You can sort the results of a query on the client side by specifying one or more sort fields.

### 1. Simple Sorting

Sort by a single bin in ascending order.

```java
import com.aerospike.query.SortDir;

// Find users and sort them by age, youngest first
RecordStream results = session.query(users)
    .where("$.city == 'New York'")
    .sortReturnedSubsetBy("age", SortDir.SORT_ASC)
    .limit(100) // IMPORTANT: Sorting requires a limit
    .execute();

results.forEach(record -> {
    System.out.println(record.record.getString("name") + " is " + record.record.getInt("age"));
});
```

> **⚠️ Important**: Client-side sorting requires a `limit()` to be set on the query. This is a safeguard to prevent the client from running out of memory by trying to load and sort an unbounded number of records.

### 2. Sorting Direction

You can specify either ascending (`SORT_ASC`) or descending (`SORT_DESC`) order.

```java
// Sort by age, oldest first
RecordStream results = session.query(users)
    .where("$.city == 'New York'")
    .sortReturnedSubsetBy("age", SortDir.SORT_DESC)
    .limit(100)
    .execute();
```

### 3. Multi-Field Sorting

You can chain `sortReturnedSubsetBy` calls to sort by multiple fields. The sorting is applied in the order the methods are called.

```java
// Sort by city, then by age within each city
RecordStream results = session.query(users)
    .sortReturnedSubsetBy("city", SortDir.SORT_ASC)
    .sortReturnedSubsetBy("age", SortDir.SORT_DESC)
    .limit(500)
    .execute();
```

### 4. Case-Sensitive Sorting

For string bins, you can control whether the sort is case-sensitive.

```java
// Sort by name, case-insensitively
RecordStream results = session.query(users)
    .sortReturnedSubsetBy("name", SortDir.SORT_ASC, false) // false for case-insensitive
    .limit(100)
    .execute();
```

---

## Pagination

Pagination allows you to process a large result set in smaller, manageable chunks or "pages." This is essential for applications that display data in tables or lists, and it helps prevent high memory usage.

### 1. Using `pageSize()`

The `pageSize()` method on the `QueryBuilder` specifies how many records to fetch per page.

```java
RecordStream results = session.query(users)
    .pageSize(100) // Fetch 100 records at a time
    .execute();
```

### 2. Iterating Through Pages

The `RecordStream` provides a `hasMorePages()` method to check if there are more pages of results available. You can then process the current page and move to the next.

```java
import java.util.List;

RecordStream results = session.query(users)
    .pageSize(100)
    .execute();

int pageNumber = 1;
while (results.hasMorePages()) {
    System.out.println("Processing page " + pageNumber++);
    
    // Process the records on the current page
    while(results.hasNext()) {
        KeyRecord keyRecord = results.next();
        // process record...
    }
    
    // When results.hasNext() becomes false, the next call to hasMorePages()
    // will fetch the next page from the server if available.
}
```

### 3. Converting Pages to Object Lists

A common pattern is to convert each page of records directly into a list of Java objects.

```java
RecordStream results = session.query(customers)
    .pageSize(50)
    .execute();

while (results.hasMorePages()) {
    List<Customer> customerPage = results.toObjectList(customerMapper);
    
    System.out.println("Fetched a page with " + customerPage.size() + " customers.");
    // process the list of customers...
}
```

---

## Complete Example: Paginated User Directory

This example demonstrates fetching and displaying a sorted list of users, page by page.

```java
import com.aerospike.RecordStream;
import com.aerospike.query.SortDir;
import java.util.List;

public class UserDirectory {
    private final Session session;
    private final TypeSafeDataSet<User> users;
    private final RecordMapper<User> userMapper;

    public UserDirectory(Session session) {
        this.session = session;
        this.users = TypeSafeDataSet.of("test", "users", User.class);
        this.userMapper = new UserMapper(); // Assume UserMapper is implemented
    }

    public void displaySortedUsers() {
        System.out.println("Fetching sorted user directory...");

        RecordStream results = session.query(users)
            .sortReturnedSubsetBy("lastName", SortDir.SORT_ASC)
            .sortReturnedSubsetBy("firstName", SortDir.SORT_ASC)
            .limit(1000) // A limit is required for sorting
            .pageSize(10)
            .execute();

        int pageNum = 1;
        while (results.hasMorePages()) {
            List<User> userPage = results.toObjectList(userMapper);
            if (userPage.isEmpty()) break;

            System.out.println("\n--- Page " + pageNum++ + " ---");
            for (User user : userPage) {
                System.out.printf("- %s, %s (Age: %d)%n", 
                    user.getLastName(), user.getFirstName(), user.getAge());
            }
        }
        System.out.println("\n--- End of Directory ---");
    }
}
```

---

## Performance Considerations

### Sorting vs. Pagination

- **Pagination without Sorting**: Very efficient. The client streams records from the server as you iterate, using minimal client-side memory.
- **Sorting**: Less efficient. The client must fetch **all records up to the specified `limit()`** from the server, store them in memory, and then perform the sort before returning the first result.

### Best Practices

- **Always use `limit()` with `sortReturnedSubsetBy()`**. This is a mandatory safeguard.
- **Keep limits reasonable**. Sorting a million records on the client will consume significant memory. If you need to sort large datasets, consider alternative data modeling or post-processing strategies.
- **Use pagination for UIs and large data processing**. Avoid loading millions of records into a single list in your application.
- **Do not combine sorting and pagination logic in the same query if you expect to traverse many pages**. The `RecordStream` will fetch all records up to the `limit`, sort them, and then deliver them in pages. This means the time-to-first-record is high. For true paginated sorting, you would need to implement a more advanced logic (e.g., keyset pagination), which is not a built-in feature of the Fluent Client.

---

## Next Steps

- **[Partition Targeting](./partition-targeting.md)**: Learn how to optimize query performance by targeting specific data partitions.
- **[Query Optimization](../performance/query-optimization.md)**: Explore more ways to make your queries faster.
