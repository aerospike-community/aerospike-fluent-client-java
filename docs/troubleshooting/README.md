# Troubleshooting & FAQ

Solutions to common problems and frequently asked questions.

## Quick Navigation

- **[Common Errors](#common-errors)** - Error messages and solutions
- **[Connection Issues](#connection-issues)** - Connectivity problems
- **[Performance Issues](#performance-issues)** - Slow operations
- **[Configuration Problems](#configuration-problems)** - Setup issues
- **[FAQ](#frequently-asked-questions)** - Common questions

---

## Common Errors

### Connection Refused

**Error**: `Connection refused: localhost:3000`

**Causes**:
1. Aerospike server not running
2. Wrong host/port
3. Firewall blocking connection

**Solutions**:
```bash
# Check if Aerospike is running
docker ps | grep aerospike
sudo systemctl status aerospike

# Start Aerospike (Docker)
docker start aerospike

# Test connection
telnet localhost 3000
```

---

### Authentication Failed

**Error**: `Authentication failed`

**Causes**:
- Incorrect username/password
- User doesn't exist
- Security not enabled on server

**Solutions**:
```java
// Verify credentials
Cluster cluster = new ClusterDefinition("localhost", 3000)
    .withNativeCredentials("correct_user", "correct_password")
    .connect();

// Check if security is enabled
asinfo -v "get-config:context=security;security.enable-security"
```

---

### Namespace Not Found

**Error**: `Namespace 'test' not found`

**Cause**: Namespace doesn't exist in server configuration

**Solution**:
```bash
# List available namespaces
asinfo -v namespaces

# Add namespace to aerospike.conf
namespace test {
    replication-factor 1
    memory-size 1G
    storage-engine memory
}
```

---

### Record Exists

**Error**: `AerospikeException.RecordExists`

**Cause**: Using `insert()` on existing record

**Solution**:
```java
// Option 1: Use upsert() instead
session.upsert(key)
    .bin("name").setTo("Alice")
    .execute();

// Option 2: Check if record exists first
RecordStream result = session.query(key).execute();
if (!result.hasNext()) {
    session.insert(key).bin("name").setTo("Alice").execute();
}

// Option 3: Handle the exception
try {
    session.insert(key).bin("name").setTo("Alice").execute();
} catch (AerospikeException.RecordExists e) {
    session.update(key).bin("name").setTo("Alice").execute();
}
```

---

### Timeout Error

**Error**: `AerospikeException.Timeout`

**Causes**:
- Network latency
- Server overloaded
- Timeout too short

**Solutions**:
```java
// Increase timeout
Behavior customBehavior = Behavior.DEFAULT.deriveWithChanges("longer-timeout", builder ->
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(30))
    .done()
);

// Check server health
asinfo -v statistics

// Monitor network latency
ping aerospike-host
```

---

### Generation Mismatch

**Error**: `AerospikeException.Generation`

**Cause**: Record modified between read and write

**Solution**:
```java
// Retry with new generation
int maxRetries = 3;
for (int i = 0; i < maxRetries; i++) {
    try {
        RecordStream result = session.query(key).execute();
        if (result.hasNext()) {
            RecordResult record = result.next();
            int generation = record.recordOrThrow().generation;
            
            session.update(key)
                .bin("balance").add(100)
                .ensureGenerationIs(generation)
                .execute();
            break;  // Success
        }
    } catch (AerospikeException.Generation e) {
        if (i == maxRetries - 1) throw e;
        // Retry
    }
}
```

---

## Connection Issues

### Can't Connect to Docker Container

**Problem**: `Connection refused` when using Docker

**Solution**:
```java
// On macOS/Windows, use host.docker.internal
Cluster cluster = new ClusterDefinition("host.docker.internal", 3000)
    .connect();

// Or find Docker bridge IP
docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' aerospike
```

### Cluster Name Mismatch

**Error**: `Cluster name mismatch: expected 'prod', got 'test'`

**Solutions**:
```java
// Option 1: Remove validation
Cluster cluster = new ClusterDefinition("localhost", 3000)
    // .validateClusterNameIs("prod")  // Remove this
    .connect();

// Option 2: Use correct cluster name
Cluster cluster = new ClusterDefinition("localhost", 3000)
    .validateClusterNameIs("test")  // Match actual cluster
    .connect();

// Check cluster name
asinfo -v "cluster-name"
```

### TLS Connection Failed

**Error**: `SSL handshake failed`

**Solutions**:
```java
// Verify TLS configuration
Cluster cluster = new ClusterDefinition("secure.example.com", 4333)
    .withTls(tls -> tls
        .enabledProtocols("TLSv1.3", "TLSv1.2")  // Try multiple versions
        .trustStorePath("/correct/path/to/truststore.jks")
        .trustStorePassword("correct_password")
    )
    .connect();

// Test with openssl
openssl s_client -connect secure.example.com:4333
```

---

## Performance Issues

### Slow Query Performance

**Symptoms**: Queries taking too long

**Solutions**:

**1. Use secondary indexes**
```java
// Create index on frequently queried field
// (via aql or server API)
CREATE INDEX age_idx ON test.users (age) NUMERIC

// Query will use index
session.query(users).where("$.age > 30").execute();
```

**2. Use partition targeting**
```java
// Parallelize across partitions
List<CompletableFuture<RecordStream>> futures = new ArrayList<>();
int partitionsPerThread = 1024 / numThreads;

for (int i = 0; i < numThreads; i++) {
    int start = i * partitionsPerThread;
    int end = start + partitionsPerThread;
    
    futures.add(CompletableFuture.supplyAsync(() ->
        session.query(users)
            .onPartitionRange(start, end)
            .execute()
    ));
}
```

**3. Optimize query behavior**
```java
Behavior queryOptimized = Behavior.DEFAULT.deriveWithChanges("query-opt", builder ->
    builder.onQuery()
        .recordQueueSize(10000)
        .maxConcurrentServers(8)
    .done()
);
```

### High Latency

**Symptoms**: Individual operations slow

**Solutions**:

**1. Check network**
```bash
ping aerospike-host
traceroute aerospike-host
```

**2. Use connection pooling correctly**
```java
// ✅ Good: Reuse cluster
private static final Cluster CLUSTER = connect();

// ❌ Bad: New cluster per operation
public void operation() {
    try (Cluster cluster = connect()) {
        // ...
    }
}
```

**3. Enable compression for large data**
```java
Behavior compressed = Behavior.DEFAULT.deriveWithChanges("compressed", builder ->
    builder.forAllOperations()
        .useCompression(true)
    .done()
);
```

### Memory Issues

**Symptoms**: OutOfMemoryError or high memory usage

**Solutions**:

**1. Use pagination**
```java
// ❌ Bad: Load all records into memory
List<Customer> all = session.query(customers)
    .execute()
    .toObjectList(mapper);

// ✅ Good: Process in pages
RecordStream results = session.query(customers)
    .pageSize(100)
    .execute();

while (results.hasMorePages()) {
    List<Customer> page = results.toObjectList(mapper);
    processPage(page);
}
```

**2. Reduce record queue size**
```java
Behavior lowMemory = Behavior.DEFAULT.deriveWithChanges("low-mem", builder ->
    builder.onQuery()
        .recordQueueSize(1000)  // Smaller queue
    .done()
);
```

---

## Configuration Problems

### YAML Not Loading

**Problem**: YAML configuration not working

**Solutions**:
```java
// Check file path
File yamlFile = new File("config/behaviors.yml");
System.out.println("File exists: " + yamlFile.exists());
System.out.println("Absolute path: " + yamlFile.getAbsolutePath());

// Start monitoring with correct path
Behavior.startMonitoring("config/behaviors.yml", 2000);

// Verify monitoring started
System.out.println("Monitoring: " + Behavior.isMonitoring());

// Try manual reload
Behavior.reloadBehaviors();

// Get behavior
Behavior behavior = Behavior.getBehavior("my-behavior");
if (behavior == null) {
    System.err.println("Behavior not found - check YAML syntax");
}
```

### Invalid Duration Format

**Error**: `Cannot parse duration: "invalid"`

**Solution**:
```yaml
# ❌ Bad
abandonCallAfter: "10"

# ✅ Good: Include unit
abandonCallAfter: "10s"

# Valid formats
abandonCallAfter: "30s"      # seconds
abandonCallAfter: "500ms"    # milliseconds
abandonCallAfter: "1m"       # minutes
abandonCallAfter: "2h"       # hours
```

---

## Frequently Asked Questions

### Should I use insert() or upsert()?

**Answer**: 
- Use `insert()` when the record **must not exist** (e.g., creating unique user ID)
- Use `upsert()` when you want to **create or update** (e.g., caching, idempotent operations)

### How do I handle concurrent updates?

**Answer**: Use generation-based optimistic locking:

```java
RecordStream result = session.query(key).execute();
if (result.hasNext()) {
    RecordResult record = result.next();
    int generation = record.recordOrThrow().generation;
    
    session.update(key)
        .bin("balance").add(100)
        .ensureGenerationIs(generation)  // Fails if modified
        .execute();
}
```

### Can I use the Fluent Client in production?

**Answer**: The current version (0.1.0) is a **developer preview**. Production readiness is planned for future releases. For production systems, consider:
- Using the traditional Aerospike client
- Waiting for a stable release
- Thorough testing in your environment

### How do I set TTL for records?

**Answer**: Use expiration methods:

```java
// Expire after duration
session.upsert(key)
    .bin("data").setTo("value")
    .expireRecordAfter(Duration.ofDays(7))
    .execute();

// Never expire
session.upsert(key)
    .bin("data").setTo("value")
    .neverExpire()
    .execute();

// Don't change current TTL
session.update(key)
    .bin("views").add(1)
    .withNoChangeInExpiration()
    .execute();
```

### How do I perform transactions?

**Answer**: Use `TransactionalSession`:

```java
TransactionalSession txSession = new TransactionalSession(cluster, behavior);

txSession.doInTransaction(tx -> {
    // All operations in this block are transactional
    Record record = tx.get(key);
    int balance = record.getInt("balance");
    
    tx.put(key, new Bin("balance", balance + 100));
});
```

### How do I query with multiple conditions?

**Answer**: Use DSL with logical operators:

```java
// String DSL
session.query(users)
    .where("$.age > 18 and $.country == 'US'")
    .execute();

// Or combine multiple conditions
session.query(users)
    .where("$.age >= 21 and $.age <= 65 and $.status == 'active'")
    .execute();
```

### How do I handle large result sets?

**Answer**: Use pagination:

```java
RecordStream results = session.query(users)
    .pageSize(100)
    .execute();

while (results.hasMorePages()) {
    List<User> page = results.toObjectList(mapper);
    // Process page
}
```

### Can I use this with Spring Boot?

**Answer**: Yes! Create a configuration class:

```java
@Configuration
public class AerospikeConfig {
    
    @Bean
    public Cluster aerospikeCluster(
            @Value("${aerospike.host}") String host,
            @Value("${aerospike.port}") int port) {
        return new ClusterDefinition(host, port).connect();
    }
    
    @Bean
    public Session aerospikeSession(Cluster cluster) {
        return cluster.createSession(Behavior.DEFAULT);
    }
    
    @PreDestroy
    public void cleanup() {
        if (cluster != null) {
            cluster.close();
        }
    }
}
```

### How do I debug connection issues?

**Answer**:
1. Enable debug logging:
   ```java
   Cluster cluster = new ClusterDefinition("localhost", 3000)
       .withLogLevel(Level.DEBUG)
       .connect();
   ```

2. Test connectivity:
   ```bash
   telnet localhost 3000
   asinfo -h localhost -p 3000 -v status
   ```

3. Check server logs:
   ```bash
   tail -f /var/log/aerospike/aerospike.log
   ```

---

## Getting More Help

### Still Stuck?

1. **Search existing issues**: [GitHub Issues](https://github.com/aerospike/aerospike-fluent-client-java/issues)
2. **Ask the community**: [Aerospike Forums](https://discuss.aerospike.com)
3. **Check examples**: [Examples & Recipes](../examples/README.md)
4. **Review docs**: [Full Documentation](../README.md)

### Reporting Bugs

When reporting issues, include:
- Java version (`java -version`)
- Fluent Client version (0.1.0)
- Aerospike Server version
- Complete error message and stack trace
- Minimal code to reproduce
- Expected vs actual behavior

---

**Didn't find your answer?** [Open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues) and we'll help!
