# Examples & Recipes

Real-world examples and reusable code snippets for the Aerospike Fluent Client.

## Overview

This section provides practical, copy-paste ready examples organized by use case.

---

## Complete Application Examples

### E-Commerce Product Catalog

A complete example showing:
- Product CRUD operations
- Inventory management
- Category-based queries
- Search functionality

```java
public class ProductCatalog {
    private final Session session;
    private final TypeSafeDataSet<Product> products;
    private final ProductMapper mapper;
    
    public ProductCatalog(Session session) {
        this.session = session;
        this.products = TypeSafeDataSet.of("ecommerce", "products", Product.class);
        this.mapper = new ProductMapper();
    }
    
    public void addProduct(Product product) {
        session.insert(products)
            .object(product)
            .execute();
    }
    
    public Optional<Product> getProduct(String productId) {
        List<Product> results = session.query(products.id(productId))
            .execute()
            .toObjectList(mapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public List<Product> searchByCategory(String category) {
        return session.query(products)
            .where("$.category == '" + category + "'")
            .execute()
            .toObjectList(mapper);
    }
    
    public void updateInventory(String productId, int quantity) {
        session.update(products.id(productId))
            .bin("inventory").add(quantity)
            .bin("lastUpdated").setTo(System.currentTimeMillis())
            .execute();
    }
    
    public List<Product> getLowStockProducts(int threshold) {
        return session.query(products)
            .where("$.inventory < " + threshold)
            .sortReturnedSubsetBy("inventory", SortDir.SORT_ASC)
            .execute()
            .toObjectList(mapper);
    }
}
```

### User Session Management

Complete session management with TTL:

```java
public class SessionManager {
    private final Session session;
    private final DataSet sessions;
    private final Duration sessionTimeout = Duration.ofHours(24);
    
    public SessionManager(Session session) {
        this.session = session;
        this.sessions = DataSet.of("app", "sessions");
    }
    
    public String createSession(String userId, Map<String, Object> data) {
        String sessionId = UUID.randomUUID().toString();
        
        session.insert(sessions.id(sessionId))
            .bin("userId").setTo(userId)
            .bin("createdAt").setTo(System.currentTimeMillis())
            .bin("lastAccessedAt").setTo(System.currentTimeMillis())
            .bin("data").setTo(data)
            .expireRecordAfter(sessionTimeout)
            .execute();
        
        return sessionId;
    }
    
    public Optional<Map<String, Object>> getSession(String sessionId) {
        RecordStream result = session.query(sessions.id(sessionId)).execute();
        
        if (result.hasNext()) {
            KeyRecord record = result.next();
            
            // Update last accessed time
            session.update(sessions.id(sessionId))
                .bin("lastAccessedAt").setTo(System.currentTimeMillis())
                .withNoChangeInExpiration()
                .execute();
            
            return Optional.of((Map<String, Object>) record.record.getValue("data"));
        }
        
        return Optional.empty();
    }
    
    public void invalidateSession(String sessionId) {
        session.delete(sessions.id(sessionId)).execute();
    }
    
    public void invalidateUserSessions(String userId) {
        RecordStream userSessions = session.query(sessions)
            .where("$.userId == '" + userId + "'")
            .execute();
        
        while (userSessions.hasNext()) {
            KeyRecord record = userSessions.next();
            session.delete(record.key).execute();
        }
    }
}
```

### Real-Time Analytics Dashboard

Counters and aggregations:

```java
public class AnalyticsDashboard {
    private final Session session;
    private final DataSet metrics;
    
    public AnalyticsDashboard(Session session) {
        this.session = session;
        this.metrics = DataSet.of("analytics", "metrics");
    }
    
    public void recordPageView(String pageId) {
        String key = "page:" + pageId + ":" + getToday();
        
        session.upsert(metrics.id(key))
            .bin("views").add(1)
            .bin("lastView").setTo(System.currentTimeMillis())
            .expireRecordAfter(Duration.ofDays(90))
            .execute();
    }
    
    public void recordUserAction(String userId, String action) {
        String key = "user:" + userId + ":" + action + ":" + getToday();
        
        session.upsert(metrics.id(key))
            .bin("count").add(1)
            .bin("lastAction").setTo(System.currentTimeMillis())
            .execute();
    }
    
    public Map<String, Long> getDailyMetrics(String date) {
        Map<String, Long> results = new HashMap<>();
        
        RecordStream records = session.query(metrics)
            .where("$.lastView != null")
            .execute();
        
        while (records.hasNext()) {
            KeyRecord record = records.next();
            String key = (String) record.key.userKey.getObject();
            if (key.endsWith(date)) {
                Long views = record.record.getLong("views");
                String pageId = key.split(":")[1];
                results.put(pageId, views);
            }
        }
        
        return results;
    }
    
    private String getToday() {
        return LocalDate.now().toString();
    }
}
```

---

## Code Snippets Library

### Atomic Counter

```java
public class AtomicCounter {
    public static void increment(Session session, DataSet dataSet, String counterId) {
        session.upsert(dataSet.id(counterId))
            .bin("value").add(1)
            .bin("lastUpdated").setTo(System.currentTimeMillis())
            .execute();
    }
    
    public static long get(Session session, DataSet dataSet, String counterId) {
        RecordStream result = session.query(dataSet.id(counterId)).execute();
        if (result.hasNext()) {
            return result.next().record.getLong("value");
        }
        return 0;
    }
}
```

### Distributed Lock

```java
public class DistributedLock {
    private final Session session;
    private final DataSet locks;
    private final Duration lockTimeout = Duration.ofSeconds(30);
    
    public DistributedLock(Session session) {
        this.session = session;
        this.locks = DataSet.of("system", "locks");
    }
    
    public boolean acquireLock(String lockName, String ownerId) {
        try {
            session.insert(locks.id(lockName))
                .bin("owner").setTo(ownerId)
                .bin("acquiredAt").setTo(System.currentTimeMillis())
                .expireRecordAfter(lockTimeout)
                .execute();
            return true;
        } catch (AerospikeException.RecordExists e) {
            // Lock is already held, this is an expected failure condition
            return false;
        } catch (AerospikeException e) {
            // Handle other potential errors like timeouts or connection issues
            System.err.println("Error acquiring lock '" + lockName + "': " + e.getMessage());
            throw e; // Re-throw to indicate a more serious problem
        }
    }
    
    public void releaseLock(String lockName, String ownerId) {
        RecordStream result = session.query(locks.id(lockName)).execute();
        if (result.hasNext()) {
            KeyRecord record = result.next();
            String owner = record.record.getString("owner");
            if (ownerId.equals(owner)) {
                session.delete(locks.id(lockName)).execute();
            }
        }
    }
}
```

### Time-Series Data

```java
public class TimeSeriesWriter {
    private final Session session;
    private final DataSet timeseries;
    
    public TimeSeriesWriter(Session session) {
        this.session = session;
        this.timeseries = DataSet.of("timeseries", "metrics");
    }
    
    public void write(String metricName, double value, long timestamp) {
        // Key: metric:timestamp_bucket
        long bucket = timestamp / 60000 * 60000;  // 1-minute buckets
        String key = metricName + ":" + bucket;
        
        session.upsert(timeseries.id(key))
            .bin("metric").setTo(metricName)
            .bin("bucket").setTo(bucket)
            .bin("values").setTo(List.of(Map.of(
                "timestamp", timestamp,
                "value", value
            )))
            .expireRecordAfter(Duration.ofDays(30))
            .execute();
    }
    
    public List<Map<String, Object>> read(String metricName, long startTime, long endTime) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        RecordStream records = session.query(timeseries)
            .where("$.metric == '" + metricName + "' and $.bucket >= " + startTime + " and $.bucket <= " + endTime)
            .execute();
        
        while (records.hasNext()) {
            KeyRecord record = records.next();
            List<Map<String, Object>> values = 
                (List<Map<String, Object>>) record.record.getValue("values");
            results.addAll(values);
        }
        
        return results;
    }
}
```

### Caching Pattern

```java
public class Cache<K, V> {
    private final Session session;
    private final DataSet cache;
    private final Duration ttl;
    private final RecordMapper<V> mapper;
    
    public Cache(Session session, String cacheName, Duration ttl, RecordMapper<V> mapper) {
        this.session = session;
        this.cache = DataSet.of("cache", cacheName);
        this.ttl = ttl;
        this.mapper = mapper;
    }
    
    public Optional<V> get(K key) {
        try {
            List<V> results = session.query(cache.idForObject(key))
                .execute()
                .toObjectList(mapper);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (AerospikeException e) {
            // Log the error, but treat it as a cache miss
            System.err.println("Failed to get key '" + key + "' from cache: " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Unexpected error getting key '" + key + "' from cache: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    public void put(K key, V value) {
        session.upsert(cache.idForObject(key))
            .bin("data").setTo(serializeValue(value))
            .bin("cachedAt").setTo(System.currentTimeMillis())
            .expireRecordAfter(ttl)
            .execute();
    }
    
    public void invalidate(K key) {
        session.delete(cache.idForObject(key)).execute();
    }
    
    private Map<String, Object> serializeValue(V value) {
        return mapper.toMap(value).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getObject()));
    }
}
```

---

## Design Patterns

### Repository Pattern

See [Object Mapping - Repository Pattern](../concepts/object-mapping.md#pattern-1-repository-pattern)

### Unit of Work Pattern

```java
public class UnitOfWork {
    private final Session session;
    private final List<Runnable> operations = new ArrayList<>();
    
    public void registerNew(DataSet dataSet, Object id, Map<String, Value> bins) {
        operations.add(() -> 
            session.insert(dataSet.idForObject(id))
                .bins(bins)
                .execute()
        );
    }
    
    public void registerModified(Key key, Map<String, Value> bins) {
        operations.add(() ->
            session.update(key)
                .bins(bins)
                .execute()
        );
    }
    
    public void registerDeleted(Key key) {
        operations.add(() -> session.delete(key).execute());
    }
    
    public void commit() {
        for (Runnable operation : operations) {
            operation.run();
        }
        operations.clear();
    }
    
    public void rollback() {
        operations.clear();
    }
}
```

### Retry Pattern

```java
public class RetryExecutor {
    public static <T> T executeWithRetry(
            Supplier<T> operation, 
            int maxRetries, 
            Duration delay) {
        
        int attempts = 0;
        while (true) {
            try {
                return operation.get();
            } catch (AerospikeException.Timeout e) {
                attempts++;
                if (attempts >= maxRetries) {
                    throw e;
                }
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }
}

// Usage
Customer customer = RetryExecutor.executeWithRetry(
    () -> getCustomer("alice"),
    3,
    Duration.ofMillis(100)
);
```

---

## Real-World Use Cases

### User Profile Management

```java
// See complete example at:
// https://github.com/aerospike/aerospike-fluent-client-java/examples/user-profiles
```

### Shopping Cart

```java
// See complete example at:
// https://github.com/aerospike/aerospike-fluent-client-java/examples/shopping-cart
```

### Leaderboard

```java
// See complete example at:
// https://github.com/aerospike/aerospike-fluent-client-java/examples/leaderboard
```

---

## Testing Examples

### Unit Testing with Mocks

```java
@Test
public void testUserService() {
    // Mock Session
    Session mockSession = mock(Session.class);
    OperationBuilder mockBuilder = mock(OperationBuilder.class);
    
    when(mockSession.insert(any())).thenReturn(mockBuilder);
    when(mockBuilder.bin(anyString())).thenReturn(mockBuilder);
    when(mockBuilder.execute()).thenReturn(mock(RecordStream.class));
    
    // Test your service
    UserService service = new UserService(mockSession);
    service.createUser("alice", "Alice Johnson");
    
    // Verify
    verify(mockSession).insert(any());
}
```

### Integration Testing

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {
    private Cluster cluster;
    private Session session;
    
    @BeforeAll
    public void setup() {
        cluster = new ClusterDefinition("localhost", 3000).connect();
        session = cluster.createSession(Behavior.DEFAULT);
    }
    
    @AfterAll
    public void teardown() {
        if (cluster != null) {
            cluster.close();
        }
    }
    
    @Test
    public void testRoundTrip() {
        DataSet test = DataSet.of("test", "integration");
        
        // Write
        session.upsert(test.id("test-key"))
            .bin("value").setTo(123)
            .execute();
        
        // Read
        RecordStream result = session.query(test.id("test-key")).execute();
        assertTrue(result.hasNext());
        assertEquals(123, result.next().record.getInt("value"));
    }
}
```

---

## Next Steps

- **Review [How-To Guides](../guides/README.md)** for detailed explanations
- **Check [API Reference](../api/README.md)** for complete documentation
- **See [Troubleshooting](../troubleshooting/README.md)** if you encounter issues

---

**Want to contribute an example?** See [Contributing Guide](../resources/contributing.md)
