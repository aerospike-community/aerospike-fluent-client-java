# Transactions

Learn how to use the `TransactionalSession` to perform atomic, multi-record operations with automatic retry logic.

## Goal

By the end of this guide, you'll know how to:
- Understand the purpose of the `TransactionalSession`
- Perform a simple, void transaction
- Perform a transaction that returns a value
- Understand the automatic retry mechanism
- Differentiate between transactional and non-transactional operations

## Prerequisites

- [Core Concepts](../../concepts/README.md)
- Understanding of database transactions (ACID properties)

---

## What is `TransactionalSession`?

`TransactionalSession` is a specialized `Session` that simplifies performing multi-record transactions. It handles the complexities of beginning, committing, aborting, and retrying transactions, allowing you to focus on your business logic.

### Key Features

- **Simplified API**: Uses a lambda-based approach (`doInTransaction`) that automatically manages the transaction lifecycle.
- **Automatic Retries**: Automatically retries the entire transaction on common transient failures, such as `MRT_BLOCKED` or `MRT_VERSION_MISMATCH`.
- **Resource Cleanup**: Automatically aborts the transaction if an exception is thrown from within your code block, preventing dangling transactions.
- **Nested Transactions**: Safely handles nested calls without creating multiple transaction contexts.

---

## Getting Started

### 1. Creating a `TransactionalSession`

You create a `TransactionalSession` just like a regular `Session`, by providing a `Cluster` and a `Behavior`.

```java
import com.aerospike.Cluster;
import com.aerospike.TransactionalSession;
import com.aerospike.policy.Behavior;

// Assume cluster is an active connection
Cluster cluster = ...;

// Create a transactional session
TransactionalSession txnSession = new TransactionalSession(cluster, Behavior.DEFAULT);
```

### 2. Performing a Void Transaction

Use `doInTransaction(tx -> { ... })` for operations that do not return a value.

**Use Case**: Atomically transfer funds from one account to another.

```java
public void transferFunds(String fromAccountId, String toAccountId, double amount) {
    DataSet accounts = DataSet.of("app", "accounts");
    Key fromKey = accounts.id(fromAccountId);
    Key toKey = accounts.id(toAccountId);

    txnSession.doInTransaction(tx -> {
        // 1. Read the "from" account's balance
        RecordStream fromResult = tx.query(fromKey).readingOnlyBins("balance").execute();
        double fromBalance = fromResult.getFirst().get().recordOrThrow().getDouble("balance");

        if (fromBalance < amount) {
            throw new InsufficientFundsException("Balance too low for transfer");
        }

        // 2. Read the "to" account's balance
        RecordStream toResult = tx.query(toKey).readingOnlyBins("balance").execute();
        double toBalance = toResult.getFirst().get().recordOrThrow().getDouble("balance");

        // 3. Update both accounts within the same transaction
        tx.update(fromKey)
            .bin("balance").setTo(fromBalance - amount)
            .execute();
            
        tx.update(toKey)
            .bin("balance").setTo(toBalance + amount)
            .execute();
    });
    
    System.out.println("Transfer successful!");
}
```

**How it Works**:
- If any part of the code block fails (e.g., an exception is thrown, or a database error occurs), the entire transaction is automatically aborted, and all changes are rolled back.
- If the code block completes successfully, the transaction is automatically committed.

### 3. Performing a Value-Returning Transaction

Use `doInTransactionReturning(tx -> { ...; return value; })` for transactions that need to return a result.

**Use Case**: Create a new user and immediately return their generated profile.

```java
public UserProfile createNewUser(String username, String email) {
    DataSet users = DataSet.of("app", "users");
    Key userKey = users.id(username);

    return txnSession.doInTransactionReturning(tx -> {
        // 1. Check if the user already exists
        if (tx.query(userKey).withNoBins().execute().hasNext()) {
            throw new UserAlreadyExistsException("Username is taken");
        }

        // 2. Create the user record
        tx.insertInto(userKey)
            .bin("email").setTo(email)
            .bin("createdAt").setTo(System.currentTimeMillis())
            .execute();
            
        // 3. Create a corresponding profile record
        tx.insertInto(profiles.id(username))
            .bin("bio").setTo("New user profile")
            .execute();

        // 4. Read and return the newly created profile data
        RecordStream profileStream = tx.query(profiles.id(username)).execute();
        return mapToUserProfile(profileStream.getFirst().get().record);
    });
}
```

---

## Automatic Retry Mechanism

The `TransactionalSession` will automatically retry the **entire** block of code within `doInTransaction` if it encounters specific, recoverable server-side errors.

**Errors that trigger a retry**:
- `ResultCode.MRT_BLOCKED`
- `ResultCode.MRT_VERSION_MISMATCH`
- `ResultCode.TXN_FAILED`

This is extremely useful for handling contention in high-throughput systems.

> **Important**: Your transactional logic should be **idempotent** if possible. Since the block may be executed more than once, it should be designed to produce the same result regardless of how many times it runs.

---

## Non-Transactional Operations

Sometimes, you may need to perform an operation within a transaction block that should **not** be part of the transaction (e.g., writing to a log, fetching configuration).

Use `.notInAnyTransaction()` on an operation builder to exclude it.

```java
txnSession.doInTransaction(tx -> {
    // This write IS part of the transaction
    tx.update(dataKey).bin("value").add(1).execute();
    
    // This write is NOT part of the transaction and will be committed immediately
    tx.update(logKey)
        .bin("log").append("Updated data record")
        .notInAnyTransaction()
        .execute();
});
```

---

## Best Practices

### ✅ DO

**Keep transactions short and fast.**
Long-running transactions can hold locks and increase the likelihood of contention and retries.

**Make your transactional logic idempotent.**
Design your code to handle being run multiple times in case of a retry.

**Handle exceptions thrown from within the transaction block.**
These will cause an abort, so your application should be prepared to catch them.

**Use a dedicated `TransactionalSession` for transactional code paths.**
Mix and matching with a regular `Session` can be confusing.

### ❌ DON'T

**Don't perform slow, non-database operations inside a transaction.**
Avoid network calls, file I/O, or long computations within the `doInTransaction` block.

**Don't swallow exceptions inside the transaction block.**
If you catch an exception and don't re-throw it, the transaction may commit in an inconsistent state. Let exceptions bubble up to trigger the automatic abort.

---

## Next Steps

- **[Info Commands](./info-commands.md)** - Learn how to get metadata and statistics from the cluster.
- **[Batch Operations](../performance/batch-operations.md)** - For high-performance, non-transactional bulk operations.

---

**Questions?** Check the [FAQ](../../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues).
