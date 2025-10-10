# Exception Hierarchy

Complete exception reference for the Aerospike Fluent Client.

This guide provides an overview of the exception hierarchy, helping you write robust error handling logic.

## Exception Tree

The Fluent Client uses a combination of custom exceptions that wrap the standard Aerospike Java Client exceptions.

```
RuntimeException
└── AeroException (com.aerospike.exception.AeroException)
    ├── AuthenticationException
    ├── AuthorizationException
    ├── GenerationException
    ├── QuotaException
    └── SecurityException

java.lang.Exception
└── com.aerospike.client.AerospikeException
    ├── Connection
    ├── Timeout
    ├── InvalidNode
    ├── RecordExists
    ├── RecordNotFound
    └── ... (and many more)
```

---

## Fluent Client Exceptions

These are custom exceptions defined within the Fluent Client to provide more specific error handling for certain categories of failures.

### `AeroException` (Base Class)

**`com.aerospike.exception.AeroException`**

#### Description
This is the base exception for all custom exceptions thrown by the Fluent Client. It provides access to the underlying Aerospike `resultCode` and other context about the error.

#### Key Fields
- `resultCode: int`: The Aerospike result code that caused the exception.
- `inDoubt: boolean`: Indicates if the operation may have completed on the server despite the error (e.g., in case of a timeout).
- `node: Node`: The cluster node where the exception occurred.
- `policy: Policy`: The policy that was in effect for the operation.
- `iteration: int`: The retry iteration number when the exception occurred.

#### Key Methods
- `getResultCode(): int`: Returns the Aerospike result code.
- `isInDoubt(): boolean`: Returns the "in doubt" status.
- `static resultCodeToException(int, String, boolean): AeroException`: A factory method to convert a result code into the appropriate `AeroException` subclass.

#### Example
```java
import com.aerospike.exception.AeroException;

try {
    session.insert(users.id("alice"))
        .bin("name").setTo("Alice")
        .execute();
} catch (AeroException e) {
    System.err.println("An error occurred with result code: " + e.getResultCode());
    if (e.isInDoubt()) {
        System.err.println("The operation is in doubt and may have succeeded.");
    }
}
```

---

### `AuthenticationException`

**`com.aerospike.exception.AuthenticationException`**

#### Description
Thrown when there is an issue with user authentication.

#### When It's Thrown
- Invalid user credentials (username/password).
- Expired user password.
- User is not authenticated for the requested operation.

#### Common Result Codes
- `ResultCode.INVALID_USER`
- `ResultCode.INVALID_PASSWORD`
- `ResultCode.INVALID_CREDENTIAL`
- `ResultCode.EXPIRED_PASSWORD`
- `ResultCode.NOT_AUTHENTICATED`

#### Example
```java
try {
    Cluster cluster = new ClusterDefinition("localhost", 3000)
        .withNativeCredentials("admin", "wrong-password")
        .connect();
} catch (AuthenticationException e) {
    System.err.println("Authentication failed: " + e.getMessage());
}
```

---

### `AuthorizationException`

**`com.aerospike.exception.AuthorizationException`**

#### Description
Thrown when an authenticated user does not have the necessary permissions to perform an operation.

#### When It's Thrown
- User lacks the required roles or privileges for a specific action (e.g., read, write, UDF execution).

#### Common Result Codes
- `ResultCode.ROLE_VIOLATION`

---

### `GenerationException`

**`com.aerospike.exception.GenerationException`**

#### Description
Thrown during an update operation when the record's generation on the server does not match the expected generation, indicating that the record has been modified by another process since it was last read. This is key for optimistic locking.

#### When It's Thrown
- When using `.ensureGenerationIs(gen)` and the server-side generation does not match.

#### Common Result Codes
- `ResultCode.GENERATION_ERROR`

#### Example
```java
import com.aerospike.exception.GenerationException;

try {
    // Attempt to update a record with a specific generation
    session.update(users.id("alice"))
        .bin("age").add(1)
        .ensureGenerationIs(5) // Expects generation to be 5
        .execute();
} catch (GenerationException e) {
    System.err.println("Optimistic locking failed. Record was modified by another transaction.");
    // Implement retry logic here
}
```

---

### `QuotaException`

**`com.aerospike.exception.QuotaException`**

#### Description
Thrown when a user exceeds their configured resource quotas (e.g., records per second).

#### When It's Thrown
- User TPS (Transactions Per Second) quota is exceeded.
- Quotas are not enabled on the server, but the client attempts an operation that requires them.

#### Common Result Codes
- `ResultCode.QUOTA_EXCEEDED`
- `ResultCode.QUOTAS_NOT_ENABLED`
- `ResultCode.INVALID_QUOTA`

---

### `SecurityException`

**`com.aerospike.exception.SecurityException`**

#### Description
A general exception for security-related issues that are not covered by authentication or authorization exceptions.

#### When It's Thrown
- Attempting to use security features when they are not enabled on the server.
- Creating a user that already exists.
- Using a forbidden password.
- Issues with roles and privileges.

#### Common Result Codes
- `ResultCode.SECURITY_NOT_ENABLED`
- `ResultCode.USER_ALREADY_EXISTS`
- `ResultCode.FORBIDDEN_PASSWORD`
- `ResultCode.INVALID_ROLE`

---

## Underlying Aerospike Java Client Exceptions

The Fluent Client is built on top of the standard Aerospike Java Client, and you may still need to catch exceptions from the underlying client, especially `AerospikeException`.

### `com.aerospike.client.AerospikeException`

This is the base class for all exceptions in the underlying client.

#### Common Subclasses to Handle

- **`AerospikeException.Connection`**: Network connection errors.
- **`AerospikeException.Timeout`**: Operation timed out. This is a critical one to handle, as the operation might be in an "in doubt" state.
- **`AerospikeException.RecordExists`**: Thrown when using `insert()` on a key that already exists.
- **`AerospikeException.RecordNotFound`**: Thrown when trying to read or delete a key that does not exist.
- **`AerospikeException.InvalidNode`**: Indicates a problem with a specific node in the cluster.

#### Example of Handling Underlying Exceptions
```java
import com.aerospike.client.AerospikeException;

try {
    session.delete(users.id("unknown-user")).execute();
} catch (AerospikeException.RecordNotFound e) {
    System.err.println("User not found, nothing to delete.");
} catch (AerospikeException.Timeout e) {
    System.err.println("Operation timed out. Status is in doubt.");
    // Add retry or compensating logic
} catch (AerospikeException e) {
    System.err.println("An Aerospike error occurred: " + e.getMessage());
}
```

## See Also

- **[Common Errors & Solutions](../troubleshooting/common-errors.md)** - A detailed guide to common runtime errors and how to fix them.
- **[Troubleshooting Guide](../troubleshooting/README.md)** - General troubleshooting tips.
- **[Official Aerospike Result Codes](https://aerospike.com/docs/client/java/usage/errors/client_error_codes.html)** - A complete list of all possible result codes from the server.
