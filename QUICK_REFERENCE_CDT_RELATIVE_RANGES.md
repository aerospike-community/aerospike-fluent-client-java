# Quick Reference: CDT Relative Range Operations

## New Methods Added

### Map Key Relative Index Range
Navigate to map items by key, then select items by relative index offset.

```java
// Get all items starting from key + offset
.bin("map").onMapKeyRelativeIndexRange(key, indexOffset).getValues()

// Get limited items starting from key + offset
.bin("map").onMapKeyRelativeIndexRange(key, indexOffset, count).getValues()
```

**Supported Key Types**: `long`, `String`, `byte[]`

### Map Value Relative Rank Range
Navigate to map items by value, then select items by relative rank offset.

```java
// Get all items starting from value + rank offset
.bin("map").onMapValueRelativeRankRange(value, rankOffset).getValues()

// Get limited items starting from value + rank offset
.bin("map").onMapValueRelativeRankRange(value, rankOffset, count).getValues()
```

**Supported Value Types**: `long`, `String`, `byte[]`

## Available Actions

All relative range navigation methods support these terminal actions:

| Action | Description | Example |
|--------|-------------|---------|
| `getValues()` | Retrieve values in range | `.onMapKeyRelativeIndexRange("key", 2).getValues()` |
| `getKeys()` | Retrieve keys in range | `.onMapKeyRelativeIndexRange("key", 2).getKeys()` |
| `count()` | Count items in range | `.onMapKeyRelativeIndexRange("key", 2).count()` |
| `countAllOthers()` | Count items NOT in range | `.onMapKeyRelativeIndexRange("key", 2).countAllOthers()` |
| `remove()` | Remove items in range | `.onMapKeyRelativeIndexRange("key", 2).remove()` |
| `removeAllOthers()` | Remove items NOT in range | `.onMapKeyRelativeIndexRange("key", 2).removeAllOthers()` |

## Common Use Cases

### 1. Pagination Through Sorted Map
```java
// Get next 10 items after "lastKey"
.bin("items")
 .onMapKeyRelativeIndexRange(lastKey, 1, 10)
 .getValues()
```

### 2. Get Top N by Rank
```java
// Get top 5 highest values
.bin("scores")
 .onMapValueRelativeRankRange(Long.MAX_VALUE, -5, 5)
 .getValues()
```

### 3. Remove Bottom N Items
```java
// Remove 3 lowest-ranked items
.bin("scores")
 .onMapValueRelativeRankRange(0L, 0, 3)
 .remove()
```

### 4. Get Range Around a Value
```java
// Get 5 items starting 2 ranks below value 100
.bin("data")
 .onMapValueRelativeRankRange(100L, -2, 5)
 .getValues()
```

### 5. Count Items in Percentile
```java
// Count items in top 25% (assuming 100 items)
.bin("scores")
 .onMapValueRelativeRankRange(Long.MAX_VALUE, -25, 25)
 .count()
```

## Parameter Meanings

### Index Offset
- **Positive**: Move forward from the reference key
- **Negative**: Move backward from the reference key
- **0**: Start at the reference key

### Rank Offset
- **Positive**: Move to higher ranks (higher values)
- **Negative**: Move to lower ranks (lower values)  
- **0**: Start at the rank of the reference value

### Count
- **Omitted**: Get all items from offset to end
- **Positive**: Get exactly N items (or less if not enough available)

## Comparison with Existing Methods

| Feature | Traditional Range | Relative Range |
|---------|------------------|----------------|
| Selection | Absolute start/end | Relative to reference |
| Parameters | `(startKey, endKey)` | `(referenceKey, offset)` |
| Flexibility | Fixed boundaries | Dynamic based on reference |
| Use Case | Known exact range | Pagination, windowing |

## Entry Points

Both `BinBuilder` and `CdtGetOrRemoveBuilder` support these methods:

```java
// Via BinBuilder (from Operation)
session.upsert(key)
  .bin("myMap")
  .onMapKeyRelativeIndexRange(...)
  
// Via CdtGetOrRemoveBuilder (during navigation)
session.upsert(key)
  .bin("outer")
  .onMapKey("inner")
  .onMapKeyRelativeIndexRange(...)
```

## Type Safety

All methods are type-safe and return appropriate builder interfaces:

```java
CdtActionInvertableBuilder onMapKeyRelativeIndexRange(long key, int index)
CdtActionInvertableBuilder onMapKeyRelativeIndexRange(String key, int index)
CdtActionInvertableBuilder onMapKeyRelativeIndexRange(byte[] key, int index)
```

**Invertable**: Supports `countAllOthers()` and `removeAllOthers()`

## Bug Fix: onMapKeuRange → onMapKeyRange

The typo in method name has been fixed:

```java
// Old (deprecated but still works)
.onMapKeuRange(10L, 20L)

// New (recommended)
.onMapKeyRange(10L, 20L)
```

## Complete Example

```java
// Setup
ClusterDefinition clusterDef = new ClusterDefinition("localhost", 3000);
Cluster cluster = clusterDef.connect();
Session session = cluster.session();
DataSet dataSet = DataSet.of("test", "users");

// Operation: Get 5 scores starting from "Charlie" + 2 positions
RecordStream results = session.upsert(dataSet.id("class123"))
    .bin("scores")
    .onMapKeyRelativeIndexRange("Charlie", 2, 5)
    .getValues()
    .execute();

// Process results
results.forEach(record -> {
    System.out.println("Result: " + record);
});
```

## Cheat Sheet

```java
// KEY RELATIVE INDEX (sorted by key)
.onMapKeyRelativeIndexRange(key, offset)       // Unbounded
.onMapKeyRelativeIndexRange(key, offset, n)    // Get n items

// VALUE RELATIVE RANK (sorted by value)
.onMapValueRelativeRankRange(val, offset)      // Unbounded
.onMapValueRelativeRankRange(val, offset, n)   // Get n items

// ACTIONS
.getValues()        // Get values
.getKeys()          // Get keys  
.count()            // Count items
.countAllOthers()   // Count inverted
.remove()           // Remove items
.removeAllOthers()  // Remove inverted
```

## Performance Notes

- Relative range operations are server-side operations (efficient)
- Suitable for large maps where client-side filtering would be expensive
- Use count parameter to limit data transfer
- Combine with inverted operations to process complementary sets

## Links

- Full Documentation: `CDT_ENHANCEMENTS_SUMMARY.md`
- Code Examples: `RelativeRangeExamples.java`
- Syntax Guide: `SYNTAX_GUIDE.md`
- API Documentation: `API_DOCUMENTATION.md`

