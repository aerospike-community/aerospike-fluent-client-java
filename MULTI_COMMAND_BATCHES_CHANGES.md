# Multi-Command Batches Branch Changes

This document describes the changes introduced in the `multi-command-batches` branch.

## Summary

This branch introduces a **chainable batch operations API** that allows heterogeneous operations (upsert, update, insert, replace, delete, touch, exists, query) to be executed in a single batch call, along with a refactoring to unify return types across all operation builders.

## Commits

| Commit | Description |
|--------|-------------|
| `8e16e34` | Add chainable batch operations API for heterogeneous multi-command batches |
| `442a36a` | Refactor: Unify return types for TOUCH, EXISTS, DELETE operations |

---

## Changed Files

### Core Implementation Files

| File | Change Type | Description |
|------|-------------|-------------|
| `src/main/java/com/aerospike/ChainableOperationBuilder.java` | **New** | Builder for chainable operations with bin modifications (upsert, update, insert, replace) |
| `src/main/java/com/aerospike/ChainableNoBinsBuilder.java` | **New** | Builder for chainable operations without bin modifications (delete, touch, exists) |
| `src/main/java/com/aerospike/ChainableQueryBuilder.java` | **New** | Builder for read operations within a batch |
| `src/main/java/com/aerospike/OperationSpec.java` | **New** | Internal class holding per-operation data (keys, operations, filters, policies) |
| `src/main/java/com/aerospike/BatchExecutor.java` | **New** | Converts OperationSpec objects to BatchRecord and executes them |
| `src/main/java/com/aerospike/BinsValuesOperations.java` | **New** | Interface for BinsValuesBuilder compatibility |
| `src/main/java/com/aerospike/Session.java` | Modified | Added chainable operation entry points |
| `src/main/java/com/aerospike/OperationBuilder.java` | Modified | Updated to support generic type parameters |
| `src/main/java/com/aerospike/OperationWithNoBinsBuilder.java` | Modified | Changed to return RecordStream instead of List<Boolean> |
| `src/main/java/com/aerospike/RecordResult.java` | Modified | Added `asBoolean()` method for result code conversion |
| `src/main/java/com/aerospike/RecordStream.java` | Modified | Added `getFirstBoolean()` convenience method |
| `src/main/java/com/aerospike/BinBuilder.java` | Modified | Made generic to preserve type parameters through chains |
| `src/main/java/com/aerospike/BinsValuesBuilder.java` | Modified | Updated for chainable API compatibility |

### CDT (Collection Data Type) Files

These files were modified to make the interface hierarchy generic, preserving type parameters through method chains:

| File | Change Type |
|------|-------------|
| `src/main/java/com/aerospike/AbstractCdtBuilder.java` | Modified |
| `src/main/java/com/aerospike/CdtActionInvertableBuilder.java` | Modified |
| `src/main/java/com/aerospike/CdtActionNonInvertableBuilder.java` | Modified |
| `src/main/java/com/aerospike/CdtContextInvertableBuilder.java` | Modified |
| `src/main/java/com/aerospike/CdtContextNonInvertableBuilder.java` | Modified |
| `src/main/java/com/aerospike/CdtGetOrRemoveBuilder.java` | Modified |
| `src/main/java/com/aerospike/CdtSetterInvertableBuilder.java` | Modified |
| `src/main/java/com/aerospike/CdtSetterNonInvertableBuilder.java` | Modified |

### Other Modified Files

| File | Change Type | Description |
|------|-------------|-------------|
| `src/main/java/com/aerospike/AbstractSessionOperationBuilder.java` | Modified | Minor updates for chainable support |
| `src/main/java/com/aerospike/SystemSettings.java` | Modified | Minor updates |
| `src/main/java/com/aerospike/policy/BehaviorFileMonitor.java` | Modified | Minor updates |
| `src/main/java/com/aerospike/policy/BehaviorRegistry.java` | Modified | Minor updates |
| `src/main/java/com/aerospike/policy/BehaviorYamlConfig.java` | Modified | Minor updates |
| `src/main/java/com/aerospike/policy/BehaviorYamlLoader.java` | Modified | Minor updates |
| `src/main/java/com/aerospike/policy/DurationDeserializer.java` | Modified | Minor updates |

### Example and Test Files

| File | Change Type |
|------|-------------|
| `src/main/java/com/example/Examples_NewStyle.java` | Modified |
| `src/main/java/com/example/QueryExamples.java` | Modified |
| `src/main/java/com/example/YamlConfigExample.java` | Modified |
| `src/main/resources/example-config.yml` | Modified |
| `src/test/java/com/aerospike/policy/BehaviorYamlTest.java` | Modified |

### Documentation Files

| File | Change Type |
|------|-------------|
| `CHAINABLE_BATCH_OPERATIONS.md` | **New** |
| `CHAINABLE_BATCH_CODE_CLEANUP_SUMMARY.md` | **New** |
| `IMPLEMENTATION_SUMMARY_CHAINABLE_BATCH.md` | **New** |
| `FLUENT_API_PRESENTATION.md` | Modified |
| `pom.xml` | Modified |

### Updated Documentation (docs/)

| File |
|------|
| `docs/api/README.md` |
| `docs/api/connection/transactional-session.md` |
| `docs/api/operations/record-stream.md` |
| `docs/concepts/README.md` |
| `docs/concepts/object-mapping.md` |
| `docs/concepts/type-safe-operations.md` |
| `docs/examples/README.md` |
| `docs/getting-started/quickstart.md` |
| `docs/guides/advanced/transactions.md` |
| `docs/guides/cdt/lists.md` |
| `docs/guides/cdt/maps.md` |
| `docs/guides/cdt/nested-operations.md` |
| `docs/guides/configuration/yaml-configuration.md` |
| `docs/guides/crud/README.md` |
| `docs/guides/crud/reading-records.md` |
| `docs/guides/crud/updating-records.md` |
| `docs/guides/migration/api-comparison.md` |
| `docs/guides/migration/migrating-from-traditional.md` |
| `docs/guides/performance/query-optimization.md` |
| `docs/guides/querying/filtering.md` |
| `docs/guides/querying/partition-targeting.md` |
| `docs/guides/querying/simple-queries.md` |
| `docs/guides/querying/sorting-pagination.md` |
| `docs/guides/querying/using-dsl.md` |
| `docs/troubleshooting/README.md` |
| `docs/troubleshooting/common-errors.md` |

---

## Detailed Change Descriptions

### 1. Chainable Batch Operations API (Commit `8e16e34`)

**Purpose:** Enable heterogeneous batch operations to be chained together and executed in a single network call for optimal performance.

#### New Classes

- **`ChainableOperationBuilder`**: Builder for operations that modify bins (upsert, update, insert, replace). Supports:
  - Bin-level modifications via `.bin("name").setTo(value)`
  - Per-operation where clauses
  - Per-operation expiration policies
  - Generation checks for optimistic locking

- **`ChainableNoBinsBuilder`**: Builder for operations without bin modifications (delete, touch, exists). Supports:
  - Where clauses
  - Expiration policies (for touch)
  - Durable delete option

- **`ChainableQueryBuilder`**: Builder for read operations within a batch. Supports:
  - Bin projection via `.bins("name", "email")`
  - Where clauses

- **`OperationSpec`**: Internal data class holding per-operation configuration (keys, operations, filters, policies)

- **`BatchExecutor`**: Converts OperationSpec objects to Aerospike BatchRecord objects and executes them

- **`BinsValuesOperations`**: Interface for type-safe bin operations

#### Usage Example

```java
// Traditional way: separate calls or complex batch setup
List<BatchRecord> batchRecords = new ArrayList<>();
batchRecords.add(new BatchWrite(policy1, key1, ops1));
batchRecords.add(new BatchWrite(policy2, key2, ops2));
batchRecords.add(new BatchDelete(policy3, key3));
client.operate(batchPolicy, batchRecords);

// Fluent chainable way
session
    .upsert(key1).bin("name").setTo("Alice")
    .update(key2).bin("count").add(1)
    .delete(key3)
    .execute();
```

#### Key Features

- **Heterogeneous Operations**: Mix different operation types in a single batch
- **Per-Operation Configuration**: Each operation can have its own filters, expiration, generation checks
- **Default Where Clause**: Apply a default filter to all operations without explicit where clauses
- **Type-Safe**: Compile-time enforcement prevents invalid operations (e.g., can't call `.bin()` on delete)
- **Performance Optimized**: All chained operations execute as a single batch call

---

### 2. Unified Return Types (Commit `442a36a`)

**Purpose:** Make the API consistent by having all operation builders return `RecordStream` instead of varying return types.

#### Changes

- **`OperationWithNoBinsBuilder.execute()`** now returns `RecordStream` instead of `List<Boolean>`
  - Successful operations → `ResultCode.OK`
  - Non-existent keys → `ResultCode.KEY_NOT_FOUND_ERROR`
  - Exceptions captured in `RecordResult` for error handling

- **`RecordResult.asBoolean()`** added to convert result codes to boolean values

- **`RecordStream.getFirstBoolean()`** added for convenient boolean retrieval

#### Usage Example

```java
// Before: inconsistent return type
List<Boolean> results = session.exists(keys).execute();

// After: consistent RecordStream return
RecordStream results = session.exists(keys).execute();

// Easy boolean access
Optional<Boolean> exists = session.exists(key).execute().getFirstBoolean();

// Error handling with failures()
session.delete(keys).execute().failures().forEach(failure -> {
    System.err.println("Failed: " + failure.key() + " - " + failure.message());
});
```

---

## Architecture

### Execution Flow

1. User chains operations using fluent API
2. Each operation creates an `OperationSpec` with its configuration
3. On `execute()`, `BatchExecutor` converts all specs to `BatchRecord` objects
4. Batch operation is executed through Aerospike client
5. Results are converted to `RecordStream`

### Code Reuse

The implementation reuses approximately 85% of existing code:
- All bin operations from `AbstractOperationBuilder`
- Expiration/generation handling from `AbstractSessionOperationBuilder`
- Filter processing from `AbstractFilterableBuilder`
- Batch execution logic from existing batch operations
- Policy infrastructure from `Behavior` and `Settings`

---

## Limitations

- Dataset-based queries (scans) cannot be used in chainable batches
- Only key-based operations are supported
- All keys must be in the same namespace

