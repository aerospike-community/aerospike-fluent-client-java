# The Aerospike Fluent Client: A Modern API Design

## Building a More Readable, Maintainable, and Scalable Java Client

---

# Part 1: Fluent APIs — Principles and Patterns

## What is a Fluent API?

A **Fluent API** (also called a Fluent Interface) is an object-oriented API design that relies on method chaining to create code that reads like natural language. The term was coined by Martin Fowler and Eric Evans in 2005.

### Core Characteristics

```java
// Traditional API
QueryBuilder builder = new QueryBuilder();
builder.setNamespace("test");
builder.setSet("users");
builder.setFilter(new AgeFilter(30));
builder.setBins(new String[]{"name", "email"});
Result result = builder.execute();

// Fluent API
Result result = session.query(users)
    .where("$.age == 30")
    .readingOnlyBins("name", "email")
    .execute();
```

The fluent version:
- **Reads like a sentence**: "Query users where age equals 30, reading only name and email bins, then execute"
- **Reduces boilerplate**: No intermediate variable assignments
- **Provides IDE discoverability**: Each method returns an object with contextually relevant methods

---

## The Builder Pattern: Foundation of Fluent APIs

Fluent APIs typically leverage the **Builder Pattern** to construct complex objects step by step:

```java
// Traditional Object Creation
WritePolicy policy = new WritePolicy();
policy.expiration = 3600;
policy.recordExistsAction = RecordExistsAction.UPDATE;
policy.commitLevel = CommitLevel.COMMIT_ALL;

Key key = new Key("test", "users", "alice");
Bin bin1 = new Bin("name", "Alice");
Bin bin2 = new Bin("age", 30);

client.put(policy, key, bin1, bin2);

// Fluent Builder Pattern
session.update(users.id("alice"))
    .bin("name").setTo("Alice")
    .bin("age").setTo(30)
    .expireRecordAfter(Duration.ofHours(1))
    .execute();
```

### Benefits of the Builder Pattern

| Benefit | Description |
|---------|-------------|
| **Immutability** | The final object can be immutable; the builder handles mutable state |
| **Validation** | Validate all parameters at build time, not at use time |
| **Optional Parameters** | Only set what you need; defaults handle the rest |
| **Self-Documentation** | Method names describe what they configure |

---

## Advantages of Fluent APIs

### 1. Readability

Code reads like natural language, making intent immediately clear:

```java
// What does this do?
client.put(wp, new Key("test", "users", id), 
    new Bin("name", name), new Bin("age", age), new Bin("ts", System.currentTimeMillis()));

// vs. What does THIS do?
session.insert(users.id(id))
    .bin("name").setTo(name)
    .bin("age").setTo(age)
    .bin("ts").setTo(System.currentTimeMillis())
    .execute();
```

### 2. IDE Discoverability

Each method returns an object with contextually appropriate methods:

```
session.query(users)
    .         ← IDE shows: where(), limit(), readingOnlyBins(), onPartitionRange(), execute()
    .where("$.age > 21")
    .         ← IDE shows: and(), or(), limit(), execute()
```

### 3. Compile-Time Safety

Invalid combinations can be prevented at compile time:

```java
// Our API prevents invalid combinations
session.query(users)
    .where("$.age > 21")
    .limit(100)          // ✓ Valid on queries
    .execute();

session.insert(users.id("alice"))
    .limit(100)          // ✗ Compile error - limit() not available on insert
    .execute();
```

### 4. Reduced Cognitive Load

Developers don't need to remember:
- Which policy object to use (`WritePolicy` vs `QueryPolicy` vs `BatchPolicy`)
- Constructor parameter order
- Which combinations are valid

The API guides them to correct usage.

---

## Disadvantages and Challenges

### 1. Stack Traces Can Be Harder to Read

```
Exception in thread "main" com.aerospike.exception.RecordNotFoundException
    at com.aerospike.RecordResult.orThrow(RecordResult.java:91)
    at com.aerospike.RecordStream.getFirstRecord(RecordStream.java:288)
    at com.example.UserService.getUser(UserService.java:45)
```

**Mitigation**: We provide cleaned stack traces that remove internal builder frames.

### 2. Debugging Can Be Trickier

Long method chains make it harder to set breakpoints on intermediate steps.

**Mitigation**: Allow breaking chains with intermediate variables when debugging:
```java
var queryBuilder = session.query(users).where("$.age > 21");
// Breakpoint here
var results = queryBuilder.execute();
```

### 3. API Design Requires More Upfront Effort

Designing a good fluent API requires careful thought about:
- Method naming consistency
- Return types at each step
- Which methods are valid in which contexts

---

## Testability and Mockability

### Traditional API Testing

```java
@Test
public void testUserCreation() {
    // Must mock multiple objects
    AerospikeClient client = mock(AerospikeClient.class);
    WritePolicy policy = new WritePolicy();
    Key key = new Key("test", "users", "alice");
    
    // Complex verification
    verify(client).put(
        argThat(p -> p.expiration == 3600),
        eq(key),
        argThat(bins -> bins[0].name.equals("name"))
    );
}
```

### Fluent API Testing

```java
@Test
public void testUserCreation() {
    // Mock the session
    Session session = mock(Session.class);
    OperationBuilder mockBuilder = mock(OperationBuilder.class, RETURNS_SELF);
    
    when(session.insert(any())).thenReturn(mockBuilder);
    when(mockBuilder.execute()).thenReturn(new RecordStream());
    
    // Test
    userService.createUser("alice", "Alice");
    
    // Verify the chain was called
    verify(session).insert(any());
    verify(mockBuilder).execute();
}
```

### Key Insight: Mock at the Right Level

The fluent API allows mocking at the **Session level**, which is the natural boundary between business logic and database operations. You don't need to mock low-level policy objects.

---

## Design Patterns in Fluent APIs

### 1. Method Chaining

Each method returns `this` or a related builder:

```java
public class OperationBuilder {
    public OperationBuilder bin(String name) {
        // Configure bin...
        return this;  // Returns self for chaining
    }
    
    public OperationBuilder expireRecordAfter(Duration duration) {
        this.expiration = duration.getSeconds();
        return this;
    }
}
```

### 2. Context-Aware Return Types

Different methods can return different builder types to limit available operations:

```java
public interface KeyBasedQueryBuilder {
    // Available for key-based queries
    KeyBasedQueryBuilder readingOnlyBins(String... bins);
    RecordStream execute();
}

public interface IndexBasedQueryBuilder extends KeyBasedQueryBuilder {
    // Additional methods for index-based queries
    IndexBasedQueryBuilder where(String expression);
    IndexBasedQueryBuilder limit(int count);
    IndexBasedQueryBuilder onPartitionRange(int start, int end);
}
```

### 3. Terminal Methods

The chain ends with a terminal method that executes the operation:

```java
session.query(users)
    .where("$.age > 21")
    .limit(100)
    .execute();      // Terminal method - executes the query
                     // No more chaining after this
```

---

# Part 2: The Aerospike Fluent Client

## Design Philosophy

The Aerospike Fluent Client was designed with several core principles:

1. **Reads Like English**: Operations should be understandable at a glance
2. **Progressive Disclosure**: Simple things should be simple; complex things should be possible
3. **Type Safety**: Catch errors at compile time, not runtime
4. **Sensible Defaults**: The common case should require minimal configuration
5. **Hierarchical Configuration**: Global settings cascade down, specific settings override

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           User Application                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ClusterDefinition ──► Cluster ──► Session ──► Builders ──► Results   │
│         │                  │           │           │            │        │
│   • Host config        • Connection  • Behavior  • Fluent    • RecordStream
│   • Credentials        • Indexes     • Txn state   chains    • RecordResult
│   • System settings    • Mapping                             • Objects    │
│                                                                          │
├─────────────────────────────────────────────────────────────────────────┤
│                       Underlying Aerospike Java Client                   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Key Abstractions

### 1. ClusterDefinition → Cluster

**Traditional:**
```java
ClientPolicy policy = new ClientPolicy();
policy.user = "username";
policy.password = "password";
policy.minConnsPerNode = 10;
policy.maxConnsPerNode = 100;
policy.timeout = 5000;
policy.failIfNotConnected = true;

Host[] hosts = new Host[] {
    new Host("node1.example.com", 3000),
    new Host("node2.example.com", 3000)
};

AerospikeClient client = new AerospikeClient(policy, hosts);

// ... use client ...

// Don't forget to close!
client.close();
```

**Fluent:**
```java
try (Cluster cluster = new ClusterDefinition("node1.example.com", 3000)
        .withNativeCredentials("username", "password")
        .withSystemSettings(settings -> settings
            .connections(conn -> conn
                .minimumConnectionsPerNode(10)
                .maximumConnectionsPerNode(100)
            )
            .timeouts(t -> t.connectionTimeout(Duration.ofSeconds(5)))
        )
        .failIfUnableToConnect()
        .connect()) {
    
    // Use cluster...
    
} // Automatically closed!
```

**Improvements:**
- Builder pattern with descriptive method names
- Nested configuration for related settings
- Implements `Closeable` for try-with-resources
- Methods reveal available options

### 2. DataSet — The Namespace/Set Abstraction

**Traditional:**
```java
// Namespace and set repeated everywhere
Key key1 = new Key("ecommerce", "products", "prod123");
Key key2 = new Key("ecommerce", "products", "prod456");
Key key3 = new Key("ecommerce", "products", "prod789");

// Easy to make typos
Key wrong = new Key("ecommerse", "products", "prod000");  // Typo not caught!
```

**Fluent:**
```java
// Define once
DataSet products = DataSet.of("ecommerce", "products");

// Reuse everywhere
Key key1 = products.id("prod123");
Key key2 = products.id("prod456");
Key key3 = products.id("prod789");

// Batch keys made easy
List<Key> keys = products.ids("prod123", "prod456", "prod789");

// Type-safe version for object mapping
TypeSafeDataSet<Product> typedProducts = 
    TypeSafeDataSet.of("ecommerce", "products", Product.class);
```

**Improvements:**
- Single source of truth for namespace/set
- No repeated string literals
- Type-safe key generation
- Support for different key types (String, int, long, byte[])
- Batch key creation

### 3. Session — The Operation Context

**Traditional:**
```java
// Policies scattered everywhere
WritePolicy writePolicy = new WritePolicy();
writePolicy.setTimeout(1000);
writePolicy.maxRetries = 3;

QueryPolicy queryPolicy = new QueryPolicy();
queryPolicy.setTimeout(5000);
queryPolicy.recordsPerSecond = 1000;

BatchPolicy batchPolicy = new BatchPolicy();
batchPolicy.setTimeout(2000);

// Each operation needs explicit policy
client.put(writePolicy, key, bins);
client.get(readPolicy, key);
client.query(queryPolicy, statement);
```

**Fluent:**
```java
// Configure once
Behavior productionBehavior = Behavior.DEFAULT.deriveWithChanges("production", b -> b
    .on(Selectors.all(), ops -> ops
        .abandonCallAfter(Duration.ofSeconds(5))
        .maximumNumberOfCallAttempts(3)
    )
    .on(Selectors.reads().query(), ops -> ops
        .waitForCallToComplete(Duration.ofSeconds(30))
    )
);

// Create session with behavior
Session session = cluster.createSession(productionBehavior);

// All operations use consistent settings
session.upsert(products.id("prod123")).bin("name").setTo("Widget").execute();
session.query(products).where("$.price > 100").execute();
```

**Improvements:**
- Centralized policy configuration
- Consistent settings across operations
- Hierarchical overrides (global → category → specific)
- Session carries context (behavior, transaction state)

---

## Operation Builders: The Heart of the API

### Writing Records

**Traditional:**
```java
Key key = new Key("test", "users", "alice");

WritePolicy writePolicy = new WritePolicy();
writePolicy.recordExistsAction = RecordExistsAction.CREATE_ONLY;
writePolicy.expiration = 86400;
writePolicy.generation = 5;
writePolicy.generationPolicy = GenerationPolicy.EXPECT_GEN_EQUAL;

Bin bin1 = new Bin("name", "Alice Johnson");
Bin bin2 = new Bin("email", "alice@example.com");
Bin bin3 = new Bin("age", 30);
Bin bin4 = new Bin("created", System.currentTimeMillis());
Bin bin5 = new Bin("tags", Arrays.asList("premium", "verified"));

try {
    client.put(writePolicy, key, bin1, bin2, bin3, bin4, bin5);
} catch (AerospikeException.KeyExists e) {
    throw new UserAlreadyExistsException("User alice already exists");
}
```

**Fluent:**
```java
DataSet users = DataSet.of("test", "users");

session.insert(users.id("alice"))
    .bin("name").setTo("Alice Johnson")
    .bin("email").setTo("alice@example.com")
    .bin("age").setTo(30)
    .bin("created").setTo(System.currentTimeMillis())
    .bin("tags").setTo(List.of("premium", "verified"))
    .expireRecordAfter(Duration.ofDays(1))
    .ensureGenerationIs(5)
    .execute();
```

**Comparison:**

| Aspect | Traditional | Fluent |
|--------|-------------|--------|
| Lines of code | 15+ | 9 |
| Intermediate objects | 7 (policy, key, 5 bins) | 0 |
| Intent clarity | Low (must read all code) | High (reads naturally) |
| Type safety | Runtime errors | Compile-time checks |
| IDE support | Minimal | Full autocomplete |

### Reading Records

**Traditional (Single Key):**
```java
Key key = new Key("test", "users", "alice");

Policy readPolicy = new Policy();
readPolicy.setTimeout(1000);

Record record = client.get(readPolicy, key, "name", "email");

if (record != null) {
    String name = record.getString("name");
    String email = record.getString("email");
    System.out.println("User: " + name + " (" + email + ")");
} else {
    System.out.println("User not found");
}
```

**Fluent (Single Key):**
```java
DataSet users = DataSet.of("test", "users");

session.query(users.id("alice"))
    .readingOnlyBins("name", "email")
    .execute()
    .getFirst()
    .ifPresentOrElse(
        record -> System.out.println("User: " + 
            record.recordOrThrow().getString("name") + " (" + 
            record.recordOrThrow().getString("email") + ")"),
        () -> System.out.println("User not found")
    );
```

**Traditional (Batch):**
```java
Key[] keys = new Key[] {
    new Key("test", "users", "alice"),
    new Key("test", "users", "bob"),
    new Key("test", "users", "carol")
};

BatchPolicy batchPolicy = new BatchPolicy();
batchPolicy.setTimeout(2000);

Record[] records = client.get(batchPolicy, keys, "name", "email");

for (int i = 0; i < records.length; i++) {
    if (records[i] != null) {
        System.out.println(keys[i].userKey + ": " + records[i].getString("name"));
    } else {
        System.out.println(keys[i].userKey + ": NOT FOUND");
    }
}
```

**Fluent (Batch):**
```java
DataSet users = DataSet.of("test", "users");

session.query(users.ids("alice", "bob", "carol"))
    .readingOnlyBins("name", "email")
    .execute()
    .forEach(result -> {
        String id = result.key().userKey.toString();
        if (result.isOk()) {
            System.out.println(id + ": " + result.recordOrThrow().getString("name"));
        } else {
            System.out.println(id + ": NOT FOUND");
        }
    });
```

### Querying with Filters

**Traditional:**
```java
Statement statement = new Statement();
statement.setNamespace("test");
statement.setSetName("users");
statement.setFilter(Filter.range("age", 21, 65));
statement.setBinNames("name", "email", "age");

QueryPolicy queryPolicy = new QueryPolicy();
queryPolicy.setTimeout(30000);
queryPolicy.maxRecords = 1000;
queryPolicy.recordsPerSecond = 500;

RecordSet recordSet = null;
try {
    recordSet = client.query(queryPolicy, statement);
    
    while (recordSet.next()) {
        Key key = recordSet.getKey();
        Record record = recordSet.getRecord();
        
        String name = record.getString("name");
        int age = record.getInt("age");
        
        System.out.println(name + " (age " + age + ")");
    }
} finally {
    if (recordSet != null) {
        recordSet.close();
    }
}
```

**Fluent:**
```java
DataSet users = DataSet.of("test", "users");

session.query(users)
    .where("$.age >= 21 and $.age <= 65")
    .readingOnlyBins("name", "email", "age")
    .limit(1000)
    .recordsPerSecond(500)
    .execute()
    .forEach(result -> {
        String name = result.recordOrThrow().getString("name");
        int age = result.recordOrThrow().getInt("age");
        System.out.println(name + " (age " + age + ")");
    });
```

**Improvements:**
- No `Statement` or `Filter` objects to construct
- Human-readable DSL for filters: `$.age >= 21`
- Automatic resource management
- Stream-compatible results

---

## The Behavior System: Hierarchical Policy Management

### The Problem with Traditional Policies

In the traditional API, you must create and manage policy objects for every operation:

```java
// You need different policies for different operations
WritePolicy writePolicy = client.copyWritePolicyDefault();
writePolicy.setTimeout(1000);
writePolicy.maxRetries = 2;

QueryPolicy queryPolicy = client.copyQueryPolicyDefault();
queryPolicy.setTimeout(30000);
queryPolicy.maxRetries = 1;

BatchPolicy batchPolicy = client.copyBatchPolicyDefault();
batchPolicy.setTimeout(5000);
batchPolicy.maxRetries = 3;

// And then you must remember to use the right one:
client.put(writePolicy, key, bins);       // What if you use wrong policy?
client.query(queryPolicy, statement);
client.get(batchPolicy, keys);
```

### The Behavior Solution

Behaviors provide a **hierarchical, declarative approach** to policy configuration:

```java
Behavior production = Behavior.DEFAULT.deriveWithChanges("production", builder -> builder
    // Global settings for ALL operations
    .on(Selectors.all(), ops -> ops
        .abandonCallAfter(Duration.ofSeconds(5))
        .maximumNumberOfCallAttempts(3)
    )
    
    // Override for all READ operations
    .on(Selectors.reads(), ops -> ops
        .waitForCallToComplete(Duration.ofSeconds(2))
    )
    
    // Specific override for batch reads
    .on(Selectors.reads().batch(), ops -> ops
        .maxConcurrentNodes(8)
        .allowInlineMemoryAccess(true)
    )
    
    // Specific override for queries
    .on(Selectors.reads().query(), ops -> ops
        .waitForCallToComplete(Duration.ofSeconds(30))
    )
    
    // Override for writes
    .on(Selectors.writes().retryable(), ops -> ops
        .commitLevel(CommitLevel.COMMIT_ALL)
    )
);

// Create session - all operations automatically use correct policies
Session session = cluster.createSession(production);

// These all use the right policies automatically!
session.upsert(key).bin("name").setTo("Alice").execute();  // Uses write policy
session.query(key).execute();                               // Uses read policy
session.query(users.ids("a", "b", "c")).execute();         // Uses batch policy
session.query(users).where("$.age > 21").execute();        // Uses query policy
```

### Behavior Inheritance

Behaviors form a hierarchy, allowing environment-specific overrides:

```java
// Base behavior with sensible defaults
Behavior base = Behavior.DEFAULT.deriveWithChanges("base", b -> b
    .on(Selectors.all(), ops -> ops
        .abandonCallAfter(Duration.ofSeconds(10))
        .maximumNumberOfCallAttempts(3)
    )
);

// Development: more lenient timeouts
Behavior development = base.deriveWithChanges("development", b -> b
    .on(Selectors.all(), ops -> ops
        .abandonCallAfter(Duration.ofSeconds(30))
    )
);

// Production: stricter settings
Behavior production = base.deriveWithChanges("production", b -> b
    .on(Selectors.all(), ops -> ops
        .abandonCallAfter(Duration.ofSeconds(5))
        .maximumNumberOfCallAttempts(5)
    )
);

// Production with high load: even stricter
Behavior productionHighLoad = production.deriveWithChanges("productionHighLoad", b -> b
    .on(Selectors.reads().batch(), ops -> ops
        .maxConcurrentNodes(16)
    )
);
```

### Dynamic Configuration from YAML

Behaviors can be loaded from YAML files and reloaded at runtime:

```yaml
behaviors:
  production:
    parent: DEFAULT
    allOperations:
      abandonCallAfter: 5s
      maximumNumberOfCallAttempts: 3
    reads:
      waitForCallToComplete: 2s
    queries:
      waitForCallToComplete: 30s
```

```java
// Load and monitor for changes
Behavior.startMonitoring("config/behaviors.yaml");

// Get behavior by name - always returns latest version
Behavior production = Behavior.getBehavior("production");
Session session = cluster.createSession(production);
```

---

## The DSL: Human-Readable Filters

### Traditional Filter Construction

```java
// Simple filter
Filter filter = Filter.equal("status", "active");

// Range filter
Filter filter = Filter.range("age", 21, 65);

// Expression filter (for complex conditions)
Expression exp = Exp.build(
    Exp.and(
        Exp.ge(Exp.intBin("age"), Exp.val(21)),
        Exp.le(Exp.intBin("age"), Exp.val(65)),
        Exp.or(
            Exp.eq(Exp.stringBin("status"), Exp.val("active")),
            Exp.eq(Exp.stringBin("status"), Exp.val("pending"))
        )
    )
);

QueryPolicy policy = new QueryPolicy();
policy.filterExp = exp;
```

### Fluent DSL

```java
// Simple equality
session.query(users).where("$.status == 'active'").execute();

// Range
session.query(users).where("$.age >= 21 and $.age <= 65").execute();

// Complex conditions - reads like English!
session.query(users)
    .where("$.age >= 21 and $.age <= 65 and ($.status == 'active' or $.status == 'pending')")
    .execute();

// With variables for dynamic values
int minAge = 21;
int maxAge = 65;
session.query(users)
    .where("$.age >= " + minAge + " and $.age <= " + maxAge)
    .execute();
```

### DSL Features

| Feature | Example |
|---------|---------|
| String comparison | `$.name == 'Alice'` |
| Numeric comparison | `$.age >= 21` |
| Boolean logic | `$.active == true` |
| AND/OR | `$.age > 21 and $.verified == true` |
| Parentheses | `($.a == 1 or $.b == 2) and $.c == 3` |
| Nested access | `$.address.city == 'NYC'` |
| List contains | `$.tags contains 'premium'` |
| String matching | `$.name like 'A%'` |

---

## Object Mapping: Type-Safe CRUD

### Traditional Approach

```java
// Manual mapping - error-prone and verbose
public User getUser(String id) {
    Key key = new Key("test", "users", id);
    Record record = client.get(null, key);
    
    if (record == null) return null;
    
    return new User(
        id,
        record.getString("name"),
        record.getInt("age"),
        record.getString("email"),
        record.getLong("createdAt"),
        record.getBoolean("active"),
        (List<String>) record.getList("tags")
    );
}

public void saveUser(User user) {
    Key key = new Key("test", "users", user.getId());
    
    Bin[] bins = new Bin[] {
        new Bin("name", user.getName()),
        new Bin("age", user.getAge()),
        new Bin("email", user.getEmail()),
        new Bin("createdAt", user.getCreatedAt()),
        new Bin("active", user.isActive()),
        new Bin("tags", user.getTags())
    };
    
    client.put(null, key, bins);
}
```

### Fluent Approach with RecordMapper

```java
// Define mapper once
public class UserMapper implements RecordMapper<User> {
    @Override
    public User toObject(Key key, Record record) {
        return new User(
            key.userKey.toString(),
            record.getString("name"),
            record.getInt("age"),
            record.getString("email"),
            record.getLong("createdAt"),
            record.getBoolean("active"),
            record.getList("tags")
        );
    }
    
    @Override
    public Map<String, Value> toMap(User user) {
        return Map.of(
            "name", Value.get(user.getName()),
            "age", Value.get(user.getAge()),
            "email", Value.get(user.getEmail()),
            "createdAt", Value.get(user.getCreatedAt()),
            "active", Value.get(user.isActive()),
            "tags", Value.get(user.getTags())
        );
    }
    
    @Override
    public Object getKey(User user) {
        return user.getId();
    }
}

// Register mapper
cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(
    Map.of(User.class, new UserMapper())
));

// Type-safe dataset
TypeSafeDataSet<User> users = TypeSafeDataSet.of("test", "users", User.class);

// Usage is clean and type-safe
session.insert(users).object(user).execute();

Optional<User> user = session.query(users.id("alice"))
    .execute()
    .getFirst(userMapper);

List<User> activeUsers = session.query(users)
    .where("$.active == true")
    .execute()
    .toObjectList(userMapper);
```

---

## CDT Operations: Fluent List and Map Manipulation

### Traditional List Operations

```java
Key key = new Key("test", "products", "prod123");

// Append to list
Operation appendOp = ListOperation.append(
    ListPolicy.Default,
    "tags",
    Value.get("new-tag")
);
client.operate(null, key, appendOp);

// Get list size
Operation sizeOp = ListOperation.size("tags");
Record result = client.operate(null, key, sizeOp);
long size = result.getLong("tags");

// Get range
Operation rangeOp = ListOperation.getRange("tags", 0, 5);
Record result = client.operate(null, key, rangeOp);
List<?> items = result.getList("tags");

// Remove by value
Operation removeOp = ListOperation.removeByValue(
    "tags", 
    Value.get("obsolete-tag"),
    ListReturnType.COUNT
);
client.operate(null, key, removeOp);
```

### Fluent List Operations

```java
DataSet products = DataSet.of("test", "products");

// Append to list
session.update(products.id("prod123"))
    .onList("tags").append("new-tag")
    .execute();

// Get list size
long size = session.query(products.id("prod123"))
    .onList("tags").listSize()
    .execute()
    .getFirstRecord()
    .getLong("tags");

// Get range
List<String> items = session.query(products.id("prod123"))
    .onList("tags").getByIndexRange(0, 5)
    .execute()
    .getFirstRecord()
    .getList("tags");

// Remove by value
session.update(products.id("prod123"))
    .onList("tags").removeByValue("obsolete-tag")
    .execute();

// Multiple operations in one call
session.update(products.id("prod123"))
    .onList("tags").append("featured")
    .onList("tags").removeByValue("obsolete")
    .onList("views").add(1)
    .bin("lastModified").setTo(System.currentTimeMillis())
    .execute();
```

### Traditional Map Operations

```java
Key key = new Key("test", "products", "prod123");

// Set a map value
Operation putOp = MapOperation.put(
    MapPolicy.Default,
    "attributes",
    Value.get("color"),
    Value.get("red")
);
client.operate(null, key, putOp);

// Get value by key
Operation getOp = MapOperation.getByKey(
    "attributes",
    Value.get("color"),
    MapReturnType.VALUE
);
Record result = client.operate(null, key, getOp);
Object color = result.getValue("attributes");

// Remove by key
Operation removeOp = MapOperation.removeByKey(
    "attributes",
    Value.get("obsolete-key"),
    MapReturnType.NONE
);
client.operate(null, key, removeOp);

// Increment a numeric value in map
Operation incrOp = MapOperation.increment(
    MapPolicy.Default,
    "stats",
    Value.get("views"),
    Value.get(1)
);
client.operate(null, key, incrOp);

// Get entries by value range
Operation rangeOp = MapOperation.getByValueRange(
    "prices",
    Value.get(10.0),
    Value.get(50.0),
    MapReturnType.KEY_VALUE
);
Record result = client.operate(null, key, rangeOp);
```

### Fluent Map Operations

```java
DataSet products = DataSet.of("test", "products");

// Set a map value
session.update(products.id("prod123"))
    .onMapKey("attributes", "color").setTo("red")
    .execute();

// Set multiple map entries at once
session.update(products.id("prod123"))
    .onMapKey("attributes", "size").setTo("large")
    .onMapKey("attributes", "weight").setTo(5.2)
    .onMapKey("attributes", "inStock").setTo(true)
    .execute();

// Get value by key
Object color = session.query(products.id("prod123"))
    .onMapKey("attributes", "color").get()
    .execute()
    .getFirstRecord()
    .getValue("attributes");

// Remove by key
session.update(products.id("prod123"))
    .onMapKey("attributes", "obsolete-key").remove()
    .execute();

// Increment a numeric value in map
session.update(products.id("prod123"))
    .onMapKey("stats", "views").add(1)
    .execute();

// Get entries by value range
session.query(products.id("prod123"))
    .onMap("prices").byValueRange(10.0, 50.0).getKeysAndValues()
    .execute();

// Complex: update map, increment counter, set timestamp - all atomic
session.update(products.id("prod123"))
    .onMapKey("attributes", "lastViewed").setTo(System.currentTimeMillis())
    .onMapKey("stats", "views").add(1)
    .onMapKey("stats", "uniqueVisitors").add(1)
    .bin("lastModified").setTo(System.currentTimeMillis())
    .execute();
```

### Compile-Time Safety: Preventing Invalid Operations

One of the most powerful features of the fluent API is preventing invalid operations at **compile time** rather than discovering them at **runtime**.

**The Problem: Non-Invertible Operations**

In Aerospike CDT operations, some operations support "inverted" results (e.g., "get all items EXCEPT this one"), while others don't. Single-item selections like "get by index" or "get by key" cannot be inverted — it makes no semantic sense to say "get all items except item at index 5."

**Traditional API: Runtime Error**

```java
Key key = new Key("test", "products", "prod123");

// This compiles fine, but fails at RUNTIME with a ParameterError!
Operation badOp = MapOperation.getByIndex(
    "myMap",
    5,                                              // Single index
    MapReturnType.COUNT | MapReturnType.INVERTED    // ← INVALID combination!
);

try {
    client.operate(null, key, badOp);
} catch (AerospikeException e) {
    // Runtime error: "Parameter error" from server
    // You only discover this in production!
}
```

**Fluent API: Compile-Time Error**

The fluent API uses **interface segregation** to prevent this at compile time:

```java
// Single-item selection returns CdtActionNonInvertableBuilder
CdtActionNonInvertableBuilder singleItem = session.query(products.id("prod123"))
    .onMap("myMap").byIndex(5);

// Available methods:
singleItem.get();          // ✓ Valid
singleItem.getKey();       // ✓ Valid  
singleItem.getValue();     // ✓ Valid
singleItem.remove();       // ✓ Valid
// singleItem.getAllOthers();    // ✗ Method doesn't exist! Compile error!
// singleItem.countAllOthers();  // ✗ Method doesn't exist! Compile error!
// singleItem.removeAllOthers(); // ✗ Method doesn't exist! Compile error!

// Range selection returns CdtActionInvertableBuilder  
CdtActionInvertableBuilder rangeItems = session.query(products.id("prod123"))
    .onMap("myMap").byIndexRange(0, 10);

// All methods available:
rangeItems.getValues();        // ✓ Valid - get items in range
rangeItems.getAllOtherValues();// ✓ Valid - get items NOT in range (inverted)
rangeItems.count();            // ✓ Valid - count items in range
rangeItems.countAllOthers();   // ✓ Valid - count items NOT in range (inverted)
rangeItems.remove();           // ✓ Valid - remove items in range
rangeItems.removeAllOthers();  // ✓ Valid - remove items NOT in range (inverted)
```

**The Design Pattern: Interface Segregation**

```java
// Non-invertible operations
public interface CdtActionNonInvertableBuilder {
    OperationBuilder getValues();
    OperationBuilder getKeys();
    OperationBuilder count();
    OperationBuilder remove();
    // Note: NO inverted methods here!
}

// Invertible operations extend non-invertible
public interface CdtActionInvertableBuilder extends CdtActionNonInvertableBuilder {
    OperationBuilder getAllOtherValues();   // Inverted operations
    OperationBuilder getAllOtherKeys();
    OperationBuilder countAllOthers();
    OperationBuilder removeAllOthers();
}

// Single-item selection returns non-invertible interface
public CdtActionNonInvertableBuilder byIndex(int index) { ... }
public CdtActionNonInvertableBuilder byKey(Object key) { ... }
public CdtActionNonInvertableBuilder byRank(int rank) { ... }

// Range selection returns invertible interface
public CdtActionInvertableBuilder byIndexRange(int start, int count) { ... }
public CdtActionInvertableBuilder byKeyRange(Object start, Object end) { ... }
public CdtActionInvertableBuilder byValueRange(Object start, Object end) { ... }
```

**Benefits:**

| Aspect | Traditional API | Fluent API |
|--------|-----------------|------------|
| Invalid operation | Compiles successfully | Compile error |
| Error discovery | Runtime (production) | Development time |
| Error message | Cryptic "ParameterError" | "Method not found" |
| Developer experience | Trial and error | IDE guides to valid options |

This is a perfect example of the fluent API philosophy: **make invalid states unrepresentable**.

---

## RecordStream: Modern Result Processing

### Traditional Result Processing

```java
// Query results
Statement stmt = new Statement();
stmt.setNamespace("test");
stmt.setSetName("users");

RecordSet rs = null;
List<User> users = new ArrayList<>();

try {
    rs = client.query(null, stmt);
    while (rs.next()) {
        Key key = rs.getKey();
        Record record = rs.getRecord();
        users.add(mapToUser(key, record));
    }
} finally {
    if (rs != null) {
        rs.close();  // Must remember to close!
    }
}
```

### Fluent RecordStream

```java
// Same query, more options
RecordStream results = session.query(users).execute();

// Option 1: forEach
results.forEach(r -> processUser(r));

// Option 2: Convert to Java Stream
List<String> names = session.query(users)
    .execute()
    .stream()
    .filter(r -> r.recordOrThrow().getInt("age") > 21)
    .map(r -> r.recordOrThrow().getString("name"))
    .collect(Collectors.toList());

// Option 3: Get first result
Optional<RecordResult> first = session.query(users.id("alice"))
    .execute()
    .getFirst();

// Option 4: Convert to objects
List<User> allUsers = session.query(users)
    .execute()
    .toObjectList(userMapper);

// Option 5: Navigatable stream with sorting and pagination
NavigatableRecordStream nav = session.query(users)
    .limit(1000)
    .execute()
    .asNavigatableStream()
    .sortBy(SortProperties.descending("age"))
    .pageSize(20);

while (nav.hasMorePages()) {
    nav.forEach(r -> displayUser(r));
    nav.nextPage();
}
```

### RecordResult: Rich Result Information

Unlike the traditional API which returns `null` for missing records:

```java
// Traditional - null means either "not found" or "error"
Record record = client.get(null, key);
if (record == null) {
    // Is it not found? Or was there an error?
}

// Fluent - RecordResult provides full context
RecordResult result = session.query(key).execute().next();

if (result.isOk()) {
    Record record = result.recordOrThrow();
} else {
    // Know exactly what happened
    switch (result.resultCode()) {
        case ResultCode.KEY_NOT_FOUND_ERROR:
            System.out.println("Record doesn't exist");
            break;
        case ResultCode.TIMEOUT:
            System.out.println("Operation timed out");
            break;
        default:
            System.out.println("Error: " + result.message());
    }
}
```

---

## Transactions: First-Class Support

### Traditional Transaction Handling

```java
Txn txn = new Txn();
try {
    WritePolicy wp = new WritePolicy();
    wp.txn = txn;
    
    // Perform operations
    client.put(wp, key1, bins1);
    client.put(wp, key2, bins2);
    
    // Commit
    client.commit(txn);
} catch (Exception e) {
    client.abort(txn);
    throw e;
}
```

### Fluent Transactions

```java
try (TransactionalSession tx = cluster.createTransactionalSession(behavior)) {
    
    // Read within transaction
    double balance1 = tx.query(accounts.id("acc1"))
        .execute()
        .getFirstRecord()
        .getDouble("balance");
    
    double balance2 = tx.query(accounts.id("acc2"))
        .execute()
        .getFirstRecord()
        .getDouble("balance");
    
    // Validate
    if (balance1 < amount) {
        throw new InsufficientFundsException();
    }
    
    // Write within transaction
    tx.update(accounts.id("acc1"))
        .bin("balance").setTo(balance1 - amount)
        .execute();
    
    tx.update(accounts.id("acc2"))
        .bin("balance").setTo(balance2 + amount)
        .execute();
    
    tx.commit();  // Atomic commit
    
} // Automatic abort on exception or if not committed
```

---

## Clever Design Decisions

### 1. Automatic Batch vs Point Operation Selection

The API automatically chooses the optimal implementation:

```java
// Single key → uses point read
session.query(users.id("alice")).execute();

// Multiple keys → automatically uses batch
session.query(users.ids("alice", "bob", "carol")).execute();

// DataSet → uses scan/query
session.query(users).execute();
```

The developer doesn't need to know about `BatchPolicy`, `BatchRead`, or choose between `client.get()` and `client.get(keys)`.

### 2. Context-Aware Method Availability

The API uses interface segregation to show only relevant methods:

```java
// QueryBuilder has different capabilities based on context
IndexBasedQueryBuilder datasetQuery = session.query(users);
datasetQuery.where("$.age > 21");     // ✓ Available for dataset queries
datasetQuery.limit(100);               // ✓ Available
datasetQuery.onPartitionRange(0, 2048);// ✓ Available

KeyBasedQueryBuilder keyQuery = session.query(users.id("alice"));
// keyQuery.where(...);                // ✗ Not available - makes no sense for key lookup
keyQuery.readingOnlyBins("name");      // ✓ Available
```

### 3. Sensible Defaults with Easy Overrides

```java
// Default behavior works for most cases
session.upsert(key)
    .bin("name").setTo("Alice")
    .execute();

// Easy to override when needed
session.upsert(key)
    .bin("name").setTo("Alice")
    .expireRecordAfter(Duration.ofDays(30))
    .ensureGenerationIs(5)
    .execute();
```

### 4. Duration Support

Human-readable time specifications:

```java
// Traditional
writePolicy.expiration = 86400;  // What is this? Seconds? Milliseconds?

// Fluent - unambiguous
.expireRecordAfter(Duration.ofDays(1))
.expireRecordAfter(Duration.ofHours(24))
.expireRecordAfterSeconds(86400)  // Still available if needed
```

### 5. Immutable, Thread-Safe Core Objects

- `DataSet` is immutable and thread-safe
- `Behavior` is immutable and thread-safe
- `Session` is lightweight and can be created per-request
- `Cluster` is thread-safe and designed for sharing

---

## Maintainability Benefits

### 1. Single Responsibility

Each class has a focused purpose:
- `ClusterDefinition`: Connection configuration
- `Cluster`: Connection lifecycle
- `Session`: Operation context
- `Behavior`: Policy configuration
- `DataSet`: Namespace/set abstraction
- `OperationBuilder`: Write operation construction
- `QueryBuilder`: Read operation construction

### 2. Open/Closed Principle

New operations can be added without modifying existing code:

```java
// Adding a new operation type just needs a new builder
public class NewOperationBuilder extends AbstractOperationBuilder {
    // New functionality
}

// Existing code remains unchanged
```

### 3. Centralized Configuration

Policy changes propagate automatically:

```java
// Change behavior once
Behavior updated = production.deriveWithChanges("updated", b -> b
    .on(Selectors.all(), ops -> ops.maximumNumberOfCallAttempts(5))
);

// All sessions using this behavior get the update
// No need to find and update every operation
```

### 4. Reduced Code Duplication

```java
// Traditional: repeated patterns
WritePolicy wp1 = new WritePolicy();
wp1.setTimeout(1000);
wp1.expiration = 3600;

WritePolicy wp2 = new WritePolicy();
wp2.setTimeout(1000);
wp2.expiration = 3600;
// ... repeated for every operation

// Fluent: define once, use everywhere
DataSet users = DataSet.of("test", "users");
Session session = cluster.createSession(behavior);
// Consistent settings for all operations
```

---

## Scalability Benefits

### 1. Dynamic Configuration

Behaviors can be loaded from YAML and reloaded without restart:

```java
// In production, tune settings without code changes or restarts
Behavior.startMonitoring("config/behaviors.yaml");

// File changes are detected and applied automatically
// New sessions pick up updated behaviors
```

### 2. Hierarchical Override System

Different tiers can have different configurations:

```java
Behavior base = Behavior.DEFAULT.deriveWithChanges("base", ...);

// Different clusters, different behaviors
Behavior tier1 = base.deriveWithChanges("tier1", b -> b
    .on(Selectors.all(), ops -> ops.abandonCallAfter(Duration.ofMillis(100)))
);

Behavior tier2 = base.deriveWithChanges("tier2", b -> b
    .on(Selectors.all(), ops -> ops.abandonCallAfter(Duration.ofSeconds(1)))
);
```

### 3. Session Isolation

Sessions are lightweight and can be created per-request:

```java
// Per-request session with request-specific behavior
public void handleRequest(Request req) {
    Behavior requestBehavior = selectBehavior(req.getPriority());
    Session session = cluster.createSession(requestBehavior);
    
    // Process request with appropriate policies
    // Session is garbage collected after request
}
```

---

## Migration Path

The Fluent Client is designed to coexist with the traditional client:

```java
// Both can run side by side
AerospikeClient traditional = new AerospikeClient("localhost", 3000);
Cluster fluent = new ClusterDefinition("localhost", 3000).connect();

// Access underlying client for edge cases
IAerospikeClient underlyingClient = cluster.getUnderlyingClient();

// Gradual migration: wrap repositories one at a time
public interface UserRepository {
    void save(User user);
    User find(String id);
}

// Traditional implementation
public class TraditionalUserRepository implements UserRepository { ... }

// Fluent implementation  
public class FluentUserRepository implements UserRepository { ... }

// Switch via configuration
UserRepository repo = useFluentClient 
    ? new FluentUserRepository(session) 
    : new TraditionalUserRepository(client);
```

---

## Summary

### Key Advantages

| Category | Traditional API | Fluent API |
|----------|-----------------|------------|
| **Readability** | Low - verbose, scattered | High - reads like English |
| **Boilerplate** | High - many intermediate objects | Low - method chaining |
| **Type Safety** | Runtime errors | Compile-time checks |
| **IDE Support** | Minimal | Full autocomplete & discovery |
| **Policy Management** | Manual, scattered | Centralized, hierarchical |
| **Configuration** | Code-only | Code + YAML, hot reload |
| **Resource Management** | Manual close() calls | Try-with-resources |
| **Testability** | Complex mocking | Simple session mocking |
| **Learning Curve** | Steep - many concepts | Gradual - guided by API |

### When to Use Each

**Use Traditional API when:**
- Maximum control is needed
- Using advanced features not yet wrapped
- Migrating existing code incrementally

**Use Fluent API when:**
- Starting new projects
- Readability and maintainability are priorities
- Team has varying Aerospike experience levels
- Dynamic configuration is valuable
- Type safety is important

---

## Conclusion

The Aerospike Fluent Client represents a modern approach to database client design. By applying proven patterns like fluent interfaces, builders, and hierarchical configuration, we've created an API that:

1. **Reduces cognitive load** - developers focus on *what* they want to do, not *how*
2. **Prevents errors** - type safety and compile-time checks catch mistakes early
3. **Scales with complexity** - simple operations stay simple, complex ones are still possible
4. **Adapts to change** - dynamic configuration without restarts
5. **Integrates smoothly** - works with existing code, modern Java features, and testing frameworks

The result is code that is more readable, maintainable, and enjoyable to write.

---

*This presentation was prepared for the Aerospike Java Client team.*

