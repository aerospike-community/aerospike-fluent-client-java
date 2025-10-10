# DSL Expressions

Learn about the different types of expressions in the DSL and how to combine them to create complex logic.

## Overview

The DSL is built around a hierarchy of expression classes that represent values, operations, and conditions. All expressions inherit from `DslExpression`. The most important subclass is `BooleanExpression`, which is used for filtering.

## Key Expression Types

- **`DslExpression`**: The base class for all expressions.
- **`BooleanExpression`**: An expression that evaluates to true or false. Used in `where()` clauses.
- **`LongExpression`**, **`DoubleExpression`**, **`StringExpression`**, etc.: Typed expressions that represent a value of a specific type. These are typically created from typed bins or literals.
- **`ArithmeticExpression`**: Represents an arithmetic operation (add, subtract, etc.) between numeric expressions.
- **`LogicalExpression`**: Represents a logical operation (`and`, `or`) between `BooleanExpression`s.
- **`Comparison`**: Represents a comparison (`eq`, `gt`, `lt`, etc.) between two expressions, resulting in a `BooleanExpression`.

---

## Combining Expressions

Expressions are designed to be combined in a fluent, chainable manner.

### Logical Combinations (`and`, `or`)

You can combine `BooleanExpression`s using `and()` and `or()`.

```java
import static com.aerospike.dsl.Dsl.*;

// (age > 30 AND city == "New York") OR country == "USA"
BooleanExpression filter = longBin("age").gt(val(30))
    .and(stringBin("city").eq(val("New York")))
    .or(stringBin("country").eq(val("USA")));
```

### Arithmetic Operations

You can perform arithmetic on numeric expressions (`LongExpression`, `DoubleExpression`).

```java
// where salary + bonus > 100000
BooleanExpression filter = longBin("salary").add(longBin("bonus"))
    .gt(val(100000L));
```

---

## Example Usage

This example shows a more complex expression that combines logical and arithmetic operations.

```java
import com.aerospike.dsl.BooleanExpression;
import static com.aerospike.dsl.Dsl.*;

// Find active users who are either under 30 or have a loyalty score
// greater than 90, and whose last login was within the last year.
BooleanExpression filter = booleanBin("isActive").isTrue()
    .and(
        longBin("age").lt(val(30))
            .or(longBin("loyaltyScore").gt(val(90)))
    )
    .and(
        longBin("lastLogin").gt(val(System.currentTimeMillis() - 31536000000L))
    );

RecordStream results = session.query(users)
    .where(filter)
    .execute();
```

---

## Next Steps

- **[Dsl Class](./dsl.md)**: The entry point for creating expressions.
- **[Type-Specific Bins](./bins.md)**: See the specific operations available for each typed bin.
- **[Using the DSL](../../guides/querying/using-dsl.md)**: A how-to guide on using the DSL for querying.
