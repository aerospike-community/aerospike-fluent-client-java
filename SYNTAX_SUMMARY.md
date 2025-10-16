# Aerospike Fluent Client - Syntax Summary & Key Findings

> **Purpose:** This document provides a concise overview of the fluent client's syntax patterns and consistency assessment. For the complete guide, see [SYNTAX_GUIDE.md](SYNTAX_GUIDE.md).

---

## TL;DR: Design Philosophy

This client follows a **fluent, declarative style emphasizing readability over verbosity**. The API reads like natural language: "Connect to cluster → Create session → Query dataset → Execute operation."

**Core Pattern:**
```java
session.action(target).configure().specify().execute()
```

**Example:**
```java
session.upsert(key)
    .bin("name").setTo("Alice")
    .expireRecordAfter(Duration.ofDays(30))
    .execute();
```

---

## Grammar Rules at a Glance

| Element | Pattern | Examples |
|---------|---------|----------|
| **Actions** | Verb-first | `query()`, `upsert()`, `insert()`, `update()`, `delete()` |
| **Configuration** | `with` prefix | `withNativeCredentials()`, `withLogLevel()` |
| **Targeting** | `on` prefix | `onMapKey()`, `onListIndex()`, `onPartition()` |
| **Factories** | `of()` static | `DataSet.of()`, `TypeSafeDataSet.of()` |
| **Sub-navigation** | Plain noun | `.bin("name")`, `.bins("a", "b")` |
| **Conditions** | `where()` / `when()` | `.where("$.age > 30")` |
| **Termination** | `.execute()` / `.connect()` | Triggers actual operation |
| **Getters** | `get` prefix | `getNamespace()`, `getSet()` |
| **Boolean queries** | `is` prefix | `isConnected()`, `isNamespaceSC()` |

---

## Common Patterns

### 1. Connection Flow
```java
Cluster cluster = new ClusterDefinition("host", 3100)
    .withNativeCredentials("user", "pass")
    .connect();
Session session = cluster.createSession(Behavior.DEFAULT);
```

### 2. Write Operations
```java
session.upsert(key)
    .bin("field").setTo(value)
    .bin("count").add(1)
    .execute();
```

### 3. Queries
```java
RecordStream results = session.query(dataSet)
    .where("$.active == true")
    .limit(100)
    .sortReturnedSubsetBy("name")
    .execute();
```

### 4. CDT Operations
```java
session.upsert(key)
    .bin("map").onMapKey("player1").setTo(9500)
    .bin("list").onListIndex(0).remove()
    .execute();
```

### 5. Transactions
```java
session.doInTransaction(tx -> {
    tx.upsert(key1).bin("x").setTo(1).execute();
    tx.delete(key2).execute();
});
```

---

## Type Safety Features

✅ **Strongly Typed:**
- `TypeSafeDataSet<Customer>` prevents wrong object types
- CDT builders enforce legal operations at compile time (e.g., can't call `.countAllOthers()` after `.onMapIndex()`)
- Generic builders: `OperationObjectBuilder<T>`

✅ **Progressive Disclosure:**
- Simple cases require minimal code
- Complex features discovered through IDE autocomplete
- Overloaded methods for different types (String, int, long, byte[])

---

## Consistency Assessment

### ✅ Highly Consistent Areas

| Area | Pattern | Notes |
|------|---------|-------|
| **Builder chaining** | All config methods return `this` | ✅ Excellent |
| **Terminal operations** | Always `.execute()` or `.connect()` | ✅ Predictable |
| **Verb-first actions** | CRUD operations start with verb | ✅ Natural language flow |
| **Per-node variants** | `foo()` and `fooPerNode()` | ✅ Clear convention |
| **Overloading strategy** | Type-specific overloads everywhere | ✅ Java-idiomatic |
| **Optional returns** | `Optional<T>` for nullable singles | ✅ Modern Java |

### ⚠️ Minor Inconsistencies Detected

| Issue | Current State | Impact | Recommendation |
|-------|---------------|--------|----------------|
| **Typo in field name** | `preferrredRacks` (triple 'r') | Low - internal only | Rename to `preferredRacks` |
| **Verb inconsistency** | `insertInto()` vs `upsert()` | Low - established API | Consider dropping "Into" suffix in v2.0 |
| **Query terminology** | `query(key)` for point reads | Low - slight confusion | Add `read(key)` alias or document clearly |

### ✅ Intentional "Inconsistencies" (Actually Correct)

| Pattern | Rationale | Verdict |
|---------|-----------|---------|
| **Complex CDT interfaces** | Enforce compile-time safety | ✅ Keep - prevents runtime errors |
| **Two transaction APIs** | Lambda (recommended) + manual (escape hatch) | ✅ Keep - different use cases |
| **`done()` only in nested builders** | Returns to parent scope | ✅ Keep - clear scope exit |

---

## Naming Conventions Reference

### Method Prefixes
- `with` → Additive configuration (`.withNativeCredentials()`)
- `on` → Targeting/scoping (`.onMapKey()`, `.onPartition()`)
- `of` → Static factories (`.of()`)
- `ensure` → Preconditions (`.ensureGenerationIs()`)
- `using` → Dependency injection (`.using(mapper)`)
- `get` → Accessors (`.getNamespace()`)
- `is` → Boolean queries (`.isConnected()`)

### Pluralization
- Singular for single operations: `bin()`, `id()`, `object()`
- Plural for batch: `bins()`, `ids()`, `objects()`
- Plural for collections: `namespaces()`, `secondaryIndexes()`

### Overloading Strategy
- **Type variants**: `id(String)`, `id(int)`, `id(long)`
- **Single vs batch**: `upsert(Key)` vs `upsert(Key, Key...)`
- **With/without options**: `insert(v)` vs `insert(v, allowFailures)`

---

## Recommendations for New Features

When extending this API, follow these rules:

### ✅ Do:
1. Use verb-first naming for actions
2. Return builder type for fluent chaining
3. Overload for type variants (String, int, long)
4. Use `.execute()` as terminal operation
5. Use `with` prefix for configuration
6. Use `on` prefix for targeting
7. Document with fluent chain examples

### ❌ Don't:
1. Break fluent chains (avoid void returns on builders)
2. Mix semantically different operations in one overload
3. Invent new verb patterns when existing ones fit
4. Return builders from terminal operations
5. Use checked exceptions in fluent chains

---

## Quick Pattern Matcher

**Need to add a feature? Match the pattern:**

| Feature Type | Pattern Template | Example |
|--------------|------------------|---------|
| **New read operation** | `session.query(target).filter*().execute()` | `session.scan(set).where(...).execute()` |
| **New write operation** | `session.action(target).specify*().execute()` | `session.merge(key).bin(...).execute()` |
| **New configuration** | `builder.withOption(value)` | `builder.withTimeout(duration)` |
| **New targeting** | `builder.onTarget(value)` | `builder.onSecondaryNode()` |
| **Batch variant** | Same verb + varargs | `action(k)` → `action(k1, k2, ...)` |
| **Per-node variant** | Same method + `PerNode` | `foo()` → `fooPerNode()` |

---

## Code Examples for LLM Generation

### Pattern 1: Simple CRUD
```java
// Template
session.{action}(key)
    .bin("field").setTo(value)
    .{configuration}()
    .execute();

// Instance
session.upsert(key)
    .bin("status").setTo("active")
    .expireRecordAfter(Duration.ofDays(7))
    .execute();
```

### Pattern 2: Filtered Query
```java
// Template
session.query(dataSet)
    .where({filterExpression})
    .{pagination}()
    .execute();

// Instance
session.query(users)
    .where("$.age > 18 and $.verified == true")
    .limit(1000)
    .execute();
```

### Pattern 3: CDT Navigation
```java
// Template
session.{action}(key)
    .bin("cdt").on{Structure}{Selector}(key).{action}()
    .execute();

// Instance
session.upsert(key)
    .bin("scores").onMapKey("player1").setTo(9500)
    .bin("items").onListIndex(0).remove()
    .execute();
```

### Pattern 4: Object Mapping
```java
// Template
session.{action}(typedDataSet)
    .object(instance)
    .using(mapper)  // optional
    .execute();

// Instance
session.upsert(customerDataSet)
    .object(customer)
    .execute();
```

---

## Validation Checklist for New APIs

When adding new methods, verify:

- [ ] Follows verb-first naming if action
- [ ] Returns builder type for chaining (except terminal)
- [ ] Uses consistent prefix (`with`, `on`, `ensure`, etc.)
- [ ] Overloaded for common types (String, int, long)
- [ ] Has `.execute()` or equivalent terminal operation
- [ ] Javadoc includes fluent chain example
- [ ] Type-safe through generics where applicable
- [ ] Matches existing verb patterns (don't add `fetch()` if `query()` exists)
- [ ] Parameter ordering: required first, optional last
- [ ] Pluralization matches singular/batch convention

---

## For LLM Prompt Engineering

**When generating new Aerospike fluent client code:**

> "Follow the fluent builder pattern with verb-first method names. Methods should chain by returning the builder type. Configuration methods use 'with' prefix, targeting uses 'on' prefix. Terminal operations call .execute(). Overload methods for String, int, long, byte[] variants. Use TypeSafe generics where applicable. Example: `session.upsert(key).bin("name").setTo("value").expireRecordAfter(duration).execute();`"

**Key phrases to include in prompts:**
- "Fluent builder pattern"
- "Verb-first naming"
- "Return this for chaining"
- "Terminal .execute() method"
- "Overload for type variants"
- "Use 'with' prefix for configuration"
- "Use 'on' prefix for targeting"

---

## Conclusion

The Aerospike Fluent Client Java API demonstrates **excellent consistency** in its design patterns, with only minor naming issues that don't impact usability. The fluent interface successfully achieves:

✅ **Discoverability** - IDE autocomplete guides users through operations  
✅ **Type Safety** - Compile-time checks prevent common errors  
✅ **Readability** - Code reads like natural language  
✅ **Consistency** - Predictable patterns across all operations  
✅ **Progressive Complexity** - Simple things simple, complex things possible  

**Verdict:** The syntax is well-designed, production-ready, and suitable as a template for similar fluent APIs. The identified minor inconsistencies are low-priority cosmetic issues that don't affect functionality.

---

**See [SYNTAX_GUIDE.md](SYNTAX_GUIDE.md) for complete details, anti-patterns, and code generation templates.**

