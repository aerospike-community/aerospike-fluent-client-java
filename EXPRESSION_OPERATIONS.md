# Expression Operations (ExpOperation) Support

This document describes the new fluent API support for Aerospike's server-side expression operations (`ExpOperation.read()` and `ExpOperation.write()`).

## Overview

Expression operations allow you to evaluate server-side expressions and either:
- **Read**: Evaluate an expression and return the result in a named bin
- **Write**: Evaluate an expression and store the result in a named bin

These operations are executed atomically on the server, enabling complex calculations without multiple round-trips.

## API Design

### Design Pattern: Dual Overloads with Lambda Configurator

Following established patterns in this codebase (`withSystemSettings`, `BehaviorBuilder.on()`), the API provides two overloads for each method:

1. **Simple form** - Just the expression, uses default flags
2. **With options** - Expression + `Consumer<Options>` lambda for configuring flags

This pattern eliminates the need for `.done()` terminators because the lambda scope naturally delimits configuration.

## Usage Examples

### Expression Read

Read the result of a server-side expression into a bin:

```java
// Simple - evaluate expression and return result
session.update(dataSet.id(1))
    .bin("result").readExp("$.a + $.b")
    .execute();

// With options - handle errors gracefully
session.update(dataSet.id(1))
    .bin("result").readExp("$.a + $.b", opts -> opts
        .returnNilForMissingBins()
        .ignoreExpressionErrors()
    )
    .execute();

// Using DslExpression objects
session.update(dataSet.id(1))
    .bin("isAdult").readExp(Bins.intBin("age").gt(18))
    .execute();
```

### Expression Write

Write the result of a server-side expression to a bin:

```java
// Simple - evaluate expression and store result
session.update(dataSet.id(1))
    .bin("total").writeExp("$.price * $.quantity")
    .execute();

// With options - control when write happens
session.update(dataSet.id(1))
    .bin("computed").writeExp("$.a * 2", opts -> opts
        .onlyWhen(BinExistsPolicy.CREATE_ONLY)
        .ignoreExpressionErrors()
    )
    .execute();

// Allow expression to delete bin (if result is nil)
session.update(dataSet.id(1))
    .bin("derived").writeExp("$.optional_field", opts -> opts
        .onlyWhen(BinExistsPolicy.UPDATE_ONLY)
        .allowBinDeletion()
    )
    .execute();
```

### Mixed Operations

Expression operations chain seamlessly with other bin operations:

```java
session.update(dataSet.id(1))
    .bin("name").setTo("John")                           // Regular write
    .bin("age").add(1)                                   // Increment
    .bin("total").writeExp("$.price * $.qty")            // Expression write
    .bin("isVip").readExp("$.purchases > 100", opts -> opts
        .ignoreExpressionErrors()
    )
    .bin("status").setTo("active")                       // Regular write
    .execute();
```

## Expression Input Types

All `readExp()` and `writeExp()` methods support four expression input types:

| Type | Description | Example |
|------|-------------|---------|
| `String` | DSL string expression | `"$.a + $.b"` |
| `DslExpression` | Programmatic DSL object | `Bins.intBin("age").gt(18)` |
| `PreparedDsl` | Prepared statement with parameters | `new PreparedDsl("$.field == $1")` |
| `Exp` | Native Aerospike expression | `Exp.add(Exp.intBin("a"), Exp.intBin("b"))` |

## Read Options (ExpReadOptions)

| Method | Description | Maps To |
|--------|-------------|---------|
| `ignoreExpressionErrors()` | If expression evaluation fails, return nil instead of error | `EVAL_NO_FAIL` |
| `returnNilForMissingBins()` | If referenced bin doesn't exist, return nil instead of error | `EVAL_NO_FAIL` |

## Write Options (ExpWriteOptions)

### Bin Existence Policy

Use the `onlyWhen()` method with `BinExistsPolicy` enum to control when writes succeed:

| Policy | Description | Maps To |
|--------|-------------|---------|
| `BinExistsPolicy.ALWAYS` | Write regardless of bin existence (default) | `DEFAULT` |
| `BinExistsPolicy.CREATE_ONLY` | Only write if bin does NOT exist | `CREATE_ONLY` |
| `BinExistsPolicy.UPDATE_ONLY` | Only write if bin already exists | `UPDATE_ONLY` |

**Note**: Using an enum ensures only one policy can be active at a time, preventing the error of setting both `CREATE_ONLY` and `UPDATE_ONLY`.

### Other Options

| Method | Description | Maps To |
|--------|-------------|---------|
| `allowBinDeletion()` | If expression returns nil, delete the bin | `ALLOW_DELETE` |
| `ignorePolicyErrors()` | If bin policy fails, succeed silently | `POLICY_NO_FAIL` |
| `ignoreExpressionErrors()` | If expression fails, succeed silently | `EVAL_NO_FAIL` |

## Files Added/Modified

### New Files

- **`ExpReadOptions.java`** - Options builder for expression read flags
- **`ExpWriteOptions.java`** - Options builder for expression write flags (includes `BinExistsPolicy` enum)

### Modified Files

- **`BinBuilder.java`** - Added `readExp()` and `writeExp()` method overloads
- **`AbstractOperationBuilder.java`** - Added `parseExpression()` helper methods

## Mapping to Aerospike Flags

| Fluent Method | Aerospike Flag | Value |
|---------------|----------------|-------|
| `ignoreExpressionErrors()` | `ExpReadFlags.EVAL_NO_FAIL` / `ExpWriteFlags.EVAL_NO_FAIL` | 16 |
| `returnNilForMissingBins()` | `ExpReadFlags.EVAL_NO_FAIL` | 16 |
| `onlyWhen(CREATE_ONLY)` | `ExpWriteFlags.CREATE_ONLY` | 1 |
| `onlyWhen(UPDATE_ONLY)` | `ExpWriteFlags.UPDATE_ONLY` | 2 |
| `allowBinDeletion()` | `ExpWriteFlags.ALLOW_DELETE` | 4 |
| `ignorePolicyErrors()` | `ExpWriteFlags.POLICY_NO_FAIL` | 8 |

## Example: Complex Expression Operations

```java
// Calculate discounts and update multiple bins atomically
session.update(customerDataSet.id(customerId))
    // Read computed discount tier
    .bin("tier").readExp("$.totalPurchases > 10000 ? 'gold' : ($.totalPurchases > 1000 ? 'silver' : 'bronze')")
    
    // Write calculated discount percentage
    .bin("discount").writeExp("$.tier == 'gold' ? 0.2 : ($.tier == 'silver' ? 0.1 : 0.05)", opts -> opts
        .onlyWhen(BinExistsPolicy.UPDATE_ONLY)
    )
    
    // Compute and store loyalty points
    .bin("points").writeExp("$.totalPurchases / 100", opts -> opts
        .allowBinDeletion()
        .ignoreExpressionErrors()
    )
    
    .execute();
```

