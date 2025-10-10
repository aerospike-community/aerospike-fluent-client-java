# Nested CDT Operations

Learn how to work with nested lists and maps in Aerospike using the Fluent Client.

## Overview

Aerospike supports nested Complex Data Types (CDTs) - lists within maps, maps within lists, and even deeper nesting. The Fluent Client provides context-based navigation to manipulate nested structures atomically.

### Nested Structure Diagram

```
┌──────────────────────────────────────────────────────────────┐
│  Record                                                       │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Bin: "userData"  (Map)                                │  │
│  │  ┌──────────────────────────────────────────────────┐  │  │
│  │  │ Key: "preferences" → Map                         │  │  │
│  │  │   ├─ "theme" → "dark"                            │  │  │
│  │  │   └─ "notifications" → List ["email", "push"]    │  │  │
│  │  │                                                   │  │  │
│  │  │ Key: "history" → List                            │  │  │
│  │  │   ├─ [0] → Map {action: "view", id: 123}        │  │  │
│  │  │   └─ [1] → Map {action: "purchase", id: 456}    │  │  │
│  │  └──────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘

Path Examples:
• userData → preferences → theme
• userData → preferences → notifications → [0]
• userData → history → [0] → action
```

## Basic Nested Operations

### Map Inside Map

```java
DataSet users = DataSet.of("app", "users");

// Structure: {profile: {settings: {theme: "dark"}}}

// Set nested value
session.update(users.id("alice"))
    .onMapKey("profile", "settings")
        .onMapKey("theme").setTo("light")
    .execute();

// Get nested value
session.query(users.id("alice"))
    .onMapKey("profile", "settings")
        .onMapKey("theme").get()
    .execute();

// Update multiple nested values
session.update(users.id("alice"))
    .onMapKey("profile", "settings")
        .onMapKey("theme").setTo("light")
    .onMapKey("profile", "settings")
        .onMapKey("language").setTo("en")
    .execute();
```

### List Inside Map

```java
// Structure: {userData: {tags: ["admin", "premium"]}}

// Append to nested list
session.update(users.id("alice"))
    .onMapKey("userData", "tags")
        .listAppend("verified")
    .execute();

// Get from nested list
session.query(users.id("alice"))
    .onMapKey("userData", "tags")
        .getByIndex(0)
    .execute();

// Remove from nested list
session.update(users.id("alice"))
    .onMapKey("userData", "tags")
        .removeByValue("premium")
    .execute();
```

### Map Inside List

```java
// Structure: {events: [{type: "login", time: 12345}, {type: "logout", time: 12350}]}

// Update map within list
session.update(users.id("alice"))
    .onList("events").atIndex(0)
        .onMapKey("time").setTo(System.currentTimeMillis())
    .execute();

// Get value from map in list
session.query(users.id("alice"))
    .onList("events").atIndex(0)
        .onMapKey("type").get()
    .execute();
```

### List Inside List

```java
// Structure: {categories: [["electronics", "phones"], ["home", "furniture"]]}

// Append to nested list
session.update(products.id("product123"))
    .onList("categories").atIndex(0)
        .listAppend("tablets")
    .execute();

// Get from deeply nested list
session.query(products.id("product123"))
    .onList("categories").atIndex(0)
        .getByIndex(1)
    .execute();
```

## Using CDT Context

For more complex nesting, use CDT context (requires traditional client API):

```java
import com.aerospike.client.cdt.CTX;
import com.aerospike.client.Value;

// Path: userData → history → [0] → tags → [1]
CTX[] context = new CTX[] {
    CTX.mapKey(Value.get("userData")),
    CTX.mapKey(Value.get("history")),
    CTX.listIndex(0),
    CTX.mapKey(Value.get("tags"))
};

// Use with operations (requires underlying client access)
```

## Common Nested Patterns

### Pattern 1: User Profile with Nested Settings

```java
public class UserProfileManager {
    private final Session session;
    private final DataSet users;
    
    public void updateTheme(String userId, String theme) {
        session.update(users.id(userId))
            .onMapKey("profile", "settings")
                .onMapKey("theme").setTo(theme)
            .execute();
    }
    
    public void addNotificationChannel(String userId, String channel) {
        session.update(users.id(userId))
            .onMapKey("profile", "notifications")
                .listAppendUnique(channel, true)
            .execute();
    }
    
    public Map<String, Object> getSettings(String userId) {
        RecordStream result = session.query(users.id(userId))
            .onMapKey("profile", "settings").get()
            .execute();
            
        if (result.hasNext()) {
            return (Map<String, Object>) result.next().record.getValue("profile");
        }
        return Map.of();
    }
}
```

### Pattern 2: Event History with Metadata

```java
public class EventTracker {
    private final Session session;
    private final DataSet entities;
    
    public void logEvent(String entityId, String eventType, Map<String, Object> metadata) {
        Map<String, Object> event = Map.of(
            "type", eventType,
            "timestamp", System.currentTimeMillis(),
            "metadata", metadata
        );
        
        session.update(entities.id(entityId))
            .onList("events").append(event)
            .execute();
    }
    
    public void updateEventMetadata(String entityId, int eventIndex, String key, Object value) {
        session.update(entities.id(entityId))
            .onList("events").atIndex(eventIndex)
                .onMapKey("metadata", key).setTo(value)
            .execute();
    }
}
```

### Pattern 3: Product Categories (Multi-level)

```java
public class CategoryManager {
    private final Session session;
    private final DataSet products;
    
    // Structure: {categories: {primary: ["electronics"], secondary: ["phones", "tablets"]}}
    
    public void addPrimaryCategory(String productId, String category) {
        session.update(products.id(productId))
            .onMapKey("categories", "primary")
                .listAppendUnique(category, true)
            .execute();
    }
    
    public void addSecondaryCategory(String productId, String category) {
        session.update(products.id(productId))
            .onMapKey("categories", "secondary")
                .listAppendUnique(category, true)
            .execute();
    }
    
    public List<String> getPrimaryCategories(String productId) {
        RecordStream result = session.query(products.id(productId))
            .onMapKey("categories", "primary").get()
            .execute();
            
        if (result.hasNext()) {
            return (List<String>) result.next().record.getValue("categories");
        }
        return List.of();
    }
}
```

## Best Practices

### Depth Limits

**Recommended**:
- **2-3 levels deep**: Optimal performance
- **4-5 levels deep**: Acceptable for most use cases
- **> 5 levels deep**: Consider restructuring data

**Example of too deep**:
```
user → profile → settings → notifications → channels → email → options → frequency
❌ Too many levels - hard to maintain and query
```

**Better structure**:
```
user → notificationSettings → emailFrequency
✅ Flattened, easier to work with
```

### When to Use Nested CDTs

✅ **Use Nested CDTs When**:
- **Logical grouping** - Related data naturally nested (profile → settings)
- **Atomic updates** - Need to update related items together
- **Small collections** - Each level has < 100 items
- **Read-heavy** - More reads than writes to nested data
- **Clear hierarchy** - Natural parent-child relationships

**Examples**: User profiles, product metadata, configuration trees

❌ **Avoid Nested CDTs When**:
- **Deep nesting** - More than 5 levels deep
- **Large collections** - Any level has > 1000 items
- **Frequent restructuring** - Schema changes often
- **Complex queries** - Need joins or aggregations
- **Write-heavy** - Many concurrent nested updates

**Alternatives**: Separate records, denormalization, relational model

### Performance Considerations

**Nested operations are slower** than top-level:
- Each level adds lookup overhead
- Path traversal takes time
- Consider caching frequently accessed nested paths

**Optimize by**:
- Keeping nesting shallow (2-3 levels)
- Using small collections at each level
- Batching multiple nested operations
- Denormalizing hot paths

## Complete Example: User Activity System

```java
public class UserActivitySystem {
    private final Session session;
    private final DataSet users = DataSet.of("app", "users");
    
    // Structure:
    // {
    //   profile: {
    //     settings: {theme: "dark", lang: "en"},
    //     stats: {logins: 10, posts: 5}
    //   },
    //   activity: [
    //     {type: "login", time: 1234567890, metadata: {ip: "1.2.3.4"}},
    //     {type: "post", time: 1234567900, metadata: {id: "post123"}}
    //   ]
    // }
    
    public void updateSetting(String userId, String key, Object value) {
        session.update(users.id(userId))
            .onMapKey("profile", "settings")
                .onMapKey(key).setTo(value)
            .execute();
    }
    
    public void incrementStat(String userId, String statName) {
        session.update(users.id(userId))
            .onMapKey("profile", "stats")
                .onMapKey(statName).increment(1)
            .execute();
    }
    
    public void logActivity(String userId, String type, Map<String, Object> metadata) {
        Map<String, Object> activity = Map.of(
            "type", type,
            "time", System.currentTimeMillis(),
            "metadata", metadata
        );
        
        session.update(users.id(userId))
            .onList("activity").append(activity)
            .execute();
        
        // Keep only last 50 activities
        session.update(users.id(userId))
            .onList("activity")
                .removeByIndexRange(0, -50)
            .execute();
    }
    
    public Map<String, Object> getUserProfile(String userId) {
        RecordStream result = session.query(users.id(userId))
            .onMapKey("profile").get()
            .execute();
            
        if (result.hasNext()) {
            return (Map<String, Object>) result.next().record.getValue("profile");
        }
        return Map.of();
    }
    
    public List<Map<String, Object>> getRecentActivity(String userId, int count) {
        RecordStream result = session.query(users.id(userId))
            .onList("activity")
                .getByIndexRange(-count, count)
            .execute();
            
        if (result.hasNext()) {
            return (List<Map<String, Object>>) result.next().record.getValue("activity");
        }
        return List.of();
    }
}
```

## Troubleshooting Nested Operations

### Path Not Found

```java
// ❌ Error: Path doesn't exist
session.update(users.id("alice"))
    .onMapKey("profile", "nonexistent")
        .onMapKey("value").setTo(123)
    .execute();

// ✅ Solution: Create intermediate structures first
session.update(users.id("alice"))
    .onMapKey("profile", "nonexistent").setTo(Map.of())
    .execute();

session.update(users.id("alice"))
    .onMapKey("profile", "nonexistent")
        .onMapKey("value").setTo(123)
    .execute();
```

### Index Out of Bounds

```java
// ❌ Error: List index doesn't exist
session.update(users.id("alice"))
    .onList("events").atIndex(100)
        .onMapKey("updated").setTo(true)
    .execute();

// ✅ Solution: Check list size first or use append
session.update(users.id("alice"))
    .onList("events").append(Map.of("updated", true))
    .execute();
```

## Next Steps

- **[Working with Lists](./lists.md)** - List operations in detail
- **[Working with Maps](./maps.md)** - Map operations in detail
- **[API Reference](../../api/README.md)** - Complete API documentation

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md)
