# Aerospike Fluent Client Java - Syntax Guide & Consistency Assessment

## Overview

This client follows a **fluent, method-chaining style** that emphasizes **readability, discoverability, and type safety**. The API is designed to read like natural language, guiding developers from connection → session → operation → execution with minimal boilerplate. Core principles include:

- **Builder pattern everywhere**: Almost every operation returns a builder for continued configuration
- **Action-first semantics**: Operations start with verbs (`query`, `upsert`, `insert`, `delete`)
- **Progressive disclosure**: Simple cases are simple; complex features are discovered through chaining
- **Type safety through generics**: `TypeSafeDataSet<T>`, `OperationObjectBuilder<T>` prevent runtime errors
- **Consistent termination**: All builder chains end with `.execute()` or `.connect()`

---

## 1. Syntax Principles and Grammar

### 1.1 The Core Fluent Pattern

All operations follow a predictable structure:

```
EntryPoint → Configuration → Specification → Action → Execution
```

**Example:**
```java
session.upsert(key)          // EntryPoint → Action
    .bin("name").setTo("Tim") // Specification
    .bin("age").setTo(30)     // Specification (chainable)
    .execute();               // Execution (terminal)
```

### 1.2 Builder Chaining Rules

1. **Builders return themselves** for continued chaining (fluent interface)
2. **Terminal operations** return concrete types or void (`.execute()`, `.connect()`)
3. **Sub-builders** navigate deeper into the object graph (`.bin()` → `BinBuilder`)
4. **Parent return** via explicit methods (`.done()` in nested builders like `TlsBuilder`)

### 1.3 Naming Grammar

| Pattern | Usage | Examples |
|---------|-------|----------|
| **Verb-first actions** | Database operations | `query()`, `upsert()`, `insert()`, `update()`, `delete()`, `touch()`, `exists()` |
| **with-prefix** | Additive configuration | `withNativeCredentials()`, `withLogLevel()`, `withNoBins()`, `withNoChangeInExpiration()` |
| **on-prefix** | Targeting/scoping | `onMapKey()`, `onListIndex()`, `onPartition()`, `onAvailablityModeReads()` |
| **of-prefix** | Factory methods | `DataSet.of()`, `TypeSafeDataSet.of()` |
| **ensure-prefix** | Preconditions | `ensureGenerationIs()` |
| **using-prefix** | Dependency injection | `using(RecordMapper)`, `usingServicesAlternate()` |
| **get-prefix** | Accessors | `getNamespace()`, `getSet()`, `getBinNames()` |
| **is-prefix** | Boolean queries | `isConnected()`, `isNamespaceSC()` |

---

## 2. Common Construction Patterns

### 2.1 Connection Lifecycle

```java
// Pattern: ClusterDefinition → configure → connect() → Cluster
Cluster cluster = new ClusterDefinition("localhost", 3100)
    .withNativeCredentials("user", "pass")
    .usingServicesAlternate()
    .preferringRacks(1, 2)
    .validateClusterNameIs("prod-cluster")
    .withTlsConfigOf()
        .tlsName("myTls")
        .caFile("/path/to/ca.pem")
    .done()
    .connect();

// Pattern: Cluster → createSession(Behavior) → Session
Session session = cluster.createSession(Behavior.DEFAULT);
```

**Key Observations:**
- Constructor takes required params (host, port)
- Method chaining for optional config
- Nested builder (`.withTlsConfigOf()...done()`) returns to parent
- Terminal `.connect()` produces `Cluster`

### 2.2 DataSet and Key Creation

```java
// Pattern: DataSet.of(namespace, set)
DataSet users = DataSet.of("production", "users");
TypeSafeDataSet<Customer> customers = TypeSafeDataSet.of("production", "customers", Customer.class);

// Pattern: dataset.id(value) | dataset.ids(values...)
Key userKey = users.id("user123");           // String key
Key numericKey = users.id(12345L);           // Long key
List<Key> keys = users.ids(1, 2, 3, 4, 5);  // Batch keys
```

**Key Observations:**
- Static factory `of()` for immutable-like entities
- Overloaded `id()` for String, int, long, byte[]
- Plural `ids()` with varargs for batch operations

### 2.3 Write Operations

```java
// Pattern: session.action(key) → configure → execute()
session.upsert(key)
    .bin("name").setTo("Alice")
    .bin("age").setTo(25)
    .expireRecordAfter(Duration.ofDays(30))
    .ensureGenerationIs(5)
    .execute();

// Pattern: Multi-key writes
session.upsert(key1, key2, key3)
    .bin("status").setTo("active")
    .execute();

// Pattern: Object mapping
session.upsert(customerDataSet)
    .object(customer)
    .using(customerMapper)  // optional if registered on cluster
    .execute();
```

**Key Observations:**
- CRUD verbs: `insertInto()`, `upsert()`, `update()`, `replace()`, `delete()`
- `bin(name)` returns `BinBuilder` with type-specific setters
- Expiration: `expireRecordAfter()`, `expireRecordAt()`, `neverExpire()`
- Generation checks: `ensureGenerationIs()`

### 2.4 Query Operations

```java
// Pattern: session.query(target) → filter → configure → execute()
RecordStream results = session.query(dataSet)
    .where("$.age > 30 and $.status == 'active'")
    .sortReturnedSubsetBy("name", SortDir.SORT_ASC)
    .limit(100)
    .pageSize(20)
    .onPartitionRange(0, 2048)
    .execute();

// Pattern: Single/batch key reads
RecordStream record = session.query(key).execute();
RecordStream batch = session.query(key1, key2, key3)
    .readingOnlyBins("name", "age")
    .execute();
```

**Key Observations:**
- `where()` accepts DSL string or `BooleanExpression`
- Progressive filtering: `limit()`, `pageSize()`, `onPartition()`
- Bin projection: `readingOnlyBins()` vs `withNoBins()`
- Returns `RecordStream` for iteration

### 2.5 Complex Data Type (CDT) Operations

```java
// Pattern: bin(name).onStructure.action()
session.upsert(key)
    .bin("scores").onMapKey("player1").setTo(9500)
    .bin("scores").onMapKey("player2").update(8200)
    .bin("items").onListIndex(0).remove()
    .execute();

// Pattern: Nested CDT navigation
session.query(key)
    .bin("data")
        .onMapKey("nested")
        .onMapKey("deep")
        .getValues()
    .execute();
```

**Key Observations:**
- Navigation: `onMapKey()`, `onMapIndex()`, `onListIndex()`, `onMapValue()`
- Actions: `setTo()`, `insert()`, `update()`, `remove()`, `getValues()`, `count()`
- Inverted operations: `countAllOthers()`, `removeAllOthers()`
- Return type interfaces enforce legal operations (e.g., no `countAllOthers()` after `onMapIndex()`)

### 2.6 Info and Monitoring

```java
// Pattern: session.info() → command()
InfoCommands info = session.info();
Set<String> namespaces = info.namespaces();
List<Sindex> indexes = info.secondaryIndexes();
Optional<NamespaceDetail> detail = info.namespaceDetails("test");

// Pattern: Per-node vs aggregated results
Map<Node, List<Sindex>> perNode = info.secondaryIndexesPerNode();
List<Sindex> merged = info.secondaryIndexes();
```

**Key Observations:**
- Consistent pair: `foo()` (aggregated) and `fooPerNode()` (split by node)
- Returns `Optional<T>` for single items, `List<T>` for collections
- Overloads: `secondaryIndexDetails(String, String)` and `secondaryIndexDetails(Sindex)`

### 2.7 Behavior Configuration

```java
// Pattern: Behavior.DEFAULT.deriveWithChanges(name, lambda)
Behavior custom = Behavior.DEFAULT.deriveWithChanges("custom", builder ->
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(30))
        .maximumNumberOfCallAttempts(3)
    .done()
    .onQuery()
        .recordQueueSize(10000)
        .maxConcurrentServers(4)
    .done()
);

Session session = cluster.createSession(custom);
```

**Key Observations:**
- Immutable-style derivation: `deriveWithChanges()` returns new instance
- Scoped configuration: `forAllOperations()`, `onQuery()`, `onBatchReads()`
- Nested builder requires `.done()` to pop scope

### 2.8 Transaction Pattern

```java
// Pattern: session.doInTransaction(lambda)
session.doInTransaction(txSession -> {
    Optional<KeyRecord> record = txSession.query(key).execute().getFirst();
    txSession.upsert(key2).bin("total").setTo(record.get().getInt("amount")).execute();
    txSession.delete(key3).execute();
});

// Pattern: Value-returning transactions
String result = session.doInTransaction(txSession -> {
    Record rec = txSession.query(key).execute().next().record;
    return rec.getString("value");
});
```

**Key Observations:**
- Lambdas: `Transactional<T>` (returns value) vs `TransactionalVoid`
- Automatic retry on transient errors (MRT_BLOCKED, MRT_VERSION_MISMATCH)
- Explicit opt-out: `.notInAnyTransaction()` on any operation

---

## 3. Consistency Rules and Naming Conventions

### 3.1 Method Naming Standards

| Category | Pattern | Examples |
|----------|---------|----------|
| **CRUD Operations** | Action verb | `insertInto()`, `upsert()`, `update()`, `replace()`, `delete()`, `touch()`, `exists()` |
| **Queries** | `query()` | Always `query()`, never `read()` or `get()` |
| **Configuration** | `with` prefix | `withNativeCredentials()`, `withLogLevel()`, `withNoBins()` |
| **Targeting** | `on` prefix | `onMapKey()`, `onListIndex()`, `onPartition()` |
| **Factories** | `of()` | `DataSet.of()`, `TypeSafeDataSet.of()` |
| **Termination** | `execute()` or `connect()` | Terminal operations that trigger side effects |
| **Sub-navigation** | Noun without verb | `.bin("name")`, `.bins("a", "b")` |
| **Conditions** | `where()` or `when()` | Filtering and predicates |

### 3.2 Parameter Ordering Conventions

1. **Required first, optional last**: `new ClusterDefinition(host, port)` then optional config
2. **Target before action**: `session.upsert(key).bin("name").setTo(value)`
3. **Varargs for lists**: `ids(1, 2, 3)` instead of `ids(List.of(1,2,3))`
4. **Overloads for types**: `setTo(String)`, `setTo(int)`, `setTo(long)`, etc.
5. **Boolean flags last**: `insert(value, allowFailures)`

### 3.3 Return Type Patterns

| Return Type | Usage | Examples |
|-------------|-------|----------|
| **Same builder** | Continue chaining | `OperationBuilder.bin().setTo().bin().setTo()` |
| **Sub-builder** | Navigate deeper | `.bin("name")` → `BinBuilder` |
| **Terminal concrete** | End of chain | `.execute()` → `RecordStream` |
| **`Optional<T>`** | Nullable single result | `info.namespaceDetails()` → `Optional<NamespaceDetail>` |
| **`List<T>`** | Multiple results | `info.secondaryIndexes()` → `List<Sindex>` |
| **`RecordStream`** | Query results | `session.query().execute()` → `RecordStream` |
| **Self-reference** | Fluent config | `ClusterDefinition.withNativeCredentials()` → `ClusterDefinition` |

### 3.4 Pluralization Rules

- **Singular for single operations**: `bin()`, `id()`, `object()`
- **Plural for batch operations**: `bins()`, `ids()`, `objects()`
- **Plural for getters returning collections**: `namespaces()`, `secondaryIndexes()`, `sets()`
- **Singular for accessors**: `getNamespace()`, `getSet()`, `getCluster()`

### 3.5 Overloading Strategy

The API heavily uses method overloading for:

1. **Type variants**: `id(String)`, `id(int)`, `id(long)`, `id(byte[])`
2. **Single vs batch**: `upsert(Key)` vs `upsert(Key, Key, Key...)`
3. **With/without options**: `insert(value)` vs `insert(value, allowFailures)`
4. **String vs typed DSL**: `where(String)` vs `where(BooleanExpression)`
5. **Convenience overloads**: `secondaryIndexDetails(Sindex)` wraps `secondaryIndexDetails(String, String)`

---

## 4. Detected Deviations or Inconsistencies

### 4.1 Naming Inconsistencies

| Issue | Current State | Recommendation |
|-------|---------------|----------------|
| **Typo in method name** | `preferringRacks()` (correct) vs internal field `preferrredRacks` (typo with triple 'r') | Rename internal field to `preferredRacks` |
| **Inconsistent verb forms** | `insertInto()` vs `upsert()` vs `update()` | Consider standardizing to `insert()`, `upsert()`, `update()` (remove "Into" suffix for consistency) |
| **Per-node suffix variation** | Consistently uses `...PerNode()` suffix | ✅ Good - maintain this |
| **Mixed singular/plural** | `object()` vs `objects()` both present | ✅ Good - intentional for single vs batch |

### 4.2 Parameter Ordering Anomalies

| Method | Current Signature | Consistency Check |
|--------|-------------------|-------------------|
| `sortReturnedSubsetBy()` | Multiple overloads with varied parameter order | ✅ Progressive disclosure pattern - acceptable |
| `id(byte[], int, int)` | Byte array with offset and length | ✅ Matches Java conventions |
| `onMapKey(String, MapOrder)` | Key first, creation policy second | ✅ Target-then-config pattern |

### 4.3 Builder Pattern Deviations

| Issue | Location | Impact |
|-------|----------|--------|
| **Nested builder return** | `TlsBuilder.done()` returns parent `ClusterDefinition` | ✅ Correct - maintains fluent chain |
| **Optional `.using()` method** | `ObjectBuilder.using(RecordMapper)` sometimes required | ⚠️ Can be confusing - document when mapper is auto-discovered vs explicit |
| **Inconsistent `done()` usage** | Only `TlsBuilder` uses `done()`, behavior builders use `.done()` scope markers | ✅ Acceptable - different contexts |

### 4.4 CDT Operation Interface Complexity

**Issue:** Complex interface hierarchy for CDT operations

- `CdtActionInvertableBuilder` vs `CdtActionNonInvertableBuilder`
- `CdtContextInvertableBuilder` vs `CdtContextNonInvertableBuilder`
- `CdtSetterInvertableBuilder` vs `CdtSetterNonInvertableBuilder`

**Analysis:** This complexity is intentional and **correct by design**. It enforces compile-time safety:
- `onMapIndex()` returns non-invertable builder (can't call `.countAllOthers()`)
- `onMapValue()` returns invertable builder (can call `.countAllOthers()`)

**Recommendation:** ✅ Keep this pattern - it's a sophisticated use of the type system to prevent runtime errors.

### 4.5 Transaction API Inconsistency

**Issue:** Two patterns for transactions:

1. Recommended: `session.doInTransaction(lambda)`
2. Advanced: `operation.inTransaction(Txn)` with manual `Txn` management

**Recommendation:** ✅ This is intentional - lambda pattern is primary, explicit `Txn` is escape hatch. Document clearly.

### 4.6 Query vs Batch Read Terminology

**Issue:** `session.query(key)` for single-record reads might confuse users expecting SQL-style queries

**Analysis:**
- `query(DataSet)` → full scan/index query
- `query(Key)` → point lookup
- `query(List<Key>)` → batch read

**Recommendation:** ⚠️ Consider adding aliases:
- `get(Key)` for point reads
- `getBatch(List<Key>)` for batch reads
- Keep `query(DataSet)` for actual queries

Alternative: Rename to clarify intent:
- `read(Key)` / `readBatch(List<Key>)` / `scan(DataSet)`

### 4.7 Exception Handling Patterns

**Issue:** Not explicitly documented in fluent chain

```java
// No visible error handling in fluent API
session.upsert(key).bin("x").setTo(1).execute(); // Can throw AerospikeException
```

**Recommendation:** Document exception behavior consistently:
- Which operations throw checked vs unchecked exceptions
- When `Optional<T>` is used vs exceptions
- Error codes that trigger automatic retry

---

## 5. Recommendations for Future Additions

### 5.1 New Feature Checklist

When adding new capabilities to this fluent API, ensure:

1. **[ ] Verb-first naming** for actions (e.g., `archive()`, `restore()`)
2. **[ ] Builder pattern** if >2 parameters or optional config needed
3. **[ ] Method overloading** for type variants (String, int, long, etc.)
4. **[ ] Terminal `.execute()`** for side-effecting operations
5. **[ ] Return self** for fluent chaining on config methods
6. **[ ] Sub-builder navigation** for complex nested structures
7. **[ ] Consistent with existing verbs**: Don't add `fetch()` when `query()` exists
8. **[ ] Javadoc examples** showing fluent chain usage
9. **[ ] Type safety** through generics where possible
10. **[ ] Immutable derivation** for shared config (like `Behavior`)

### 5.2 Example: Adding a "Batch Upsert with Individual TTLs" Feature

**Bad Design:**
```java
// ❌ Breaks fluent pattern
Map<Key, Bin[]> operations = new HashMap<>();
operations.put(key1, new Bin[]{new Bin("x", 1)});
session.batchUpsertWithTtl(operations, new int[]{300, 400});
```

**Good Design:**
```java
// ✅ Follows fluent conventions
session.upsert(key1, key2)
    .bin("status").setTo("active")
    .forKey(key1).expireRecordAfterSeconds(300)
    .forKey(key2).expireRecordAfterSeconds(400)
    .execute();
```

**Key principles applied:**
- Verb-first: `upsert()`
- Progressive disclosure: Common `.bin()` first, per-key config via `.forKey()`
- Terminal execution: `.execute()`
- Type-safe: Builder prevents mismatched keys

### 5.3 Naming Consistency Matrix for New APIs

When adding new methods, match these patterns:

| If you need to... | Use this pattern | Example |
|-------------------|------------------|---------|
| Add a read operation | `session.query(target).configure().execute()` | `session.query(key).withTimeout(1s).execute()` |
| Add a write operation | `session.action(target).bin().setTo().execute()` | `session.merge(key).bin("data").setTo(x).execute()` |
| Add targeting | `on + Noun` | `.onBackupNode()`, `.onPrimaryOnly()` |
| Add filtering | `.where()` or `.when()` | `.where("$.active == true")` |
| Add configuration | `with + Noun` or `.verb + Noun` | `.withRetryPolicy()`, `.compressPayload()` |
| Add batch variant | Same verb, varargs or List | `upsert(key)` → `upsert(key1, key2, ...)` |
| Return optional result | `Optional<T>` | `findRecord()` → `Optional<Record>` |
| Return multiple results | `List<T>` or `Stream<T>` | `listAll()` → `List<T>` |

### 5.4 Type Safety Enhancements

Future APIs should continue the type safety patterns:

```java
// Good: Enforce correct usage at compile time
DataSet users = DataSet.of("test", "users");
TypeSafeDataSet<Customer> customers = TypeSafeDataSet.of("test", "customers", Customer.class);

// Prevent:
// session.upsert(customers).object(new Order()); // Compile error - wrong type!

// Allow:
session.upsert(customers).object(new Customer()).execute(); // ✅ Type safe
```

### 5.5 Documentation Standards

For every new fluent API:

1. **Method-level Javadoc** with complete fluent chain example
2. **Parameter descriptions** explaining constraints (e.g., "must be > 0")
3. **Return value semantics** (especially for builders)
4. **Exception documentation** - what throws, when, and why
5. **Example code snippets** showing 2-3 use cases (simple, intermediate, advanced)

**Template:**
```java
/**
 * Creates a new X operation for the specified target.
 * 
 * <p>This method initiates a fluent chain for X. The operation
 * is not executed until {@code .execute()} is called.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * session.newOperation(key)
 *     .configure(options)
 *     .bin("field").setTo(value)
 *     .execute();
 * }</pre>
 * 
 * @param target the target for this operation (not null)
 * @return a builder for configuring the operation
 * @throws IllegalArgumentException if target is null
 * @see #execute()
 */
public OperationBuilder newOperation(Key target) { ... }
```

---

## 6. Automated Syntax Validation Rules

For LLM-assisted API generation, enforce these rules:

### 6.1 Linting Rules

```yaml
rules:
  - name: "fluent-methods-return-builder"
    pattern: "public (\\w+Builder) (\\w+)\\(.*\\) \\{.*return this;.*\\}"
    severity: error
    message: "Fluent builder methods must return the builder type"
  
  - name: "terminal-execute"
    pattern: "public (?!\\w+Builder)(\\w+) execute\\(\\)"
    severity: error
    message: "Terminal execute() must not return a builder type"
  
  - name: "verb-first-actions"
    pattern: "public \\w+Builder (insert|upsert|update|delete|query|scan)\\w*\\("
    severity: warning
    message: "Action methods should start with verb"
  
  - name: "with-prefix-config"
    pattern: "public \\w+ with[A-Z]\\w+\\(.*\\).*return this"
    severity: info
    message: "Configuration methods should use 'with' prefix"
```

### 6.2 Code Generation Template

When generating new fluent APIs:

```java
public class {{OperationName}}Builder {
    private final Session session;
    private final Key target;
    // ... state fields
    
    // Package-private constructor
    {{OperationName}}Builder(Session session, Key target) {
        this.session = session;
        this.target = target;
    }
    
    // Fluent configuration methods (return this)
    public {{OperationName}}Builder configure{{Option}}({{Type}} value) {
        this.{{field}} = value;
        return this;
    }
    
    // Sub-builder navigation
    public {{SubBuilder}} {{subElement}}(String name) {
        return new {{SubBuilder}}(this, name);
    }
    
    // Terminal execution
    public {{ResultType}} execute() {
        // Implementation
        return result;
    }
}
```

---

## 7. Summary: The Fluent Grammar

Think of this API as a **domain-specific language** with this grammar:

```
Statement     := Connection | Query | Mutation
Connection    := ClusterDef.configure*.connect()
Query         := Session.query(Target).filter*.configure*.execute()
Mutation      := Session.action(Target).specify*.configure*.execute()

ClusterDef    := new ClusterDefinition(Host+)
Session       := Cluster.createSession(Behavior)
Target        := Key | Key+ | DataSet
Action        := insert | upsert | update | replace | delete | touch | exists
Specify       := bin(Name).operation | onMapKey().operation | ...
Filter        := where(DSL) | sortReturnedSubsetBy() | limit() | ...
Configure     := expireRecordAfter() | ensureGenerationIs() | ...
Execute       := .execute() → RecordStream | RecordResult
Connect       := .connect() → Cluster
```

**Key takeaway:** Every fluent chain follows this grammar. New features should slot into existing production rules rather than inventing new ones.

---

## Appendix A: Quick Reference Cheat Sheet

### Connection
```java
new ClusterDefinition(host, port)
  .withNativeCredentials(user, pass)
  .usingServicesAlternate()
  .preferringRacks(1, 2)
  .validateClusterNameIs(name)
  .withTlsConfigOf().tlsName(name).done()
  .connect()
```

### Session & DataSet
```java
Session session = cluster.createSession(Behavior.DEFAULT);
DataSet ds = DataSet.of(namespace, set);
Key k = ds.id(value);
```

### Write
```java
session.upsert(key)
  .bin("x").setTo(value)
  .expireRecordAfter(duration)
  .execute();
```

### Query
```java
session.query(dataSet)
  .where("$.age > 30")
  .limit(100)
  .execute();
```

### CDT
```java
session.upsert(key)
  .bin("map").onMapKey("k").setTo(v)
  .bin("list").onListIndex(0).remove()
  .execute();
```

### Transaction
```java
session.doInTransaction(tx -> {
  tx.upsert(key).bin("x").setTo(1).execute();
});
```

---

## Appendix B: Anti-Patterns to Avoid

### ❌ Don't: Break fluent chains
```java
// Bad
OperationBuilder builder = session.upsert(key);
builder.bin("x").setTo(1);
builder.bin("y").setTo(2);
RecordStream result = builder.execute();
```
**Why:** Verbose, loses fluent flow

### ✅ Do: Chain methods
```java
// Good
RecordStream result = session.upsert(key)
    .bin("x").setTo(1)
    .bin("y").setTo(2)
    .execute();
```

---

### ❌ Don't: Overload with semantically different operations
```java
// Bad
session.upsert(key).setTo("value");        // String value
session.upsert(key).setTo(bins);           // Array of bins?
```
**Why:** Same method name, completely different semantics

### ✅ Do: Use distinct methods or sub-builders
```java
// Good
session.upsert(key).bin("x").setTo("value").execute();
session.upsert(key).bins("a", "b").values(1, 2).execute();
```

---

### ❌ Don't: Return void from builder methods
```java
// Bad
public void bin(String name, Object value) {
    ops.add(Operation.put(new Bin(name, value)));
}
```
**Why:** Breaks fluent chaining

### ✅ Do: Return builder for chaining
```java
// Good
public OperationBuilder bin(String name) {
    return new BinBuilder(this, name);
}
```

---

## Conclusion

This syntax guide serves as the **grammar specification** for the Aerospike Fluent Client API. When extending the API, new features should read like natural language, follow the established verb-first, builder-pattern conventions, and maintain type safety through generics and interface constraints. The minor inconsistencies identified should be addressed in a future major version, but the overall design is **highly consistent, discoverable, and production-ready**.

**For LLM-assisted development:** Use this guide to generate code that seamlessly integrates with the existing API style, ensuring developers experience a cohesive, intuitive interface regardless of which features they use.

