# DSL Type-Specific Bins

Learn about the type-specific bin classes in the DSL and the operations they provide.

## Overview

When you create a typed bin using the `Dsl` class (e.g., `Dsl.longBin("myBin")`), you get an object that represents that bin and provides methods for type-safe operations. These classes (`LongBin`, `StringBin`, etc.) are the building blocks for creating expressions.

Each typed bin class implements a corresponding expression interface (e.g., `LongBin` implements `LongExpression`), which provides the methods for comparisons and operations.

---

## Common Operations

All typed bin classes support a common set of operations for comparison and logical combination.

### Comparison Operations

These methods create a `Comparison` expression, which results in a `BooleanExpression`.

| Method | Description |
| --- | --- |
| `eq(value)` | Equal to |
| `ne(value)` | Not equal to |
| `gt(value)` | Greater than |
| `lt(value)` | Less than |
| `gte(value)` | Greater than or equal to |
| `lte(value)` | Less than or equal to |

The `value` can be a literal of the same type or another expression of the same type.

**Example:**
```java
import static com.aerospike.dsl.Dsl.*;

// age == 30
BooleanExpression filter1 = longBin("age").eq(val(30));

// city != "London"
BooleanExpression filter2 = stringBin("city").ne(val("London"));

// score > other_score
BooleanExpression filter3 = doubleBin("score").gt(doubleBin("other_score"));
```

### Logical Operations

These methods allow you to combine boolean expressions.

| Method | Description |
| --- | --- |
| `and(BooleanExpression other)` | Logical AND |
| `or(BooleanExpression other)` | Logical OR |
| `not()` | Logical NOT |

**Example:**
```java
// age > 30 AND city == "New York"
BooleanExpression filter = longBin("age").gt(val(30))
    .and(stringBin("city").eq(val("New York")));
```

---

## Numeric Bins (`LongBin`, `DoubleBin`)

Numeric bins provide additional methods for arithmetic operations.

### Arithmetic Operations

These methods create an `ArithmeticExpression`.

| Method | Description |
| --- | --- |
| `add(value)` | Addition |
| `sub(value)` | Subtraction |
| `mul(value)` | Multiplication |
| `div(value)` | Division |

The `value` can be a numeric literal or another numeric expression.

**Example:**
```java
import static com.aerospike.dsl.Dsl.*;

// salary + bonus > 100000
BooleanExpression filter = longBin("salary").add(longBin("bonus"))
    .gt(val(100000L));

// (price * tax_rate) < 50.0
BooleanExpression filter2 = doubleBin("price").mul(doubleBin("tax_rate"))
    .lt(val(50.0));
```

---

## Boolean Bins (`BooleanBin`)

Boolean bins provide methods for checking truthiness.

| Method | Description |
| --- | --- |
| `isTrue()` | Checks if the bin value is true. |
| `isFalse()` | Checks if the bin value is false. |

**Example:**
```java
import static com.aerospike.dsl.Dsl.*;

// Find all active users
BooleanExpression filter = booleanBin("isActive").isTrue();
```

---

## Next Steps

- **[Dsl Class](./dsl.md)**: The entry point for creating typed bins.
- **[Expressions](./expressions.md)**: Learn how expressions are combined.
- **[Using the DSL](../../guides/querying/using-dsl.md)**: A how-to guide on using the DSL for querying.
