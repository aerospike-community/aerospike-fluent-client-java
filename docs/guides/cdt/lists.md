# Working with Lists

Learn how to perform operations on List-type bins using the Fluent Client.

## Overview

Aerospike lists are ordered collections that can contain any supported data type. The Fluent Client provides a fluent API for manipulating lists on the server atomically.

### What You Can Do With Lists

- **Add items**: Append, insert at specific positions
- **Remove items**: By index, by value, by rank
- **Retrieve items**: Get by index, range, value, rank
- **Sort and clear**: Organize and reset lists
- **Nested operations**: Work with lists inside maps or other lists

### List Diagram

```
┌────────────────────────────────────────────────────────────┐
│  List Bin: "tags"                                          │
│                                                             │
│  Index:   0      1        2      3       4                │
│  Value: "new"  "sale"  "promo" "hot"  "limited"           │
│  Rank:    3      4        2      1       0                │
│                                                             │
│  • Index: Position in list (0-based)                       │
│  • Rank: Order by value (0 = smallest, -1 = largest)      │
│  • Negative indices count from end: -1 = last element     │
└────────────────────────────────────────────────────────────┘
```

## Basic List Operations

### Appending Items

Add items to the end of a list:

```java
DataSet products = DataSet.of("ecommerce", "products");

// Append a single tag
session.update(products.id("product123"))
    .onList("tags").append("new")
    .execute();

// Append multiple tags
session.update(products.id("product123"))
    .onList("tags").append("sale")
    .onList("tags").append("promo")
    .execute();

// Append with type safety
session.update(products.id("product123"))
    .onList("tags").listAppend("featured")  // String
    .onList("ratings").listAppend(4.5)      // Double
    .onList("views").listAppend(1000L)      // Long
    .execute();
```

### Inserting at Specific Position

Insert items at a specific index:

```java
// Insert "urgent" at position 0 (beginning)
session.update(products.id("product123"))
    .onList("tags").insertAt(0, "urgent")
    .execute();

// Insert multiple items
session.update(products.id("product123"))
    .onList("tags").insertAt(1, "special")
    .onList("tags").insertAt(2, "limited")
    .execute();
```

**Index Examples**:
- `0`: Insert at beginning
- `2`: Insert at third position
- `-1`: Insert before last element
- Index beyond list size: Error (use append instead)

### Getting List Size

Retrieve the number of items in a list:

```java
RecordStream result = session.query(products.id("product123"))
    .onList("tags").listSize()
    .execute();

if (result.hasNext()) {
    KeyRecord record = result.next();
    Long size = record.record.getLong("tags");
    System.out.println("Number of tags: " + size);
}
```

### Clearing a List

Remove all items from a list:

```java
session.update(products.id("product123"))
    .onList("tags").listClear()
    .execute();
```

## Retrieving List Items

### Get by Index

Retrieve an item at a specific position:

```java
// Get first tag (index 0)
RecordStream result = session.query(products.id("product123"))
    .onList("tags").getByIndex(0)
    .execute();

// Get last tag (index -1)
RecordStream result = session.query(products.id("product123"))
    .onList("tags").getByIndex(-1)
    .execute();

// Get third tag (index 2)
RecordStream result = session.query(products.id("product123"))
    .onList("tags").getByIndex(2)
    .execute();
```

### Get by Range

Retrieve multiple items in a range:

```java
// Get first 3 tags (indices 0, 1, 2)
session.query(products.id("product123"))
    .onList("tags").getByIndexRange(0, 3)
    .execute();

// Get last 2 tags
session.query(products.id("product123"))
    .onList("tags").getByIndexRange(-2, 2)
    .execute();

// Get all tags from index 2 to end
session.query(products.id("product123"))
    .onList("tags").getByIndexRange(2, Integer.MAX_VALUE)
    .execute();
```

### Get by Value

Retrieve all items matching a specific value:

```java
// Get all occurrences of "sale"
session.query(products.id("product123"))
    .onList("tags").getByValue("sale")
    .execute();
```

### Get by Rank

Rank is the position when the list is sorted:

```java
// For list ["apple", "cherry", "banana"]
// Ranks: 0="apple", 1="banana", 2="cherry"

// Get lowest ranked item (smallest value)
session.query(products.id("product123"))
    .onList("tags").getByRank(0)
    .execute();

// Get highest ranked item (largest value)
session.query(products.id("product123"))
    .onList("tags").getByRank(-1)
    .execute();
```

## Removing List Items

### Remove by Index

Remove an item at a specific position:

```java
// Remove first item
session.update(products.id("product123"))
    .onList("tags").removeByIndex(0)
    .execute();

// Remove last item
session.update(products.id("product123"))
    .onList("tags").removeByIndex(-1)
    .execute();
```

### Remove by Index Range

Remove multiple items:

```java
// Remove first 2 items
session.update(products.id("product123"))
    .onList("tags").removeByIndexRange(0, 2)
    .execute();

// Remove last 3 items
session.update(products.id("product123"))
    .onList("tags").removeByIndexRange(-3, 3)
    .execute();
```

### Remove by Value

Remove all items matching a value:

```java
// Remove all "sale" tags
session.update(products.id("product123"))
    .onList("tags").removeByValue("sale")
    .execute();
```

### Remove by Rank

Remove items by their sorted position:

```java
// Remove lowest ranked item
session.update(products.id("product123"))
    .onList("tags").removeByRank(0)
    .execute();

// Remove highest ranked item
session.update(products.id("product123"))
    .onList("tags").removeByRank(-1)
    .execute();
```

## Advanced List Operations

### Sorting Lists

Sort a list in place:

```java
// Sort ascending
session.update(products.id("product123"))
    .onList("tags").listSort()
    .execute();
```

### Incrementing Numeric Values

Atomically increment a number in a list:

```java
// Increment view count at index 0
session.update(products.id("product123"))
    .onList("viewCounts").incrementBy(0, 1)
    .execute();
```

### Unique Values

Append only if the value doesn't already exist:

```java
// Add "featured" only if it's not already in the list
session.update(products.id("product123"))
    .onList("tags").listAppendUnique("featured", false)
    .execute();

// With failure allowed (no error if duplicate)
session.update(products.id("product123"))
    .onList("tags").listAppendUnique("featured", true)
    .execute();
```

## List Policies

### Ordered vs Unordered

Lists can be ordered or unordered:

```java
import com.aerospike.client.cdt.ListPolicy;
import com.aerospike.client.cdt.ListOrder;
import com.aerospike.client.cdt.ListWriteFlags;

// Ordered list (automatically sorted)
ListPolicy orderedPolicy = new ListPolicy(ListOrder.ORDERED, ListWriteFlags.DEFAULT);

// Unordered list (insertion order)
ListPolicy unorderedPolicy = new ListPolicy(ListOrder.UNORDERED, ListWriteFlags.DEFAULT);
```

**When to Use**:
- **Ordered**: Need automatic sorting, range queries by value
- **Unordered**: Insertion order matters, better performance

### Write Flags

Control list modification behavior:

```java
// Default: Allow duplicates, upsert
ListWriteFlags.DEFAULT

// Only add unique values
ListWriteFlags.ADD_UNIQUE

// Don't fail on policy violation
ListWriteFlags.NO_FAIL

// Bounded inserts (don't expand beyond size)
ListWriteFlags.INSERT_BOUNDED
```

## Nested List Operations

Work with lists inside other collections:

```java
// Data structure: {tags: ["new", "sale"], categories: [["electronics"], ["home"]]}

// Append to nested list
session.update(products.id("product123"))
    .onList("categories").withContext(ctx -> 
        ctx.listIndex(0)  // First category list
    )
    .append("phones")
    .execute();

// Get from nested list
session.query(products.id("product123"))
    .onList("categories").withContext(ctx ->
        ctx.listIndex(1)  // Second category list
    )
    .getByIndex(0)
    .execute();
```

## Common Patterns

### Pattern 1: Tag Management

```java
public class TagManager {
    private final Session session;
    private final DataSet products;
    
    public void addTag(String productId, String tag) {
        session.update(products.id(productId))
            .onList("tags").listAppendUnique(tag, true)
            .execute();
    }
    
    public void removeTag(String productId, String tag) {
        session.update(products.id(productId))
            .onList("tags").removeByValue(tag)
            .execute();
    }
    
    public List<String> getTags(String productId) {
        RecordStream result = session.query(products.id(productId))
            .execute();
            
        if (result.hasNext()) {
            KeyRecord record = result.next();
            return (List<String>) record.record.getList("tags");
        }
        return List.of();
    }
}
```

### Pattern 2: Time-Series Data

```java
// Store recent events as a list
public void addEvent(String userId, Map<String, Object> event) {
    session.update(users.id(userId))
        .onList("recentEvents").append(event)
        .execute();
    
    // Keep only last 100 events
    session.update(users.id(userId))
        .onList("recentEvents").removeByIndexRange(0, -100)
        .execute();
}
```

### Pattern 3: Leaderboard

```java
// Store scores as a sorted list
public void addScore(String leaderboardId, int score) {
    session.update(leaderboards.id(leaderboardId))
        .onList("scores").listAdd(score)  // Adds to sorted position
        .execute();
}

public List<Integer> getTopScores(String leaderboardId, int count) {
    RecordStream result = session.query(leaderboards.id(leaderboardId))
        .onList("scores").getByRankRange(-count, count)
        .execute();
    
    // Process result...
}
```

## Performance Considerations

### Index vs Rank Operations

**Index operations** are faster:
- Direct position lookup: O(1)
- Use when you know the position

**Rank operations** require sorting:
- Need to compute ranks: O(n log n)
- Use when you need value-based ordering

### List Size

Large lists impact performance:
- **< 100 items**: Excellent performance
- **100-1000 items**: Good performance
- **> 1000 items**: Consider alternative data structures

### Ordered vs Unordered

**Ordered lists**:
- ✅ Automatic sorting
- ✅ Efficient range queries by value
- ❌ Slower inserts (need to find position)

**Unordered lists**:
- ✅ Fast appends (O(1))
- ✅ Preserves insertion order
- ❌ Range queries by value require full scan

## When to Use Lists

### ✅ Use Lists When:

- **Order matters** - Chronological events, rankings
- **Small to medium size** - < 1000 items typically
- **Need indexing** - Access by position
- **Atomic operations** - Update without read-modify-write
- **Duplicate values OK** - Tags, categories can repeat

**Examples**: Recent activity, tags, rankings, categories

### ❌ Avoid Lists When:

- **Very large collections** - > 10,000 items (use separate records)
- **Frequent middle inserts** - Lots of shifting required
- **Key-value pairs** - Use maps instead
- **Unique values only** - Consider sets
- **Full-text search needed** - Use secondary indexes on bins

## Error Handling

Common list operation errors:

```java
try {
    session.update(products.id("product123"))
        .onList("tags").getByIndex(100)  // Index out of bounds
        .execute();
} catch (AerospikeException.InvalidRequest e) {
    // Index out of range
    log.error("Invalid index", e);
}

try {
    session.update(products.id("product123"))
        .onList("tags").listAppendUnique("duplicate", false)
        .execute();
} catch (AerospikeException.RecordExists e) {
    // Value already exists and NO_FAIL not set
    log.info("Tag already exists");
}
```

## Complete Example

```java
public class ProductTagging {
    private final Session session;
    private final DataSet products = DataSet.of("ecommerce", "products");
    
    public void initialize(String productId) {
        // Create product with empty tags list
        session.insert(products.id(productId))
            .bin("name").setTo("Example Product")
            .bin("tags").setTo(List.of())
            .execute();
    }
    
    public void addTags(String productId, String... tags) {
        OperationBuilder op = session.update(products.id(productId));
        
        for (String tag : tags) {
            op.onList("tags").listAppendUnique(tag, true);
        }
        
        op.execute();
    }
    
    public void removeTag(String productId, String tag) {
        session.update(products.id(productId))
            .onList("tags").removeByValue(tag)
            .execute();
    }
    
    public List<String> getTags(String productId) {
        RecordStream result = session.query(products.id(productId))
            .execute();
            
        if (result.hasNext()) {
            return (List<String>) result.next().record.getList("tags");
        }
        return List.of();
    }
    
    public void sortTags(String productId) {
        session.update(products.id(productId))
            .onList("tags").listSort()
            .execute();
    }
}
```

## Next Steps

- **[Working with Maps](./maps.md)** - Key-value operations within bins
- **[Nested Operations](./nested-operations.md)** - Complex nested CDT structures
- **[API Reference: OperationBuilder](../../api/operation-builder.md)** - Complete API documentation

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [Troubleshooting Guide](../../troubleshooting/README.md)
