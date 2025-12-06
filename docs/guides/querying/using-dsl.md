# Using the DSL

Learn how to build type-safe, compile-time checked queries with the Aerospike Domain Specific Language (DSL).

## Goal

By the end of this guide, you'll know how to:
- Write type-safe query expressions instead of strings
- Combine multiple conditions with logical operators
- Perform comparisons on different data types
- Understand the benefits of using the DSL

## Prerequisites

- [Simple Queries](./simple-queries.md) completed
- Familiarity with Java static imports and builder patterns

---

## What is the DSL?

The DSL provides a fluent, programmatic way to build query filters. It offers a type-safe alternative to writing raw string-based `where` clauses.

### String-Based `where` Clause (The "Old" Way)

```java
// Prone to typos, no compile-time checks, hard to refactor
session.query(users)
    .where("$.age >= 25 and $.city == 'New York'")
    .execute();
```

### DSL-Based `where` Clause (The "New" Way)

```java
import static com.aerospike.dsl.Dsl.*;

// Type-safe, compile-time checked, easy to refactor
session.query(users)
    .where(
        and(
            longBin("age").gte(25),
            stringBin("city").eq("New York")
        )
    )
    .execute();
```

---

## Getting Started with the DSL

### 1. Static Import

To use the DSL effectively, static import the `Dsl` class:

```java
import static com.aerospike.dsl.Dsl.*;
```

### 2. Bin Type References

The DSL uses typed bin references to ensure type safety.

- `longBin("binName")` - For numeric integer bins
- `doubleBin("binName")` - For numeric floating-point bins
- `stringBin("binName")` - For string bins
- `blobBin("binName")` - For byte array bins
- `booleanBin("binName")` - For boolean bins

### 3. Comparison Operators

Each typed bin reference provides comparison methods:

| Method | Operator | Description |
|---|---|---|
| `.eq()` | `==` | Equal |
| `.neq()` | `!=` | Not Equal |
| `.gt()` | `>` | Greater Than |
| `.gte()` | `>=` | Greater Than or Equal |
| `.lt()` | `<` | Less Than |
| `.lte()` | `<=` | Less Than or Equal |
| `.in()` | `IN` | In a list of values |
| `.contains()` | `CONTAINS` | For collections |
| `.matches()` | `LIKE` | String pattern matching (regex) |

---

## Building Queries with the DSL

### Simple Equality Query

**String-based**:
```java
.where("$.city == 'London'")
```

**DSL-based**:
```java
.where(stringBin("city").eq("London"))
```

### Numeric Range Query

**String-based**:
```java
.where("$.age >= 18 and $.age < 65")
```

**DSL-based**:
```java
.where(
    and(
        longBin("age").gte(18),
        longBin("age").lt(65)
    )
)
```

### Logical Operators

Combine expressions with `and()`, `or()`, and `not()`.

**`and()`**: All conditions must be true.
```java
.where(
    and(
        stringBin("status").eq("active"),
        longBin("loginCount").gt(10),
        booleanBin("premiumUser").isTrue()
    )
)
```

**`or()`**: At least one condition must be true.
```java
.where(
    or(
        stringBin("country").eq("USA"),
        stringBin("country").eq("Canada"),
        stringBin("country").eq("Mexico")
    )
)
```
This is equivalent to `stringBin("country").in("USA", "Canada", "Mexico")`.

**`not()`**: Negates a condition.
```java
.where(not(stringBin("status").eq("archived")))
```

### String Matching (Regex)

Use `.matches()` for regular expression matching.

```java
// Find users with a @gmail.com email address
.where(stringBin("email").matches(".*@gmail\\.com$"))
```

---

## Benefits of Using the DSL

### 1. Type Safety

The compiler catches errors for you.

```java
// ❌ Won't compile: Comparing a number bin to a string
.where(longBin("age").eq("thirty"))

// ✅ Compiles: Correct type
.where(longBin("age").eq(30))
```

### 2. Refactoring Safety

If you rename a bin, your IDE's refactoring tools can update the DSL code. String-based queries must be updated manually.

```java
// Refactoring "age" to "userAge"
// IDE will automatically update this:
.where(longBin("userAge").gte(18))

// IDE will NOT update this, causing a runtime error:
.where("$.age >= 18")
```

### 3. Improved Readability and Maintainability

Complex queries are easier to read and modify.

**String-based (Hard to read)**:
```java
.where("($.status == 'active' or $.status == 'pending') and $.loginCount > 0 and not $.isFlagged == true")
```

**DSL-based (Easy to read)**:
```java
.where(
    and(
        or(
            stringBin("status").eq("active"),
            stringBin("status").eq("pending")
        ),
        longBin("loginCount").gt(0),
        not(booleanBin("isFlagged").isTrue())
    )
)
```

### 4. No Injection Risk

The DSL prevents query injection vulnerabilities by design, as it doesn't rely on string concatenation.

```java
// ❌ Vulnerable string-based query
String userInput = "' OR '1'='1";
.where("$.username == '" + userInput + "'") // Dangerous!

// ✅ Safe DSL query
String userInput = "' OR '1'='1";
.where(stringBin("username").eq(userInput)) // Safe!
```

---

## Complete Example: Advanced User Search

```java
import static com.aerospike.dsl.Dsl.*;
import java.util.List;

public class UserSearchService {
    private final Session session;
    private final DataSet users;
    
    public UserSearchService(Session session) {
        this.session = session;
        this.users = DataSet.of("app", "users");
    }
    
    public List<User> findActivePremiumUsers(String country, int minLogins) {
        
        var filter = and(
            booleanBin("isActive").isTrue(),
            booleanBin("isPremium").isTrue(),
            stringBin("country").eq(country),
            longBin("loginCount").gte(minLogins)
        );
        
        RecordStream results = session.query(users)
            .where(filter)
            .execute();
            
        return results.stream()
            .map(this::mapToUser)
            .collect(Collectors.toList());
    }
    
    public List<User> findRecentlyActiveAdminsOrModerators() {
        
        var filter = and(
            or(
                stringBin("role").eq("admin"),
                stringBin("role").eq("moderator")
            ),
            longBin("lastLogin").gte(System.currentTimeMillis() - Duration.ofDays(30).toMillis())
        );
        
        RecordStream results = session.query(users)
            .where(filter)
            .execute();
            
        return results.stream()
            .map(this::mapToUser)
            .collect(Collectors.toList());
    }
    
    private User mapToUser(RecordResult kr) {
        // Mapping logic...
        return new User(...);
    }
}
```

---

## Best Practices

### ✅ DO

**Use `static import` for `Dsl`**
This makes the code much cleaner and more readable.

**Reuse `BooleanExpression` objects**
```java
// Define common filters once
private static final BooleanExpression IS_ACTIVE = booleanBin("isActive").isTrue();
private static final BooleanExpression IS_PREMIUM = booleanBin("isPremium").isTrue();

// Reuse them
.where(and(IS_ACTIVE, IS_PREMIUM))
```

**Combine with other query builders**
```java
session.query(users)
    .where(longBin("age").gt(21))
    .readingOnlyBins("name", "email")
    .limit(100)
    .execute();
```

### ❌ DON'T

**Don't mix DSL objects with string concatenation**
The DSL is designed to replace string-based queries, not to be used within them.

---

## Next Steps

- **[Filtering with WHERE](./filtering.md)** - See a side-by-side comparison of string vs. DSL for more complex scenarios.
- **[API Reference](../../api/dsl/README.md)** - For a complete list of all DSL methods and operators.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
