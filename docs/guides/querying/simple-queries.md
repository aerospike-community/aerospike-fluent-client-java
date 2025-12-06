# Simple Queries

Learn the basics of querying data in Aerospike using the Fluent Client.

## Goal

By the end of this guide, you'll know how to:
- Scan all records in a set
- Query records using a secondary index
- Limit the number of results
- Select specific bins to return
- Understand the difference between a scan and a query

## Prerequisites

- [CRUD Guides](../crud/README.md) completed
- Basic understanding of database indexes

---

## Scan vs. Query

**Scan**
- Reads **every record** in a namespace or set.
- Does **not** require a secondary index.
- Can be slow and resource-intensive on large datasets.
- Use for: Full data processing, backups, analytics jobs.

**Query**
- Reads only records that **match a filter**.
- **Requires a secondary index** on the filtered bin.
- Very fast and efficient, even on large datasets.
- Use for: Selective data retrieval in applications.

---

## Scanning Records

A scan is the simplest way to retrieve multiple records.

### Scanning a Full Set

```java
DataSet users = DataSet.of("test", "users");

// This will read every record in the "users" set
RecordStream allUsers = session.query(users).execute();

allUsers.forEach(record -> {
    System.out.println("User: " + record.recordOrThrow().getString("name"));
});
```

### Limiting Scan Results

Use `limit()` to stop the scan after a certain number of records have been retrieved. This is useful for sampling data.

```java
// Get a sample of up to 100 users
RecordStream userSample = session.query(users)
    .limit(100)
    .execute();
```

### Selecting Bins in a Scan

Reduce network traffic by selecting only the bins you need.

```java
// Get only the email and country for all users
RecordStream userEmails = session.query(users)
    .readingOnlyBins("email", "country")
    .execute();
```

---

## Querying with a Secondary Index

To perform efficient queries, you must have a secondary index on the bin you are filtering.

### 1. Create a Secondary Index

You can create an index using `aql` or another Aerospike tool.

```sql
-- In aql, create an index on the 'age' bin in the 'users' set
CREATE INDEX idx_users_age ON test.users (age) NUMERIC
```

### 2. Query Using the Index

Use `where()` to apply a filter. The Fluent Client will automatically use the secondary index if one exists for the filtered bin.

```java
DataSet users = DataSet.of("test", "users");

// Find all users who are 30 years old
RecordStream results = session.query(users)
    .where("$.age == 30")
    .execute();

results.forEach(record -> {
    System.out.println(record.recordOrThrow().getString("name") + " is 30.");
});
```

### Querying on a String Bin

```sql
-- Create an index on the 'city' bin
CREATE INDEX idx_users_city ON test.users (city) STRING
```

```java
// Find all users in "New York"
RecordStream newYorkers = session.query(users)
    .where("$.city == 'New York'")
    .execute();
```

### Querying a Numeric Range

```java
// Find users between the ages of 25 and 35 (inclusive)
RecordStream ageRange = session.query(users)
    .where("$.age >= 25 and $.age <= 35")
    .execute();
```

---

## The `RecordStream` Result

All query and scan operations return a `RecordStream`.

### Processing Results

```java
RecordStream results = session.query(users).where("$.active == true").execute();

// Using a while loop
while (results.hasNext()) {
    RecordResult record = results.next();
    // Process record...
}

// Using forEach
results.forEach(record -> {
    // Process record...
});

// Using Java Stream API
long count = results.stream()
    .filter(kr -> kr.recordOrThrow().getInt("loginCount") > 10)
    .count();
```

See [Reading Records](../crud/reading-records.md#working-with-recordstream) for a full guide on `RecordStream`.

---

## Complete Example: Product Search

```java
public class ProductSearch {
    private final Session session;
    private final DataSet products;
    
    public ProductSearch(Session session) {
        this.session = session;
        this.products = DataSet.of("ecommerce", "products");
    }
    
    // Assumes a secondary index exists on 'category' (STRING)
    public List<Product> findByCategory(String category) {
        RecordStream results = session.query(products)
            .where("$.category == '" + category + "'")
            .execute();
            
        return results.stream()
            .map(this::mapToProduct)
            .collect(Collectors.toList());
    }
    
    // Assumes a secondary index exists on 'price' (NUMERIC)
    public List<Product> findByPriceRange(double minPrice, double maxPrice) {
        RecordStream results = session.query(products)
            .where("$.price >= " + minPrice + " and $.price <= " + maxPrice)
            .execute();
            
        return results.stream()
            .map(this::mapToProduct)
            .collect(Collectors.toList());
    }
    
    // A scan operation
    public List<Product> getFeaturedProducts(int limit) {
        return session.query(products)
            .where("$.featured == true") // This might be a scan if 'featured' is not indexed
            .limit(limit)
            .execute()
            .stream()
            .map(this::mapToProduct)
            .collect(Collectors.toList());
    }
    
    private Product mapToProduct(RecordResult kr) {
        // Mapping logic...
        return new Product(...);
    }
}
```

---

## Error Handling

When querying, especially with scans, it's important to handle potential errors.

```java
import com.aerospike.client.AerospikeException;

try {
    RecordStream results = session.query(users)
        .where("$.age > 30")
        .execute();
    
    results.forEach(record -> {
        // process...
    });

} catch (AerospikeException.Timeout e) {
    System.err.println("Query timed out. Results may be incomplete.");

} catch (AerospikeException e) {
    // This can happen if an index doesn't exist and scans are disabled
    if (e.getResultCode() == com.aerospike.client.ResultCode.INDEX_NOT_FOUND) {
        System.err.println("Query failed: Secondary index not found.");
    } else {
        System.err.println("An unexpected error occurred: " + e.getMessage());
    }
}
```

---

## Best Practices

### ✅ DO

**Create secondary indexes for common query filters**
This is the most important step for query performance.

**Be specific in your filters**
`where("$.age == 30")` is much faster than scanning and filtering on the client side.

**Select only the bins you need**
```java
session.query(users)
    .where("$.status == 'active'")
    .readingOnlyBins("userId", "email")
    .execute();
```

### ❌ DON'T

**Don't run unfiltered scans on large sets in production applications**
This can cause high server load and network traffic. Use scans for background jobs or analytics.

**Don't filter on a bin that is not indexed**
The client will fall back to a full scan and apply the filter on the client side, which is very inefficient.

**Don't assume a `where()` clause will use an index**
The filter must be on a bin that has a secondary index created on the server.

---

## Troubleshooting

### Query is Slow

**Problem**: A query with a `where()` clause is taking a long time.

**Cause**: Most likely, there is no secondary index on the bin you are filtering.

**Solution**:
1.  Verify the index exists: `aql> show indexes`
2.  Create the index if it's missing: `aql> CREATE INDEX ...`
3.  Check the data type of the index (NUMERIC, STRING, GEO2DSPHERE).
4.  Ensure your `where()` clause matches the indexed data type.

### Empty Results

**Problem**: A query returns no records, but you know matching data exists.

**Causes**:
- **Data type mismatch**: Querying a numeric bin with a string value (e.g., `where("$.age == '30'")` instead of `where("$.age == 30")`).
- **Set/Namespace mismatch**: The `DataSet` is pointing to the wrong location.
- **Index not populated**: The index might still be building on the server. Check index status with `asinfo`.

---

## Next Steps

- **[Filtering with WHERE](./filtering.md)** - Learn advanced filtering techniques.
- **[Using the DSL](./using-dsl.md)** - Write type-safe query expressions.
- **[Sorting & Pagination](./sorting-pagination.md)** - Manage large result sets.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
