# Chainable Batch Operations

## Overview

The chainable batch operations API allows you to execute heterogeneous batch operations in a single network call. You can mix different operation types (upsert, update, insert, replace, delete, touch, exists, query) on different keys with different bin operations, filters, and policies.

## Key Features

- **Heterogeneous Operations**: Mix upsert, update, delete, touch, exists, and query operations in a single batch
- **Flexible Key Handling**: Each operation can target single or multiple keys
- **Per-Operation Configuration**: Each operation can have its own:
  - Bin modifications
  - Where clauses (filters)
  - Generation checks
  - Expiration policies
- **Default Where Clause**: Apply a default filter to all operations without explicit where clauses
- **Type-Safe**: Compile-time enforcement of which operations support which features
- **Performance Optimized**: Automatically executed as a single batch operation

## Basic Usage

### Simple Chaining

```java
session
    .upsert(users.id("user-1"))
        .bin("name").setTo("Alice")
        .bin("age").setTo(30)
    .update(users.id("user-2"))
        .bin("age").add(1)
    .delete(users.id("user-3"))
    .execute();
```

### Multiple Keys Per Operation

```java
session
    .update(users.ids("user-1", "user-2", "user-3"))
        .bin("status").setTo("active")
    .delete(users.ids("user-4", "user-5"))
    .execute();
```

## Operation Types

### Operations with Bin Modifications

These operations return `ChainableOperationBuilder` and support bin-level modifications:

- **`upsert(key)`** - Create or update record
- **`update(key)`** - Update existing record only
- **`insert(key)`** - Create new record only
- **`replace(key)`** - Replace entire record

```java
session
    .upsert(key)
        .bin("x").setTo(1)
        .bin("y").add(5)
    .insert(anotherKey)
        .bin("name").setTo("Bob")
    .execute();
```

### Operations without Bin Modifications

These operations return `ChainableNoBinsBuilder` and do NOT support bin modifications:

- **`delete(key)`** - Delete record
- **`touch(key)`** - Update metadata (generation, TTL) without modifying bins
- **`exists(key)`** - Check if record exists

```java
session
    .delete(key1)
        .where("$.status == 'inactive'")
    .touch(key2)
        .expireRecordAfter(Duration.ofDays(30))
    .exists(key3)
    .execute();
```

### Query (Read) Operations

Query operations return `ChainableQueryBuilder`:

- **`query(key)`** - Read record(s)

```java
session
    .upsert(key1)
        .bin("x").setTo(1)
    .query(key2)
        .bins("name", "email")  // Project specific bins
        .where("$.age > 21")
    .execute();
```

## Advanced Features

### Per-Operation Where Clauses

Each operation can have its own filter condition:

```java
session
    .update(users.ids("user-1", "user-2"))
        .bin("age").add(1)
        .where("$.age < 100")  // Only for this update
    .delete(users.id("user-3"))
        .where("$.status == 'inactive'")  // Only for this delete
    .execute();
```

### Default Where Clause

Apply a default filter to all operations without explicit where clauses:

```java
session
    .update(key1)
        .bin("x").setTo(1)
        .where("$.x < 10")  // Uses its own where clause
    .update(key2)
        .bin("y").setTo(2)  // Uses defaultWhere
    .delete(key3)           // Uses defaultWhere
    .defaultWhere("$.isActive == true")  // Applied to key2 and key3
    .execute();
```

### Per-Operation Policies

#### Expiration (TTL)

```java
session
    .upsert(sessionKey)
        .bin("data").setTo("...")
        .expireRecordAfter(Duration.ofHours(1))
    .update(premiumUserKey)
        .bin("plan").setTo("premium")
        .neverExpire()
    .touch(tempKey)
        .expireRecordAfter(Duration.ofMinutes(15))
    .execute();
```

Available expiration methods:
- `expireRecordAfter(Duration)` - Relative expiration
- `expireRecordAfterSeconds(int)` - Relative expiration in seconds
- `expireRecordAt(Date)` - Absolute expiration
- `expireRecordAt(LocalDateTime)` - Absolute expiration
- `neverExpire()` - TTL = -1 (permanent)
- `withNoChangeInExpiration()` - TTL = -2 (keep current)
- `expiryFromServerDefault()` - TTL = 0 (use namespace default)

#### Generation Checks

```java
session
    .update(accountKey)
        .bin("balance").setTo(1000)
        .ensureGenerationIs(5)  // Optimistic locking
    .execute();
```

#### Delete-Specific Options

```java
session
    .delete(users.ids("user-1", "user-2"))
        .where("$.status == 'banned'")
        .durablyDelete(true)  // Only available on delete operations
    .execute();
```

## Type Safety

The API enforces type safety at compile time:

```java
// ✅ Valid: upsert supports bin operations
session.upsert(key)
    .bin("name").setTo("Alice")
    .execute();

// ❌ Compile error: delete doesn't support bin operations
session.delete(key)
    .bin("name").setTo("Alice")  // ERROR!
    .execute();

// ✅ Valid: Can chain to operations that support bins
session.delete(key1)
    .upsert(key2)
    .bin("name").setTo("Alice")  // OK now
    .execute();
```

## Complex Example

```java
long now = System.currentTimeMillis();

session
    // Deactivate expired trial users
    .update(users.ids("user-1", "user-2", "user-3"))
        .bin("status").setTo("inactive")
        .bin("deactivatedAt").setTo(now)
        .where("$.tier == 'trial' and $.trialExpiresAt < %d", now)
    
    // Promote users to premium
    .update(users.ids("user-10", "user-11"))
        .bin("tier").setTo("premium")
        .bin("promotedAt").setTo(now)
        .ensureGenerationIs(3)
    
    // Update login timestamps
    .update(users.ids("user-20", "user-21"))
        .bin("lastLogin").setTo(now)
        .bin("loginCount").add(1)
    
    // Touch inactive users to extend TTL
    .touch(users.ids("user-30", "user-31"))
        .expireRecordAfter(Duration.ofDays(90))
    
    // Delete banned users
    .delete(users.ids("user-40", "user-41"))
        .where("$.status == 'banned'")
        .durablyDelete(true)
    
    // Read audit records
    .query(users.ids("user-1", "user-10", "user-20"))
        .bins("name", "status", "tier")
    
    // Default filter for operations without explicit where clause
    .defaultWhere("$.isActive == true")
    
    .execute();
```

## Architecture

### Classes

- **`OperationSpec`** - Internal class holding per-operation data (keys, operations, filters, policies)
- **`ChainableOperationBuilder`** - Builder for operations with bin modifications (upsert, update, insert, replace)
- **`ChainableNoBinsBuilder`** - Builder for operations without bin modifications (delete, touch, exists)
- **`ChainableQueryBuilder`** - Builder for read operations
- **`BatchExecutor`** - Converts OperationSpec objects to BatchRecord and executes them

### Execution Flow

1. User chains operations using fluent API
2. Each operation creates an `OperationSpec` with its configuration
3. On `execute()`, `BatchExecutor` converts all specs to `BatchRecord` objects
4. Batch operation is executed through Aerospike client
5. Results are converted to `RecordStream`

### Code Reuse

The implementation reuses ~85% of existing code:
- All bin operations from `AbstractOperationBuilder`
- Expiration/generation handling from `AbstractSessionOperationBuilder`
- Filter processing from `AbstractFilterableBuilder`
- Batch execution logic from existing batch operations
- Policy infrastructure from `Behavior` and `Settings`

## Performance

- All chained operations are executed as a **single batch call**
- Reduces network round-trips from N to 1
- Automatically uses appropriate batch policies based on operation types
- Supports transaction integration

## Examples

See `ChainableBatchExamples.java` for comprehensive examples including:
- Basic chaining
- Mixed operations
- Multiple keys per operation
- Where clauses (per-operation and default)
- Per-operation policies
- Query operations in batch
- Complex real-world scenarios

## Comparison with Traditional Client

### Traditional Aerospike Client

```java
// Traditional way: separate calls or complex batch setup
List<BatchRecord> batchRecords = new ArrayList<>();
batchRecords.add(new BatchWrite(policy1, key1, ops1));
batchRecords.add(new BatchWrite(policy2, key2, ops2));
batchRecords.add(new BatchDelete(policy3, key3));
client.operate(batchPolicy, batchRecords);
```

### Fluent Chainable API

```java
// Fluent way: intuitive chaining
session
    .upsert(key1).bin("x").setTo(1)
    .update(key2).bin("y").add(5)
    .delete(key3)
    .execute();
```

## Limitations

- Dataset-based queries (scans) cannot be used in chainable batches
- Only key-based operations are supported
- All keys must be in the same namespace

## Future Enhancements

Potential future improvements:
- Support for batch UDF execution
- Async execution variants
- Progress callbacks for large batches
- Batch size optimization hints



