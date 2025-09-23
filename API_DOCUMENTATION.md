# Aerospike Client API Documentation

This document provides comprehensive documentation for the Aerospike client API, covering the main classes and methods used for connecting to Aerospike clusters, performing operations, and managing data.

## Table of Contents

1. [Connection Management](#connection-management)
2. [Session Management](#session-management)
3. [Data Operations](#data-operations)
4. [Query Operations](#query-operations)
5. [Object Mapping](#object-mapping)
6. [Info and Monitoring](#info-and-monitoring)
7. [Behavior Configuration](#behavior-configuration)
8. [Index Monitoring](#index-monitoring)

## Connection Management

### ClusterDefinition

The `ClusterDefinition` class provides a fluent API for configuring and creating connections to Aerospike clusters.

#### Constructor

```java
// Single host connection
ClusterDefinition(String hostname, int port)

// Multiple hosts connection
ClusterDefinition(Host... hosts)
ClusterDefinition(List<Host> hosts)
```

#### Configuration Methods

```java
// Authentication
ClusterDefinition withNativeCredentials(String userName, String password)

// Cluster validation
ClusterDefinition validateClusterNameIs(String clusterName)

// Rack awareness
ClusterDefinition preferredRacks(int... racks)

// Service discovery
ClusterDefinition usingServicesAlternate()

// Logging
ClusterDefinition withLogLevel(Level logLevel)
ClusterDefinition useLogSink(Callback callback)
```

#### Connection

```java
Cluster connect()
```

**Example:**
```java
try (Cluster cluster = new ClusterDefinition("localhost", 3100)
        .withNativeCredentials("username", "password")
        .usingServicesAlternate()
        .preferredRacks(1, 2)
        .connect()) {
    
    Session session = cluster.createSession(Behavior.DEFAULT);
    // Use the session...
}
```

### Cluster

The `Cluster` class represents an active connection to an Aerospike cluster.

#### Key Methods

```java
// Session creation
Session createSession(Behavior behavior)

// Record mapping factory
Cluster setRecordMappingFactory(RecordMappingFactory factory)
RecordMappingFactory getRecordMappingFactory()

// Connection status
boolean isConnected()

// Resource management
void close()
```

#### Example Usage

```java
Cluster cluster = new ClusterDefinition("localhost", 3100).connect();

// Set up object mapping
cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
    Customer.class, new CustomerMapper(),
    Address.class, new AddressMapper()
)));

// Create session
Session session = cluster.createSession(Behavior.DEFAULT);

// Use session for operations...

// Clean up
cluster.close();
```

## Session Management

### Session

The `Session` class provides the main interface for performing database operations.

#### Creation

```java
Session session = cluster.createSession(Behavior behavior);
```

#### Key Methods

```java
// Query operations
QueryBuilder query(DataSet dataSet)
QueryBuilder query(Key key)
QueryBuilder query(List<Key> keys)

// Write operations
OperationBuilder upsert(Key key)
OperationBuilder insert(Key key)
OperationBuilder update(Key key)
OperationBuilder delete(Key key)

// Object operations
OperationObjectBuilder<T> upsert(TypeSafeDataSet<T> dataSet)
OperationObjectBuilder<T> insert(TypeSafeDataSet<T> dataSet)
OperationObjectBuilder<T> update(TypeSafeDataSet<T> dataSet)

// Multi-key operations
MultiValueBuilder upsert(List<Key> keys)
MultiValueBuilder insert(List<Key> keys)
MultiValueBuilder update(List<Key> keys)

// Info operations
InfoCommands info()
NamespaceInfo getNamespaceInfo(String namespace)

// Utility
void truncate(DataSet set)
boolean isNamespaceSC(String namespace)
```

## Data Operations

### DataSet

The `DataSet` class represents a namespace and set combination.

#### Creation

```java
DataSet dataSet = DataSet.of("namespace", "set");
```

#### Key Generation

```java
Key id(String id)
Key id(int id)
Key id(long id)
Key id(byte[] id)
Key idForObject(Object object)

List<Key> ids(String... ids)
List<Key> ids(int... ids)
List<Key> ids(long... ids)
List<Key> ids(List<?> ids)
```

### TypeSafeDataSet

The `TypeSafeDataSet` class provides type-safe operations for Java objects.

```java
TypeSafeDataSet<Customer> customerDataSet = TypeSafeDataSet.of("test", "person", Customer.class);
```

### OperationBuilder

The `OperationBuilder` class provides methods for building database operations.

#### Basic Operations

```java
// Set operations
OperationBuilder setTo(String value)
OperationBuilder setTo(int value)
OperationBuilder setTo(long value)
OperationBuilder setTo(double value)
OperationBuilder setTo(boolean value)
OperationBuilder setTo(byte[] value)
OperationBuilder setTo(List<?> value)
OperationBuilder setTo(Map<?, ?> value)

// Get operations
OperationBuilder get()
OperationBuilder get(String... bins)

// Arithmetic operations
OperationBuilder add(int amount)
OperationBuilder add(long amount)
OperationBuilder add(float amount)
OperationBuilder add(double amount)

// String operations
OperationBuilder append(String value)
OperationBuilder prepend(String value)

// Expiration
OperationBuilder expireRecordAfter(Duration duration)
OperationBuilder expireRecordAfterSeconds(int seconds)
OperationBuilder expireRecordAt(Date date)
OperationBuilder expireRecordAt(LocalDateTime date)
OperationBuilder neverExpire()
OperationBuilder withNoChangeInExpiration()

// Generation
OperationBuilder ensureGenerationIs(int generation)
```

#### Complex Data Type Operations

```java
// Map operations
CdtGetOrRemoveBuilder onMapIndex(int index)
CdtGetOrRemoveBuilder onMapKey(String key)
CdtGetOrRemoveBuilder onMapKey(long key)

// List operations
CdtGetOrRemoveBuilder onListIndex(int index)
```

#### Execution

```java
RecordStream execute()
```

## Query Operations

### QueryBuilder

The `QueryBuilder` class provides a fluent API for building and executing queries against Aerospike.

#### Query Types

The QueryBuilder automatically selects the appropriate query implementation based on the input:

- **Dataset queries**: Uses secondary indexes when available, falls back to scan
- **Single key queries**: Direct key lookup
- **Multiple key queries**: Batch key operations

#### Constructors

```java
// Query entire dataset
QueryBuilder(Session session, DataSet dataSet)

// Query single key
QueryBuilder(Session session, Key key)

// Query multiple keys
QueryBuilder(Session session, List<Key> keys)
```

#### Query Configuration

```java
// Bin selection
QueryBuilder readingOnlyBins(String... binNames)
QueryBuilder withNoBins()

// Limits and pagination
QueryBuilder limit(long limit)
QueryBuilder pageSize(int pageSize)

// Partition filtering
QueryBuilder onPartition(int partId)
QueryBuilder onPartitionRange(int startIncl, int endExcl)

// Sorting
QueryBuilder sortReturnedSubsetBy(String field)
QueryBuilder sortReturnedSubsetBy(String field, SortDir sortDir)
QueryBuilder sortReturnedSubsetBy(String field, SortDir sortDir, boolean caseSensitive)

// Filtering
QueryBuilder where(String dsl)
QueryBuilder where(BooleanExpression dsl)

// Transaction control
QueryBuilder notInAnyTransaction()
QueryBuilder inTransaction(Txn txn)
```

#### Execution

```java
RecordStream execute()
```

#### Example Queries

```java
// Simple query
RecordStream results = session.query(customerDataSet).execute();

// Query with filtering
RecordStream results = session.query(customerDataSet)
    .where("$.name == 'Tim' and $.age > 30")
    .limit(100)
    .execute();

// Query with sorting
RecordStream results = session.query(customerDataSet)
    .sortReturnedSubsetBy("name", SortDir.SORT_ASC)
    .sortReturnedSubsetBy("age", SortDir.SORT_DESC, true)
    .pageSize(20)
    .execute();

// Query specific keys
RecordStream results = session.query(customerDataSet.ids(1, 2, 3))
    .readingOnlyBins("name", "age")
    .execute();

// Query with partition targeting
RecordStream results = session.query(customerDataSet)
    .onPartitionRange(0, 2048)
    .limit(1000)
    .execute();
```

### RecordStream

The `RecordStream` class provides methods for processing query results.

#### Iteration

```java
boolean hasNext()
KeyRecord next()
void forEach(Consumer<KeyRecord> consumer)
```

#### Pagination

```java
boolean hasMorePages()
Optional<ResettablePagination> asResettablePagination()
```

#### Sorting

```java
Optional<Sortable> asSortable()
```

#### Object Conversion

```java
<T> List<T> toObjectLlist(RecordMapper<T> mapper)
Stream<KeyRecord> stream()
Optional<KeyRecord> getFirst()
```

#### Example Usage

```java
RecordStream rs = session.query(customerDataSet).pageSize(10).execute();

// Iterate through results
while (rs.hasNext()) {
    KeyRecord record = rs.next();
    System.out.println("Key: " + record.key + ", Value: " + record.record);
}

// Convert to objects
List<Customer> customers = rs.toObjectLlist(customerMapper);

// Pagination
int page = 0;
while (rs.hasMorePages()) {
    System.out.println("Page: " + (++page));
    List<Customer> pageCustomers = rs.toObjectLlist(customerMapper);
    pageCustomers.forEach(System.out::println);
}

// Sorting
rs.asSortable().ifPresent(sort -> {
    sort.sortBy(List.of(new SortProperties("name", SortDir.SORT_ASC, false)));
    // Process sorted results...
});
```

## Object Mapping

### RecordMappingFactory

Interface for providing record mappers.

```java
<T> RecordMapper<T> getMapper(Class<T> clazz)
```

### DefaultRecordMappingFactory

Default implementation of `RecordMappingFactory`.

```java
DefaultRecordMappingFactory(Map<Class<?>, RecordMapper<?>> map)
```

### RecordMapper

Interface for converting between Aerospike records and Java objects.

```java
T fromMap(Map<String, Object> map, Key recordKey, int generation)
Map<String, Value> toMap(T element)
Object id(T element)
```

#### Example Implementation

```java
public class CustomerMapper implements RecordMapper<Customer> {
    @Override
    public Customer fromMap(Map<String, Object> map, Key recordKey, int generation) {
        return new Customer(
            (Long) map.get("id"),
            (String) map.get("name"),
            (Integer) map.get("age"),
            new Date((Long) map.get("dob"))
        );
    }
    
    @Override
    public Map<String, Value> toMap(Customer customer) {
        Map<String, Value> map = new HashMap<>();
        map.put("id", Value.get(customer.getId()));
        map.put("name", Value.get(customer.getName()));
        map.put("age", Value.get(customer.getAge()));
        map.put("dob", Value.get(customer.getDob().getTime()));
        return map;
    }
    
    @Override
    public Object id(Customer customer) {
        return customer.getId();
    }
}
```

#### Usage

```java
// Set up mapping factory
cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
    Customer.class, new CustomerMapper(),
    Address.class, new AddressMapper()
)));

// Use with typed datasets
TypeSafeDataSet<Customer> customerDataSet = TypeSafeDataSet.of("test", "person", Customer.class);

// Object operations
session.insertInto(customerDataSet).object(customer).execute();
Customer result = session.query(customerDataSet.id(1)).execute().toObjectLlist(customerMapper).get(0);
```

## Info and Monitoring

### InfoCommands

The `InfoCommands` class provides methods for retrieving cluster information.

#### Namespace Information

```java
Set<String> namespaces()
Optional<NamespaceDetail> namespaceDetails(String namespace)
Map<Node, Optional<NamespaceDetail>> namespaceDetailsPerNode(String namespace)
```

#### Secondary Index Information

```java
List<Sindex> secondaryIndexes()
Map<Node, List<Sindex>> secondaryIndexesPerNode()
Optional<SindexDetail> secondaryIndexDetails(String namespace, String indexName)
Optional<SindexDetail> secondaryIndexDetails(Sindex index)
Map<Node, Optional<SindexDetail>> secondaryIndexDetailsPerNode(String namespace, String indexName)
```

#### Set Information

```java
List<SetDetail> sets()
Map<Node, List<SetDetail>> setsPerNode()
```

#### Example Usage

```java
InfoCommands info = session.info();

// Get all namespaces
Set<String> namespaces = info.namespaces();
namespaces.forEach(ns -> {
    Optional<NamespaceDetail> details = info.namespaceDetails(ns);
    details.ifPresent(System.out::println);
});

// Get secondary indexes
List<Sindex> indexes = info.secondaryIndexes();
indexes.forEach(index -> {
    System.out.println("Index: " + index);
    Optional<SindexDetail> details = info.secondaryIndexDetails(index);
    details.ifPresent(System.out::println);
});
```

### NamespaceInfo

The `NamespaceInfo` class provides real-time namespace information.

```java
NamespaceInfo getNamespaceInfo(String namespace)
NamespaceInfo getNamespaceInfo(String namespace, int refreshIntervalSecs)

boolean isStopWrites()
```

## Index Monitoring

### IndexesMonitor

The `IndexesMonitor` class runs a background thread that periodically queries the cluster for secondary index information and maintains an up-to-date cache.

#### Key Features

- **Automatic monitoring**: Starts automatically when a Cluster is created
- **Daemon thread**: Does not prevent JVM shutdown
- **Thread-safe**: Synchronized access to cached index information
- **Error handling**: Gracefully handles exceptions without stopping monitoring

#### Methods

```java
// Start monitoring
void startMonitor(Session session, Duration frequency)

// Get cached indexes
Set<Index> getIndexes()

// Stop monitoring
void stopMonitor()
```

#### Example Usage

```java
// Typically handled automatically by Cluster
IndexesMonitor monitor = new IndexesMonitor();
monitor.startMonitor(session, Duration.ofSeconds(5));

// Get current indexes
Set<Index> indexes = monitor.getIndexes();

// Stop monitoring
monitor.stopMonitor();
```

#### Monitoring Process

The monitoring thread performs the following steps:

1. Queries the cluster for all secondary indexes using `Session.info()`
2. For each index, retrieves detailed information including entries per bin value
3. Converts the information to `Index` objects
4. Updates the internal cache
5. Sleeps for the specified frequency before repeating

#### Integration with Query System

The cached index information is used by the query system to:

- Optimize query execution
- Provide index-aware query planning
- Select appropriate query strategies (index vs scan)
- Improve query performance

## Behavior Configuration

### Behavior

The `Behavior` class configures how operations are performed.

#### Default Behavior

```java
Behavior.DEFAULT
```

#### Custom Behavior

```java
Behavior customBehavior = Behavior.DEFAULT.deriveWithChanges("custom", builder -> 
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(30))
        .maximumNumberOfCallAttempts(3)
    .done()
    .onQuery()
        .recordQueueSize(10000)
        .maxConcurrentServers(4)
    .done()
);
```

#### Behavior Configuration Methods

```java
// All operations
BehaviorBuilder forAllOperations()

// Specific operation types
BehaviorBuilder onAvailablityModeReads()
BehaviorBuilder onBatchReads()
BehaviorBuilder onBatchWrites()
BehaviorBuilder onConsistencyModeReads()
BehaviorBuilder onInfo()
BehaviorBuilder onNonRetryableWrites()
BehaviorBuilder onQuery()
BehaviorBuilder onRetryableWrites()
```

#### Policy Configuration

```java
// Timeouts
BehaviorBuilder abandonCallAfter(Duration duration)
BehaviorBuilder waitForCallToComplete(Duration duration)
BehaviorBuilder waitForConnectionToComplete(Duration duration)
BehaviorBuilder waitForSocketResponseAfterCallFails(Duration duration)

// Retry settings
BehaviorBuilder maximumNumberOfCallAttempts(int attempts)
BehaviorBuilder delayBetweenRetries(Duration duration)

// Consistency
BehaviorBuilder replicaOrder(NodeCategory... categories)
BehaviorBuilder readConsistency(ReadModeSC mode)
BehaviorBuilder migrationReadConsistency(ReadModeAP mode)

// Query settings
BehaviorBuilder recordQueueSize(int size)
BehaviorBuilder maxConcurrentServers(int servers)

// Batch settings
BehaviorBuilder allowInlineMemoryAccess(boolean allow)
BehaviorBuilder allowInlineSsdAccess(boolean allow)
```

## Complete Example

Here's a complete example showing how to use the API:

```java
// Connect to cluster
try (Cluster cluster = new ClusterDefinition("localhost", 3100)
        .withNativeCredentials("username", "password")
        .connect()) {
    
    // Set up object mapping
    cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
        Customer.class, new CustomerMapper(),
        Address.class, new AddressMapper()
    )));
    
    // Create session with custom behavior
    Behavior behavior = Behavior.DEFAULT.deriveWithChanges("custom", builder -> 
        builder.forAllOperations()
            .abandonCallAfter(Duration.ofSeconds(30))
            .maximumNumberOfCallAttempts(3)
        .done()
    );
    
    Session session = cluster.createSession(behavior);
    
    // Create datasets
    TypeSafeDataSet<Customer> customerDataSet = TypeSafeDataSet.of("test", "person", Customer.class);
    
    // Insert data
    Customer customer = new Customer(1L, "John Doe", 30, new Date());
    session.insertInto(customerDataSet).object(customer).execute();
    
    // Query data
    RecordStream results = session.query(customerDataSet)
        .where("$.name == 'John Doe'")
        .limit(10)
        .execute();
    
    List<Customer> customers = results.toObjectLlist(new CustomerMapper());
    customers.forEach(System.out::println);
    
    // Get cluster info
    Set<String> namespaces = session.info().namespaces();
    System.out.println("Namespaces: " + namespaces);
    
} catch (Exception e) {
    e.printStackTrace();
}
```

## Error Handling

The API provides comprehensive error handling through exceptions:

- `AerospikeException`: Base exception for Aerospike-specific errors
- `AuthenticationException`: Authentication failures
- `AuthorizationException`: Authorization failures
- `QuotaException`: Quota exceeded errors
- `SecurityException`: Security-related errors

## Best Practices

1. **Resource Management**: Always use try-with-resources for Cluster instances
2. **Object Mapping**: Use typed datasets and record mappers for type safety
3. **Behavior Configuration**: Create custom behaviors for different use cases
4. **Query Optimization**: Use appropriate limits and pagination for large result sets
5. **Error Handling**: Implement proper exception handling for production applications
6. **Connection Pooling**: Reuse Session instances when possible
7. **Monitoring**: Use InfoCommands to monitor cluster health and performance
8. **Index Monitoring**: Let the IndexesMonitor handle index discovery automatically
9. **Query Planning**: Use partition targeting for load balancing and parallel processing
10. **Performance Tuning**: Configure appropriate page sizes and limits for your use case 