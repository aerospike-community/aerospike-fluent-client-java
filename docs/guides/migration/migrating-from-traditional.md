# Migrating from Traditional Client

Guide for migrating from the traditional Aerospike Java Client to the Fluent Client.

## Overview

The Fluent Client is a modern wrapper around the traditional Aerospike Java Client, providing a more intuitive and type-safe API while maintaining full access to underlying functionality.

### Migration Strategy

```
┌──────────────────────────────────────────────────────────┐
│  Migration Approaches                                     │
├──────────────────────────────────────────────────────────┤
│  1. Incremental (Recommended)                            │
│     • Migrate module by module                            │
│     • Both clients can coexist                            │
│     • Lower risk, easier testing                          │
│                                                           │
│  2. Complete Rewrite                                      │
│     • Replace all at once                                 │
│     • Clean break, consistent code                        │
│     • Higher risk, more work upfront                      │
└──────────────────────────────────────────────────────────┘
```

## Key Differences

### Connecting to Cluster

**Traditional Client**:
```java
ClientPolicy policy = new ClientPolicy();
policy.user = "username";
policy.password = "password";

AerospikeClient client = new AerospikeClient(policy, "localhost", 3000);
// Must remember to close
client.close();
```

**Fluent Client**:
```java
// With auto-close
try (Cluster cluster = new ClusterDefinition("localhost", 3000)
        .withNativeCredentials("username", "password")
        .connect()) {
    // Use cluster
} // Automatically closed
```

### Writing Records

**Traditional Client**:
```java
WritePolicy writePolicy = new WritePolicy();
writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
writePolicy.expiration = 3600;

Key key = new Key("test", "users", "alice");
Bin bin1 = new Bin("name", "Alice");
Bin bin2 = new Bin("age", 30);
Bin bin3 = new Bin("email", "alice@example.com");

client.put(writePolicy, key, bin1, bin2, bin3);
```

**Fluent Client**:
```java
DataSet users = DataSet.of("test", "users");
Session session = cluster.createSession(Behavior.DEFAULT);

session.upsert(users.id("alice"))
    .bin("name").setTo("Alice")
    .bin("age").setTo(30)
    .bin("email").setTo("alice@example.com")
    .expireRecordAfterSeconds(3600)
    .execute();
```

### Reading Records

**Traditional Client**:
```java
Key key = new Key("test", "users", "alice");
Record record = client.get(null, key);

if (record != null) {
    String name = record.getString("name");
    int age = record.getInt("age");
}
```

**Fluent Client**:
```java
DataSet users = DataSet.of("test", "users");

RecordStream result = session.query(users.id("alice"))
    .execute();

if (result.hasNext()) {
    RecordResult kr = result.next();
    String name = kr.recordOrThrow().getString("name");
    int age = kr.recordOrThrow().getInt("age");
}
```

### Querying with Filters

**Traditional Client**:
```java
Statement stmt = new Statement();
stmt.setNamespace("test");
stmt.setSetName("users");
stmt.setFilter(Filter.range("age", 21, 65));

RecordSet rs = client.query(null, stmt);
try {
    while (rs.next()) {
        Record record = rs.getRecord();
        // Process record
    }
} finally {
    rs.close();
}
```

**Fluent Client**:
```java
DataSet users = DataSet.of("test", "users");

RecordStream results = session.query(users)
    .where("$.age >= 21 and $.age <= 65")
    .execute();

results.forEach(kr -> {
    // Process record
});
```

### Batch Operations

**Traditional Client**:
```java
Key[] keys = new Key[] {
    new Key("test", "users", "alice"),
    new Key("test", "users", "bob")
};

Record[] records = client.get(null, keys);
for (Record record : records) {
    if (record != null) {
        // Process
    }
}
```

**Fluent Client**:
```java
DataSet users = DataSet.of("test", "users");

RecordStream results = session.query(users.ids("alice", "bob"))
    .execute();

results.forEach(kr -> {
    // Process record
});
```

## Migration Patterns

### Pattern 1: Repository Wrapper

Migrate by wrapping existing code:

```java
// Old repository
public class OldUserRepository {
    private final AerospikeClient client;
    
    public void save(User user) {
        Key key = new Key("test", "users", user.getId());
        Bin[] bins = {
            new Bin("name", user.getName()),
            new Bin("age", user.getAge())
        };
        client.put(null, key, bins);
    }
}

// New repository (coexist during migration)
public class UserRepository {
    private final Session session;
    private final DataSet users = DataSet.of("test", "users");
    
    public void save(User user) {
        session.upsert(users.id(user.getId()))
            .bin("name").setTo(user.getName())
            .bin("age").setTo(user.getAge())
            .execute();
    }
}
```

### Pattern 2: Adapter Pattern

Create an adapter to switch implementations:

```java
public interface UserDataAccess {
    void save(User user);
    User get(String id);
}

public class TraditionalUserDataAccess implements UserDataAccess {
    private final AerospikeClient client;
    // Traditional implementation
}

public class FluentUserDataAccess implements UserDataAccess {
    private final Session session;
    private final DataSet users = DataSet.of("test", "users");
    // Fluent implementation
}

// Switch via configuration
UserDataAccess dataAccess = useFluentClient 
    ? new FluentUserDataAccess(session)
    : new TraditionalUserDataAccess(client);
```

### Pattern 3: Gradual Module Migration

Migrate one module at a time:

```
Step 1: User Service → Fluent Client
Step 2: Product Service → Still Traditional
Step 3: Order Service → Still Traditional

... gradually migrate each
```

## Complete Migration Example

**Before (Traditional)**:
```java
public class UserService {
    private final AerospikeClient client;
    
    public UserService() {
        this.client = new AerospikeClient("localhost", 3000);
    }
    
    public void createUser(String id, String name, int age) {
        WritePolicy policy = new WritePolicy();
        policy.recordExistsAction = RecordExistsAction.CREATE_ONLY;
        
        Key key = new Key("test", "users", id);
        Bin[] bins = {
            new Bin("name", name),
            new Bin("age", age),
            new Bin("created", System.currentTimeMillis())
        };
        
        try {
            client.put(policy, key, bins);
        } catch (AerospikeException.RecordExistsException e) {
            throw new IllegalStateException("User already exists");
        }
    }
    
    public User getUser(String id) {
        Key key = new Key("test", "users", id);
        Record record = client.get(null, key);
        
        if (record == null) {
            return null;
        }
        
        return new User(
            id,
            record.getString("name"),
            record.getInt("age"),
            record.getLong("created")
        );
    }
    
    public void close() {
        client.close();
    }
}
```

**After (Fluent)**:
```java
public class UserService implements AutoCloseable {
    private final Cluster cluster;
    private final Session session;
    private final DataSet users = DataSet.of("test", "users");
    
    public UserService() {
        this.cluster = new ClusterDefinition("localhost", 3000).connect();
        this.session = cluster.createSession(Behavior.DEFAULT);
    }
    
    public void createUser(String id, String name, int age) {
        try {
            session.insert(users.id(id))
                .bin("name").setTo(name)
                .bin("age").setTo(age)
                .bin("created").setTo(System.currentTimeMillis())
                .execute();
        } catch (AerospikeException.RecordExists e) {
            throw new IllegalStateException("User already exists");
        }
    }
    
    public User getUser(String id) {
        RecordStream result = session.query(users.id(id))
            .execute();
        
        if (!result.hasNext()) {
            return null;
        }
        
        RecordResult kr = result.next();
        return new User(
            id,
            kr.recordOrThrow().getString("name"),
            kr.recordOrThrow().getInt("age"),
            kr.recordOrThrow().getLong("created")
        );
    }
    
    @Override
    public void close() {
        cluster.close();
    }
}
```

## Migration Checklist

### Preparation
- [ ] Review Fluent Client documentation
- [ ] Identify all Aerospike usage in codebase
- [ ] Choose migration strategy (incremental vs complete)
- [ ] Set up test environment with both clients
- [ ] Create compatibility tests

### During Migration
- [ ] Update dependencies (add fluent client)
- [ ] Create Cluster/Session instead of AerospikeClient
- [ ] Replace Policy objects with Behavior
- [ ] Convert Key creation to DataSet usage
- [ ] Replace Bin arrays with fluent `.bin()` calls
- [ ] Update query/scan code to use DSL
- [ ] Convert batch operations
- [ ] Update CDT operations (lists/maps)
- [ ] Ensure proper resource cleanup (try-with-resources)

### Testing
- [ ] Unit tests for migrated code
- [ ] Integration tests with real Aerospike
- [ ] Performance comparison tests
- [ ] Error handling verification
- [ ] Load testing for production scenarios

### Post-Migration
- [ ] Remove traditional client dependency (if fully migrated)
- [ ] Update documentation
- [ ] Train team on new API
- [ ] Monitor production performance

## Common Pitfalls

### ❌ Not Closing Resources

**Wrong**:
```java
Cluster cluster = new ClusterDefinition("localhost", 3000).connect();
Session session = cluster.createSession(Behavior.DEFAULT);
// Forgot to close - resource leak!
```

**Right**:
```java
try (Cluster cluster = new ClusterDefinition("localhost", 3000).connect()) {
    Session session = cluster.createSession(Behavior.DEFAULT);
    // Use session
} // Automatically closed
```

### ❌ Mixing Clients Incorrectly

**Wrong**:
```java
// Don't try to share connection
AerospikeClient traditional = new AerospikeClient("localhost", 3000);
Cluster fluent = ...; // Can't reuse traditional client's connection
```

**Right**:
```java
// Each has its own connection
AerospikeClient traditional = new AerospikeClient("localhost", 3000);
Cluster fluent = new ClusterDefinition("localhost", 3000).connect();
// Both connect independently to same cluster
```

## Getting Help

- **[API Comparison Guide](./api-comparison.md)** - Side-by-side comparison
- **[Troubleshooting](../../troubleshooting/README.md)** - Common issues
- **[Examples](../../examples/README.md)** - Code examples

---

**Questions about migration?** Open an issue on [GitHub](https://github.com/aerospike/aerospike-fluent-client-java/issues)
