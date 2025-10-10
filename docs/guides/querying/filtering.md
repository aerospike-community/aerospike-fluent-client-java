# Filtering with WHERE

Learn advanced techniques for filtering data using the `where()` clause in the Fluent Client.

## Goal

By the end of this guide, you'll know how to:
- Choose between string-based and DSL-based filters
- Construct complex logical queries (`AND`, `OR`, `NOT`)
- Filter on different data types (numbers, strings, booleans)
- Use range, list, and regex filters
- Understand the importance of secondary indexes for filtering

## Prerequisites

- [Simple Queries](./simple-queries.md) completed
- [Using the DSL](./using-dsl.md) completed

---

## Two Ways to Filter: String vs. DSL

### 1. String-Based `where()`

- **Pros**: Quick for simple queries, easy to copy-paste.
- **Cons**: Prone to typos, no compile-time checks, harder to refactor, risk of injection.

```java
session.query(users)
    .where("$.age >= 18 and $.country == 'USA'")
    .execute();
```

### 2. DSL-Based `where()`

- **Pros**: Type-safe, compile-time checked, easy to refactor, no injection risk.
- **Cons**: More verbose for very simple queries.

```java
import static com.aerospike.dsl.Dsl.*;

session.query(users)
    .where(
        and(
            longBin("age").gte(18),
            stringBin("country").eq("USA")
        )
    )
    .execute();
```

> **Recommendation**: Use the **DSL** for all application code. Use strings for quick tests or debugging if needed.

---

## Filtering by Data Type

### Numeric Bins

**Use Case**: Find users by age, products by price, etc.
**Index Type**: `NUMERIC`

**String-based**:
```java
.where("$.price > 100 and $.price <= 500")
```

**DSL-based**:
```java
.where(
    and(
        doubleBin("price").gt(100),
        doubleBin("price").lte(500)
    )
)
```

### String Bins

**Use Case**: Find users by city, products by category, etc.
**Index Type**: `STRING`

**String-based**:
```java
.where("$.category == 'electronics'")
```

**DSL-based**:
```java
.where(stringBin("category").eq("electronics"))
```

### Boolean Bins

**Use Case**: Find active users, featured products, etc.
**Index Type**: `NUMERIC` (Booleans are stored as 0 or 1)

**String-based**:
```java
.where("$.isActive == true")
```

**DSL-based**:
```java
.where(booleanBin("isActive").isTrue())

// or for false
.where(booleanBin("isActive").isFalse())
```

---

## Advanced Filtering Techniques

### Combining Conditions (`AND`, `OR`, `NOT`)

**Use Case**: Find active, premium users from the USA.

**String-based**:
```java
.where("$.isActive == true and $.plan == 'premium' and $.country == 'USA'")
```

**DSL-based**:
```java
.where(
    and(
        booleanBin("isActive").isTrue(),
        stringBin("plan").eq("premium"),
        stringBin("country").eq("USA")
    )
)
```

**Use Case**: Find users who are either admins or moderators.

**String-based**:
```java
.where("$.role == 'admin' or $.role == 'moderator'")
```

**DSL-based**:
```java
.where(
    or(
        stringBin("role").eq("admin"),
        stringBin("role").eq("moderator")
    )
)
```

**Use Case**: Find all users who are NOT archived.

**String-based**:
```java
.where("not $.status == 'archived'")
```

**DSL-based**:
```java
.where(not(stringBin("status").eq("archived")))
```

### Filtering with a List (`IN`)

**Use Case**: Find products in a specific set of categories.

**String-based**: Not directly supported in a simple way.

**DSL-based**:
```java
.where(stringBin("category").in("electronics", "books", "home"))
```

### Regular Expression Matching

**Use Case**: Find users with a specific email domain.
**Index Type**: `STRING`

**String-based**:
```java
// Note: Requires careful escaping
.where("$.email LIKE '.*@example\\.com$'")
```

**DSL-based**:
```java
.where(stringBin("email").matches(".*@example\\.com$"))
```

---

## The Role of Secondary Indexes

**A `where()` clause is only efficient if it operates on an indexed bin.**

- If an index **exists**, the query is executed on the server, and only matching records are returned. This is **very fast**.
- If an index **does not exist**, the client performs a full **scan** of the entire set and applies the filter on the client side. This is **very slow** and should be avoided in production applications.

### How to Verify Index Usage

1.  **Check `aql`**: `show indexes`
2.  **Monitor Server Logs**: Look for query logs to see if an index is being used.
3.  **Performance**: If a query is slow, it's likely not using an index.

### Example: Indexed vs. Non-Indexed Query

```java
// Assume 'country' is indexed, but 'department' is not

// ✅ FAST: Uses a secondary index on 'country'
session.query(users)
    .where(stringBin("country").eq("USA"))
    .execute();

// ❌ SLOW: Scans the entire 'users' set because 'department' is not indexed
session.query(users)
    .where(stringBin("department").eq("engineering"))
    .execute();
```

---

## Complete Example: E-commerce Filter

```java
import static com.aerospike.dsl.Dsl.*;
import java.util.List;

public class ProductFilterService {
    private final Session session;
    private final DataSet products;
    
    public ProductFilterService(Session session) {
        this.session = session;
        this.products = DataSet.of("ecommerce", "products");
    }
    
    // Assumes indexes on 'category', 'onSale', and 'price'
    public List<Product> findSaleItemsInCategory(String category, double maxPrice) {
        
        var filter = and(
            stringBin("category").eq(category),
            booleanBin("onSale").isTrue(),
            doubleBin("price").lte(maxPrice)
        );
        
        RecordStream results = session.query(products)
            .where(filter)
            .readingOnlyBins("productId", "name", "price")
            .limit(100)
            .execute();
            
        return results.stream()
            .map(this::mapToProduct)
            .collect(Collectors.toList());
    }
    
    private Product mapToProduct(KeyRecord kr) {
        // Mapping logic...
        return new Product(...);
    }
}
```

---

## Best Practices

### ✅ DO

**Always filter on indexed bins for application queries.**
This is the most critical rule for query performance.

**Use the DSL for type safety and maintainability.**
It prevents a wide range of common bugs.

**Combine `where()` with other query builders.**
```java
session.query(products)
    .where(stringBin("category").eq("electronics"))
    .readingOnlyBins("name", "price") // Select bins
    .limit(50)                        // Limit results
    .execute();
```

**Reuse filter expressions.**
```java
private static final BooleanExpression IS_ACTIVE = booleanBin("isActive").isTrue();

// ...
session.query(users).where(and(IS_ACTIVE, ...)).execute();
```

### ❌ DON'T

**Don't write queries with filters on non-indexed fields in performance-critical code.**
This will lead to slow, full scans.

**Don't build filter strings with user input.**
This can lead to injection vulnerabilities. The DSL is inherently safe from this.

**Don't apply complex client-side filtering after a broad query.**
Let the database do the filtering. It's much more efficient.

---

## Next Steps

You've now mastered the core of querying!

- **[Sorting & Pagination](./sorting-pagination.md)** - Learn how to order and paginate your results.
- **[Performance Guide](../performance/query-optimization.md)** - Dive deeper into query optimization.
- **[API Reference](../../api/dsl/README.md)** - See all available DSL operators.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
