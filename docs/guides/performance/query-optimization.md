# Query Optimization

Learn best practices and techniques for writing high-performance queries and scans with the Fluent Client.

## Goal

By the end of this guide, you'll know how to:
- Use secondary indexes effectively
- Minimize data transfer with projections
- Avoid performance pitfalls with scans
- Understand the impact of filters on query speed
- Configure query-specific policies

## Prerequisites

- [Simple Queries](../../guides/querying/simple-queries.md)
- [Filtering with WHERE](../../guides/querying/filtering.md)

---

## 1. The Golden Rule: Index Everything You Query

The single most important factor for query performance is the presence of a **secondary index** on the bin you are filtering.

- A query with a `where()` clause on an **indexed** bin is a **Secondary Index Query**. The server efficiently looks up matching records and returns only that subset. This is **fast**.
- A query with a `where()` clause on a **non-indexed** bin results in a **Scan**. The client must read **every record** in the set and apply the filter on the client side. This is **very slow**.

**Bad: Scan + Client-Side Filter**
```java
// SLOW: 'city' is not indexed. This will scan the entire 'users' set.
session.query(users)
    .where(stringBin("city").eq("London"))
    .execute();
```

**Good: Secondary Index Query**
```java
// 1. Create the index in aql
// CREATE INDEX idx_users_city ON test.users (city) STRING

// 2. Run the query
// FAST: This query will use the secondary index.
session.query(users)
    .where(stringBin("city").eq("London"))
    .execute();
```

## 2. Projection: Fetch Only What You Need

By default, a query returns all bins for each matching record. You can significantly improve performance by reducing the amount of data transferred over the network.

### `readingOnlyBins(String... binNames)`

Specify exactly which bins you need. This is the most common and effective projection technique.

**Bad: Fetching all 50 bins for a user**
```java
// Inefficient if you only need the email address
session.query(userKey).execute();
```

**Good: Fetching only 1 bin**
```java
// Efficient: Minimal network traffic
session.query(userKey)
    .readingOnlyBins("email")
    .execute();
```

### `withNoBins()`

Use this when you only need to check for the existence of a record or read its metadata (generation, TTL). This is the fastest way to read from the database as no bin data is transferred.

```java
// Get the generation for optimistic locking
int generation = session.query(userKey)
    .withNoBins()
    .execute()
    .getFirst()
    .get().record.generation;
```

## 3. Understand Your Filters

The selectivity of your filter has a major impact on performance.

### High-Selectivity Filters (Good)

Filters that match a small number of records are ideal. Equality checks on high-cardinality bins are very efficient.

```java
// Very fast: Assumes 'email' is indexed and unique.
.where(stringBin("email").eq("user@example.com"))
```

### Low-Selectivity Filters (Use with Caution)

Filters that match a large percentage of your dataset can still put significant load on the server, even with an index.

```java
// Can be slow: If half of your users are 'active', this will return a lot of data.
.where(booleanBin("isActive").isTrue())
```
For low-selectivity queries, consider if a background scan or an analytics job is a better approach.

### Range and Regex Filters

Range (`>`, `<`, `BETWEEN`) and regex (`matches`) queries are supported by secondary indexes, but they are generally less performant than direct equality checks. Use them when necessary, but prefer equality filters for latency-critical operations.

## 4. Scans: Know When and How to Use Them

Scans are powerful but can be dangerous if misused in a production application.

### When to Use Scans

- **Background jobs**: Data cleanup, validation, or migration scripts.
- **Analytics**: Full-dataset analysis where all records must be processed.
- **Small datasets**: On sets with a few thousand records, a scan can be perfectly acceptable.

### How to Use Scans Safely

- **Always use projection**: Never scan all bins unless you absolutely need them.
- **Control the pace**: If you are running a large backfill, introduce delays between processing chunks of records to avoid overloading the cluster.
- **Configure scan policies**: Use a dedicated `Behavior` to set scan-specific timeouts and other policies.

```java
Behavior scanBehavior = Behavior.DEFAULT.deriveWithChanges("scan-job", builder ->
    builder.onScans()
        .abandonCallAfter(Duration.ofMinutes(30)) // Allow long-running scans
    .done()
);

Session scanSession = cluster.createSession(scanBehavior);

scanSession.query(users)
    .readingOnlyBins("userId", "lastLogin")
    .execute()
    .forEach(record -> {
        // process...
    });
```

## 5. Tune Query Policies

For advanced use cases, you can tune query-specific policies in your `Behavior`.

```java
Behavior queryBehavior = Behavior.DEFAULT.deriveWithChanges("query-heavy", builder ->
    builder.onQueries()
        // The maximum number of records to return in a single sub-query to a node.
        // The default is 0 (no limit). Tuning this can help with memory management on the client.
        .maxRecords(5000)
    .done()
);
```

## Performance Checklist

When writing a query, ask yourself:

1.  [ ] **Am I filtering on an indexed bin?** (Most important)
2.  [ ] **Am I using projection (`readingOnlyBins`) to fetch only the data I need?**
3.  [ ] **Is my filter highly selective?** (Will it match a small, targeted set of records?)
4.  [ ] **If this is a scan, is it running in a background job, not a user-facing request?**
5.  [ ] **Have I considered the performance implications of my `where` clause?** (Equality vs. Range/Regex)

---

## Next Steps

- **[How-To Guides](../../guides/README.md)** - Revisit the guides for practical examples.
- **[API Reference: `Behavior`](../../api/behavior.md)** - For a full list of all policy settings.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
