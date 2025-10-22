# DSL Class

The `Dsl` class is the main entry point for building type-safe query expressions.

## Overview

The `Dsl` class is a static factory class that provides methods for creating typed bin expressions, literals, and other expression components. It is the starting point for constructing complex, type-safe filter and projection expressions that can be used in queries and operations.

The `Dsl` class is designed to be used with a static import to improve readability:
```java
import static com.aerospike.dsl.Dsl.*;
```

## Key Concepts

- **Typed Bins**: The `Dsl` class provides methods like `longBin()`, `stringBin()`, `doubleBin()`, etc., to create typed representations of bins. These typed bins provide methods for type-safe comparisons and operations.
- **Literals**: The `val()` methods are used to create typed literal expressions from Java values.
- **Expressions**: The `Dsl` class and the typed bins are used to construct `BooleanExpression` objects for filtering and other `DslExpression` objects for projections and calculations.

---

## Core Methods

### Bin Creation

| Method | Description |
| --- | --- |
| `longBin(String name)` | Creates a `LongBin` expression for a bin containing a long value. |
| `doubleBin(String name)` | Creates a `DoubleBin` expression for a bin containing a double value. |
| `stringBin(String name)` | Creates a `StringBin` expression for a bin containing a string value. |
| `booleanBin(String name)` | Creates a `BooleanBin` expression for a bin containing a boolean value. |
| `blobBin(String name)` | Creates a `BlobBin` expression for a bin containing a byte array. |

**Example:**
```java
import static com.aerospike.dsl.Dsl.*;

LongBin ageBin = longBin("age");
StringBin nameBin = stringBin("name");
```

### Literal Creation

| Method | Description |
| --- | --- |
| `val(Long value)` | Creates a `LongLiteralExpression` from a long value. |
| `val(Double value)` | Creates a `DoubleLiteralExpression` from a double value. |
| `val(String value)` | Creates a `StringLiteralExpression` from a string value. |
| `val(Boolean value)` | Creates a `BooleanLiteralExpression` from a boolean value. |
| `val(byte[] value)` | Creates a `BlobLiteralExpression` from a byte array. |

**Example:**
```java
BooleanExpression filter = longBin("age").gt(val(30));
```

---

## Example Usage

The `Dsl` class is typically used to build a `BooleanExpression` that is passed to the `where()` method of a `QueryBuilder`.

```java
import com.aerospike.query.QueryBuilder;
import com.aerospike.dsl.BooleanExpression;
import static com.aerospike.dsl.Dsl.*;

// Build a type-safe filter expression
BooleanExpression filter = longBin("age").gt(val(30))
    .and(stringBin("city").eq(val("New York")));

// Use the expression in a query
RecordStream results = session.query(users)
    .where(filter)
    .execute();
```

---

## Next Steps

- **[Expressions](./expressions.md)**: Learn more about the different types of expressions and how to combine them.
- **[Type-Specific Bins](./bins.md)**: See the specific operations available for each typed bin.
- **[Using the DSL](../../guides/querying/using-dsl.md)**: A how-to guide on using the DSL for querying.
