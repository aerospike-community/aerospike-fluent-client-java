# CDT Enhancements Implementation Summary

## Overview
This document summarizes the enhancements made to the Aerospike Fluent Client Java library's Complex Data Type (CDT) operations, specifically adding support for **relative range operations** on maps and fixing a typo in an existing method.

## Changes Summary

### Statistics
- **Files Modified**: 3
- **Lines Added**: 291
- **Lines Removed**: 1

### Modified Files
1. `src/main/java/com/aerospike/CdtGetOrRemoveBuilder.java` (+176 lines)
2. `src/main/java/com/aerospike/BinBuilder.java` (+83 lines)
3. `src/main/java/com/aerospike/CdtOperationParams.java` (+33 lines)

## Detailed Changes

### 1. CdtGetOrRemoveBuilder.java

#### A. Typo Fix
**Fixed**: Method name typo `onMapKeuRange` → `onMapKeyRange`

```java
/**
 * @deprecated Typo in method name. Use {@link #onMapKeyRange(long, long)} instead.
 */
@Deprecated
public CdtContextInvertableBuilder onMapKeuRange(long startIncl, long endExcl) {
    return onMapKeyRange(startIncl, endExcl);
}

public CdtContextInvertableBuilder onMapKeyRange(long startIncl, long endExcl) {
    params.pushCurrentToContextAndReplaceWith(CdtOperation.MAP_BY_KEY_RANGE, 
        Value.get(startIncl), Value.get(endExcl));
    return this;
}
```

**Backward Compatibility**: The old method is retained but marked as `@Deprecated` to maintain backward compatibility.

#### B. New Navigation Methods

Added 12 new navigation methods for relative range operations:

**1. Map Key Relative Index Range** (3 overloads without count):
```java
public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(long key, int index)
public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(String key, int index)
public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(byte[] key, int index)
```

**2. Map Key Relative Index Range with Count** (3 overloads):
```java
public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(long key, int index, int count)
public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(String key, int index, int count)
public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(byte[] key, int index, int count)
```

**3. Map Value Relative Rank Range** (3 overloads without count):
```java
public CdtActionInvertableBuilder onMapValueRelativeRankRange(long value, int rank)
public CdtActionInvertableBuilder onMapValueRelativeRankRange(String value, int rank)
public CdtActionInvertableBuilder onMapValueRelativeRankRange(byte[] value, int rank)
```

**4. Map Value Relative Rank Range with Count** (3 overloads):
```java
public CdtActionInvertableBuilder onMapValueRelativeRankRange(long value, int rank, int count)
public CdtActionInvertableBuilder onMapValueRelativeRankRange(String value, int rank, int count)
public CdtActionInvertableBuilder onMapValueRelativeRankRange(byte[] value, int rank, int count)
```

#### C. Updated Action Methods

Updated 6 action methods to handle the new relative range operations:

1. **`getValues()`** - Added cases for:
   - `MAP_BY_KEY_REL_INDEX_RANGE`
   - `MAP_BY_VALUE_REL_RANK_RANGE`
   - Both with and without count parameter

2. **`getKeys()`** - Added cases for:
   - `MAP_BY_KEY_REL_INDEX_RANGE`
   - `MAP_BY_VALUE_REL_RANK_RANGE`
   - Both with and without count parameter

3. **`count()`** - Added cases for:
   - `MAP_BY_KEY_REL_INDEX_RANGE`
   - `MAP_BY_VALUE_REL_RANK_RANGE`
   - Both with and without count parameter

4. **`countAllOthers()`** - Added inverted cases for:
   - `MAP_BY_KEY_REL_INDEX_RANGE`
   - `MAP_BY_VALUE_REL_RANK_RANGE`
   - Both with and without count parameter

5. **`remove()`** - Added removal cases for:
   - `MAP_BY_KEY_REL_INDEX_RANGE`
   - `MAP_BY_VALUE_REL_RANK_RANGE`
   - Both with and without count parameter

6. **`removeAllOthers()`** - Added inverted removal cases for:
   - `MAP_BY_KEY_REL_INDEX_RANGE`
   - `MAP_BY_VALUE_REL_RANK_RANGE`
   - Both with and without count parameter

**Example Implementation Pattern**:
```java
case MAP_BY_KEY_REL_INDEX_RANGE:
    if (params.hasInt2()) {
        return opBuilder.addOp(MapOperation.getByKeyRelativeIndexRange(
            binName, params.getVal1(), params.getInt1(), params.getInt2(), 
            MapReturnType.VALUE, params.context()));
    } else {
        return opBuilder.addOp(MapOperation.getByKeyRelativeIndexRange(
            binName, params.getVal1(), params.getInt1(), 
            MapReturnType.VALUE, params.context()));
    }
```

### 2. BinBuilder.java

Added 12 navigation methods that mirror the `CdtGetOrRemoveBuilder` changes. These methods create new `CdtGetOrRemoveBuilder` instances with the appropriate operation parameters.

**Pattern**:
```java
public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(long key, int index) {
    return new CdtGetOrRemoveBuilder(binName, opBuilder, 
        new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, 
            Value.get(key), index));
}
```

All 12 overloads follow the same structure as in `CdtGetOrRemoveBuilder.java`, ensuring consistency across the API.

### 3. CdtOperationParams.java

#### A. New Constructors

Added 2 new constructors to support relative range operations:

```java
public CdtOperationParams(CdtOperation operation, Value val1, int int1) {
    this.val1 = val1;
    this.int1 = int1;
    this.operation = operation;
}

public CdtOperationParams(CdtOperation operation, Value val1, int int1, int int2) {
    this.val1 = val1;
    this.int1 = int1;
    this.int2 = int2;
    this.operation = operation;
}
```

#### B. New Helper Methods

Added 3 new helper methods:

```java
public void pushCurrentToContextAndReplaceWith(CdtOperation operation, Value val1, int int1) {
    pushCurrentToContext();
    this.operation = operation;
    this.val1 = val1;
    this.int1 = int1;
}

public void pushCurrentToContextAndReplaceWith(CdtOperation operation, Value val1, 
                                                int int1, int int2) {
    pushCurrentToContext();
    this.operation = operation;
    this.val1 = val1;
    this.int1 = int1;
    this.int2 = int2;
}

public boolean hasInt2() {
    return this.int2 != 0;
}
```

## Enum Values (Pre-existing)

The `CdtOperation` enum already contained the necessary values:
```java
protected static enum CdtOperation {
    // ... other operations ...
    MAP_BY_KEY_REL_INDEX_RANGE,    // Used for key relative index range operations
    MAP_BY_VALUE_REL_RANK_RANGE,   // Used for value relative rank range operations
    // ... other operations ...
}
```

## Usage Examples

### Example 1: Get Values by Key Relative Index Range
```java
session.upsert(dataSet.id("user123"))
    .bin("scores")
    .onMapKeyRelativeIndexRange("math", 2)  // Start at "math" key, offset by 2
    .getValues()
    .execute();
```

### Example 2: Get Values by Key Relative Index Range with Count
```java
session.upsert(dataSet.id("user123"))
    .bin("scores")
    .onMapKeyRelativeIndexRange("math", 2, 5)  // Get 5 items starting at offset 2
    .getValues()
    .execute();
```

### Example 3: Get Values by Value Relative Rank Range
```java
session.upsert(dataSet.id("user123"))
    .bin("scores")
    .onMapValueRelativeRankRange(75L, 3)  // Start at value 75, offset by rank 3
    .getValues()
    .execute();
```

### Example 4: Count All Others (Inverted Operation)
```java
session.upsert(dataSet.id("user123"))
    .bin("scores")
    .onMapKeyRelativeIndexRange("math", 2, 5)
    .countAllOthers()  // Count all items NOT in the range
    .execute();
```

### Example 5: Remove by Relative Range
```java
session.upsert(dataSet.id("user123"))
    .bin("scores")
    .onMapValueRelativeRankRange(50L, 0, 10)  // Remove 10 lowest scores >= 50
    .remove()
    .execute();
```

## API Design Consistency

All changes follow the established fluent API patterns:

1. **Method Chaining**: All navigation methods return builder interfaces for continued chaining
2. **Type Overloading**: Support for `long`, `String`, and `byte[]` parameter types
3. **Invertable Operations**: Return `CdtActionInvertableBuilder` to enable inverted operations
4. **Consistent Naming**: Follow the `on<Type><Criteria>` pattern
5. **Javadoc Documentation**: All new methods include comprehensive Javadoc comments
6. **Backward Compatibility**: Deprecated old method instead of removing it

## Testing Considerations

The following scenarios should be tested:

1. **Relative Index Range Operations**:
   - Without count parameter (unbounded range)
   - With count parameter (bounded range)
   - With different key types (long, String, byte[])

2. **Relative Rank Range Operations**:
   - Without count parameter (unbounded range)
   - With count parameter (bounded range)
   - With different value types (long, String, byte[])

3. **Action Method Variants**:
   - `getValues()` - Retrieve values in range
   - `getKeys()` - Retrieve keys in range
   - `count()` - Count items in range
   - `countAllOthers()` - Count items NOT in range
   - `remove()` - Remove items in range
   - `removeAllOthers()` - Remove items NOT in range

4. **Nested Context Operations**:
   - Using relative range operations within nested map/list contexts
   - Chaining multiple context navigations

5. **Edge Cases**:
   - Empty maps
   - Single-element maps
   - Negative index/rank offsets
   - Count exceeding available items

## Compilation Status

✅ **No compilation errors** related to these changes
- All new methods compile successfully
- The pre-existing DSL-related compilation errors in the project are unrelated to these changes

## Migration Guide

### For Users Using the Deprecated Method

If you're using `onMapKeuRange`:
```java
// Old (still works but deprecated)
.bin("map").onMapKeuRange(10L, 20L).getValues()

// New (recommended)
.bin("map").onMapKeyRange(10L, 20L).getValues()
```

### For Users Adding Relative Range Operations

```java
// Key relative index range (new feature)
.bin("map")
 .onMapKeyRelativeIndexRange("startKey", 2)  // Start 2 positions after "startKey"
 .getValues()

// With count limit (new feature)
.bin("map")
 .onMapKeyRelativeIndexRange("startKey", 2, 5)  // Get 5 items
 .getValues()

// Value relative rank range (new feature)
.bin("map")
 .onMapValueRelativeRankRange(100L, 3)  // Start 3 ranks after value 100
 .getValues()
```

## API Compatibility Matrix

| Feature | CdtGetOrRemoveBuilder | BinBuilder | Status |
|---------|----------------------|------------|--------|
| `onMapKeyRange` (fixed typo) | ✅ | N/A | Complete |
| `onMapKeyRelativeIndexRange` (no count) | ✅ | ✅ | Complete |
| `onMapKeyRelativeIndexRange` (with count) | ✅ | ✅ | Complete |
| `onMapValueRelativeRankRange` (no count) | ✅ | ✅ | Complete |
| `onMapValueRelativeRankRange` (with count) | ✅ | ✅ | Complete |
| Action method support | ✅ | N/A | Complete |
| CdtOperationParams support | ✅ | ✅ | Complete |

## Next Steps

1. **Testing**: Create comprehensive unit and integration tests
2. **Documentation**: Update API documentation and user guides
3. **Examples**: Add more usage examples to the examples package
4. **Release Notes**: Document these changes in the next release
5. **DSL Integration**: Ensure DSL-based queries can leverage these new operations

## References

- Aerospike CDT Operations: https://docs.aerospike.com/docs/guide/cdt-map.html
- Original Syntax Guide: `SYNTAX_GUIDE.md`
- API Documentation: `API_DOCUMENTATION.md`

---

**Implementation Date**: October 13, 2025
**Status**: ✅ Complete and Ready for Testing

