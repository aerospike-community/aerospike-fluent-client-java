# `TransactionalSession`

A session that supports transactional operations with automatic retry logic.

`com.aerospike.TransactionalSession`

## Overview

The `TransactionalSession` extends the base `Session` to provide robust, multi-operation transactional capabilities. It ensures that a series of read and write operations are treated as a single atomic unit. If any operation within the transaction fails, the entire transaction is aborted, and no changes are committed.

Key features include:
- **Atomicity**: All operations within the `doInTransaction` block either succeed together or fail together.
- **Automatic Retries**: Automatically retries the entire transaction on transient, recoverable failures (like deadlocks or optimistic locking conflicts), improving resilience.
- **Resource Management**: Ensures that transaction resources are always cleaned up, automatically aborting the transaction if an exception occurs.
- **Nested Transactions**: Safely supports nested transactional calls without creating new underlying transactions.

## Creating a `TransactionalSession`

You create a `TransactionalSession` directly from a `Cluster` object.

```java
import com.aerospike.Cluster;
import com.aerospike.TransactionalSession;
import com.aerospike.policy.Behavior;

// Assume cluster is an existing, connected Cluster object
TransactionalSession txSession = cluster.createTransactionalSession(Behavior.DEFAULT);
```

## Methods

### `doInTransactionReturning(Transactional<T> operation)`

Executes a transactional operation that returns a value. The operation is defined as a lambda expression.

**Note:** This method is named differently from `doInTransaction(TransactionalVoid)` to avoid Java type inference ambiguity when using complex lambda bodies with control flow statements like `while` loops.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `operation` | `Transactional<T>` | A lambda expression that takes a `Session` as a parameter and returns a value of type `T`. All database operations inside this lambda are part of the transaction. |

**Returns:** `T` - The value returned by the lambda expression.

**Throws:**
- `AerospikeException` - If the transaction fails with a non-retryable error.
- `RuntimeException` - If any other unexpected exception occurs.

**Example: Transferring Funds**
```java
// Atomically move 100 from one account to another
long amountToTransfer = 100L;

try {
    txSession.doInTransactionReturning(tx -> {
        // Read the current balance from both accounts
        long fromBalance = tx.query(accounts.id("acc1")).execute().getFirst().get().recordOrThrow().getLong("balance");
        long toBalance = tx.query(accounts.id("acc2")).execute().getFirst().get().recordOrThrow().getLong("balance");

        // Check for sufficient funds
        if (fromBalance < amountToTransfer) {
            throw new IllegalStateException("Insufficient funds");
        }

        // Perform the updates
        tx.update(accounts.id("acc1"))
            .bin("balance").setTo(fromBalance - amountToTransfer)
            .execute();
            
        tx.update(accounts.id("acc2"))
            .bin("balance").setTo(toBalance + amountToTransfer)
            .execute();
    });
    System.out.println("Transfer successful!");

} catch (IllegalStateException e) {
    System.err.println("Transfer failed: " + e.getMessage());
} catch (AerospikeException e) {
    System.err.println("Database error during transfer: " + e.getMessage());
}
```

---

### `doInTransaction(TransactionalVoid operation)`

Executes a transactional operation that does not return a value.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `operation` | `TransactionalVoid` | A lambda expression that takes a `Session` as a parameter and returns `void`. |

**Throws:**
- `AerospikeException` - If the transaction fails with a non-retryable error.
- `RuntimeException` - If any other unexpected exception occurs.

**Example: Processing an Order**
```java
txSession.doInTransaction(tx -> {
    // 1. Decrement product inventory
    tx.update(products.id("prod123"))
        .bin("inventory").add(-1)
        .execute();

    // 2. Add item to user's order history
    tx.update(orders.id("user789"))
        .bin("items").append(Map.of("productId", "prod123", "ts", System.currentTimeMillis()))
        .execute();
});
```

## Retryable Failures

The `TransactionalSession` will automatically retry the transaction block for the following `ResultCode` values, which are considered transient:

- `ResultCode.MRT_BLOCKED`
- `ResultCode.MRT_VERSION_MISMATCH`
- `ResultCode.TXN_FAILED`

For any other `AerospikeException` or a standard `RuntimeException`, the transaction is immediately aborted, and the exception is thrown to the caller.

## Related Classes

- **[`Session`](./session.md)**: The base class for operations.
- **[`Cluster`](./cluster.md)**: Used to create the `TransactionalSession`.

## See Also

- **[Guide: Transactions](../../guides/advanced/transactions.md)** (To be created)
