# Working with Maps

Learn how to perform operations on Map-type bins using the Fluent Client.

## Overview

Aerospike maps are collections of key-value pairs, similar to HashMap or Dictionary in other languages. Maps provide efficient lookups by key and support ordered storage.

### What You Can Do With Maps

- **Set/Update entries**: Put, replace, or create key-value pairs
- **Remove entries**: By key, index, value, or rank
- **Retrieve entries**: Get by key, range, value, rank
- **Increment/Decrement**: Atomic numeric operations
- **Clear**: Remove all entries

### Map Diagram

```
┌────────────────────────────────────────────────────────────┐
│  Map Bin: "attributes"                                      │
│                                                             │
│  Key Ordered Map:                                          │
│  ┌────────────┬───────┐                                     │
│  │    Key     │ Value │                                     │
│  ├────────────┼───────┤                                     │
│  │  "color"   │ "red" │    ← Index 0, Rank depends on value│
│  │  "price"   │  29.99│    ← Index 1                       │
│  │  "weight"  │  5.2  │    ← Index 2                       │
│  └────────────┴───────┘                                     │
│                                                             │
│  • Index: Position in map (insertion or key order)         │
│  • Rank: Sorted position by value                          │
│  • Key: Unique identifier for each entry                   │
└────────────────────────────────────────────────────────────┘
```

## Basic Map Operations

### Setting Map Values

```java
DataSet products = DataSet.of("ecommerce", "products");

// Set a single key-value pair
session.update(products.id("product123"))
    .onMapKey("attributes", "color").setTo("red")
    .execute();

// Set multiple entries
session.update(products.id("product123"))
    .onMapKey("attributes", "size").setTo("large")
    .onMapKey("attributes", "weight").setTo(5.2)
    .onMapKey("attributes", "inStock").setTo(true)
    .execute();

// Set an entire map at once
Map<String, Object> attrs = Map.of(
    "color", "blue",
    "size", "medium",
    "price", 29.99
);
session.update(products.id("product123"))
    .bin("attributes").setTo(attrs)
    .execute();
```

### Getting Map Values

```java
// Get value by key
RecordStream result = session.query(products.id("product123"))
    .onMapKey("attributes", "color").get()
    .execute();

// Get multiple keys
session.query(products.id("product123"))
    .onMapKey("attributes", "color").get()
    .onMapKey("attributes", "size").get()
    .execute();

// Get entire map
RecordStream result = session.query(products.id("product123"))
    .bin("attributes").get()
    .execute();
    
if (result.hasNext()) {
    Map<String, Object> attributes = 
        (Map<String, Object>) result.next().recordOrThrow().getValue("attributes");
}
```

### Getting Map Size

```java
RecordStream result = session.query(products.id("product123"))
    .onMap("attributes").mapSize()
    .execute();

if (result.hasNext()) {
    Long size = result.next().recordOrThrow().getLong("attributes");
    System.out.println("Map has " + size + " entries");
}
```

### Clearing a Map

```java
session.update(products.id("product123"))
    .onMap("attributes").mapClear()
    .execute();
```

## Removing Map Entries

### Remove by Key

```java
// Remove single key
session.update(products.id("product123"))
    .onMapKey("attributes", "temporary").remove()
    .execute();

// Remove multiple keys
session.update(products.id("product123"))
    .onMapKey("attributes", "key1").remove()
    .onMapKey("attributes", "key2").remove()
    .execute();
```

### Remove by Index

```java
// Remove first entry (index 0)
session.update(products.id("product123"))
    .onMap("attributes").removeByIndex(0)
    .execute();

// Remove last entry (index -1)
session.update(products.id("product123"))
    .onMap("attributes").removeByIndex(-1)
    .execute();
```

### Remove by Value

```java
// Remove all entries with value "discontinued"
session.update(products.id("product123"))
    .onMap("attributes").removeByValue("discontinued")
    .execute();
```

### Remove by Rank

```java
// Remove entry with lowest ranked value (rank 0)
session.update(products.id("product123"))
    .onMap("attributes").removeByRank(0)
    .execute();

// Remove entry with highest ranked value (rank -1)
session.update(products.id("product123"))
    .onMap("attributes").removeByRank(-1)
    .execute();
```

## Advanced Map Operations

### Increment/Decrement

Atomically modify numeric values:

```java
// Increment view count
session.update(products.id("product123"))
    .onMapKey("stats", "viewCount").increment(1)
    .execute();

// Decrement inventory
session.update(products.id("product123"))
    .onMapKey("inventory", "quantity").decrement(5)
    .execute();

// Increment with specific value
session.update(products.id("product123"))
    .onMapKey("stats", "likes").increment(10)
    .execute();
```

### Get by Index

Retrieve entries by their position in the map:

```java
// Get first entry
session.query(products.id("product123"))
    .onMap("attributes").getByIndex(0)
    .execute();

// Get last entry
session.query(products.id("product123"))
    .onMap("attributes").getByIndex(-1)
    .execute();

// Get range of entries (indices 1-3)
session.query(products.id("product123"))
    .onMap("attributes").getByIndexRange(1, 3)
    .execute();
```

### Get by Rank

Retrieve entries by their value ranking:

```java
// Get entry with lowest value
session.query(products.id("product123"))
    .onMap("prices").getByRank(0)
    .execute();

// Get entry with highest value
session.query(products.id("product123"))
    .onMap("prices").getByRank(-1)
    .execute();

// Get top 3 values
session.query(products.id("product123"))
    .onMap("prices").getByRankRange(-3, 3)
    .execute();
```

## Map Policies and Ordering

### Map Order Types

```java
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.cdt.MapPolicy;

// Unordered (default) - fastest for inserts
MapOrder.UNORDERED

// Key ordered - sorted by key
MapOrder.KEY_ORDERED

// Key-value ordered - sorted by key, then value
MapOrder.KEY_VALUE_ORDERED
```

### Write Policies

```java
import com.aerospike.client.cdt.MapWriteFlags;

// Default: Create or update keys
MapWriteFlags.DEFAULT

// Only create new keys (fail if exists)
MapWriteFlags.CREATE_ONLY

// Only update existing keys (fail if not exists)
MapWriteFlags.UPDATE_ONLY

// Don't fail on policy violation
MapWriteFlags.NO_FAIL
```

**Usage**:
```java
import com.aerospike.client.cdt.MapPolicy;

MapPolicy createOnly = new MapPolicy(MapOrder.KEY_ORDERED, MapWriteFlags.CREATE_ONLY);

// Use with operation (note: requires underlying client access)
```

## Nested Map Operations

Work with maps inside other collections:

```java
// Data: {user: {preferences: {theme: "dark", lang: "en"}}}

// Update nested map
session.update(users.id("alice"))
    .onMapKey("preferences", "theme").setTo("light")
    .withContext(ctx -> ctx.mapKey(Value.get("user")))
    .execute();

// Get from nested map
session.query(users.id("alice"))
    .onMapKey("preferences", "theme").get()
    .withContext(ctx -> ctx.mapKey(Value.get("user")))
    .execute();
```

## Common Patterns

### Pattern 1: User Preferences

```java
public class PreferencesManager {
    private final Session session;
    private final DataSet users;
    
    public void setPreference(String userId, String key, Object value) {
        session.update(users.id(userId))
            .onMapKey("preferences", key).setTo(value)
            .execute();
    }
    
    public Object getPreference(String userId, String key) {
        RecordStream result = session.query(users.id(userId))
            .onMapKey("preferences", key).get()
            .execute();
            
        if (result.hasNext()) {
            return result.next().recordOrThrow().getValue("preferences");
        }
        return null;
    }
    
    public void removePreference(String userId, String key) {
        session.update(users.id(userId))
            .onMapKey("preferences", key).remove()
            .execute();
    }
}
```

### Pattern 2: Counters/Statistics

```java
public class StatsTracker {
    private final Session session;
    private final DataSet entities;
    
    public void incrementStat(String entityId, String statName) {
        session.update(entities.id(entityId))
            .onMapKey("stats", statName).increment(1)
            .execute();
    }
    
    public Map<String, Long> getStats(String entityId) {
        RecordStream result = session.query(entities.id(entityId))
            .bin("stats").get()
            .execute();
            
        if (result.hasNext()) {
            return (Map<String, Long>) result.next().recordOrThrow().getValue("stats");
        }
        return Map.of();
    }
}
```

### Pattern 3: Product Attributes

```java
public class ProductAttributes {
    private final Session session;
    private final DataSet products;
    
    public void updateAttributes(String productId, Map<String, Object> updates) {
        OperationBuilder op = session.update(products.id(productId));
        
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            op.onMapKey("attributes", entry.getKey()).setTo(entry.getValue());
        }
        
        op.execute();
    }
    
    public Map<String, Object> getAttributes(String productId) {
        RecordStream result = session.query(products.id(productId))
            .bin("attributes").get()
            .execute();
            
        if (result.hasNext()) {
            return (Map<String, Object>) result.next().recordOrThrow().getValue("attributes");
        }
        return Map.of();
    }
}
```

## When to Use Maps

### ✅ Use Maps When:

- **Key-value lookups** - Need fast access by key
- **Sparse data** - Not all records have same fields
- **Dynamic schemas** - Fields vary per record
- **Counters** - Multiple named counters per record
- **Attributes** - Product attributes, user preferences
- **Metadata** - Tags, labels, properties

**Examples**: User preferences, product attributes, statistics, configuration

### ❌ Avoid Maps When:

- **Fixed schema** - All records have same fields (use bins instead)
- **Very large maps** - > 1000 entries (split into multiple records)
- **Ordered sequences** - Use lists instead
- **Full-text search** - Use secondary indexes
- **Complex queries** - Need joins or aggregations

## Performance Considerations

### Map Size

- **< 100 entries**: Excellent performance
- **100-1000 entries**: Good performance
- **> 1000 entries**: Consider partitioning across records

### Ordered vs Unordered

**KEY_ORDERED maps**:
- ✅ Efficient range queries by key
- ✅ Predictable iteration order
- ❌ Slower inserts (need to maintain order)

**UNORDERED maps**:
- ✅ Fastest inserts and updates
- ✅ Lowest memory overhead
- ❌ No ordering guarantees

### Key Types

Supported key types:
- `String` - Most common
- `Integer/Long` - For numeric keys
- `byte[]` - Binary keys

## Complete Example

```java
public class ProductManager {
    private final Session session;
    private final DataSet products = DataSet.of("ecommerce", "products");
    
    public void createProduct(String productId, Map<String, Object> attributes) {
        session.insert(products.id(productId))
            .bin("name").setTo("Product")
            .bin("attributes").setTo(attributes)
            .bin("stats").setTo(Map.of(
                "views", 0,
                "likes", 0,
                "purchases", 0
            ))
            .execute();
    }
    
    public void updateAttribute(String productId, String key, Object value) {
        session.update(products.id(productId))
            .onMapKey("attributes", key).setTo(value)
            .execute();
    }
    
    public void incrementView(String productId) {
        session.update(products.id(productId))
            .onMapKey("stats", "views").increment(1)
            .execute();
    }
    
    public Map<String, Object> getAttributes(String productId) {
        RecordStream result = session.query(products.id(productId))
            .bin("attributes").get()
            .execute();
            
        if (result.hasNext()) {
            return (Map<String, Object>) result.next().recordOrThrow().getValue("attributes");
        }
        return Map.of();
    }
    
    public Map<String, Long> getStats(String productId) {
        RecordStream result = session.query(products.id(productId))
            .bin("stats").get()
            .execute();
            
        if (result.hasNext()) {
            return (Map<String, Long>) result.next().recordOrThrow().getValue("stats");
        }
        return Map.of();
    }
}
```

## Next Steps

- **[Working with Lists](./lists.md)** - Ordered collection operations
- **[Nested Operations](./nested-operations.md)** - Complex nested CDT structures
- **[API Reference: OperationBuilder](../../api/operation-builder.md)** - Complete API documentation

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [Troubleshooting Guide](../../troubleshooting/README.md)
