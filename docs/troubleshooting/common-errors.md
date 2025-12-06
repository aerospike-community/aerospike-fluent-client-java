# Common Errors & Solutions

A comprehensive guide to common errors, their causes, and how to solve them.

## How to Use This Guide

1.  **Find Your Error**: Use `Cmd/Ctrl + F` to search for your error message or exception class.
2.  **Understand the Cause**: Review the common causes, ranked by likelihood.
3.  **Apply the Solution**: Follow the steps and code examples to fix the issue.

---

## Connection Errors

Errors that occur when the client tries to connect to or communicate with the Aerospike cluster.

### 1. Connection Refused

**Error Message:** `Connection refused: localhost/127.0.0.1:3000`

**Exception Class:** `com.aerospike.client.AerospikeException.Connection`

**Common Causes:**
1.  **Aerospike Server Not Running**: The most common cause is that the database server is not running on the target machine.
2.  **Incorrect Host/Port**: The client is configured with the wrong IP address or port for the Aerospike server.
3.  **Firewall or Network ACL**: A firewall on the client, server, or network is blocking the connection on port 3000.
4.  **Docker Networking Issue**: If running Aerospike in Docker, the port may not be correctly exposed to the host machine.

**How to Fix:**

**Option 1: Verify Aerospike Server Status**
```bash
# If using Docker
docker ps | grep aerospike

# If using a system service
sudo systemctl status aerospike
```
If the server is not running, start it.

**Option 2: Test Network Connectivity**
```bash
# Use telnet to check if the port is open
telnet localhost 3000
```
If telnet cannot connect, check firewall rules and the server's `aerospike.conf` to ensure it's listening on the correct interface and port.

**Option 3: Correct Docker Configuration**
For Docker on macOS or Windows, sometimes `localhost` doesn't work.
```java
// Try using host.docker.internal
Cluster cluster = new ClusterDefinition("host.docker.internal", 3000).connect();
```

---

### 2. Operation Timed Out

**Error Message:** `Client timeout: socket ...`

**Exception Class:** `com.aerospike.client.AerospikeException.Timeout`

**Common Causes:**
1.  **Network Latency**: High latency between the client and the server.
2.  **Server Overload**: The Aerospike server is too busy to respond within the client's configured timeout.
3.  **Client Timeout Too Short**: The configured `totalTimeout` or `socketTimeout` on the client policy is too aggressive for the operation being performed.
4.  **Long-Running Scans**: A scan or query operation is processing a massive amount of data and takes longer than the timeout.

**How to Fix:**

**Option 1: Increase Client Timeout**
```java
// Create a custom behavior with a longer timeout
Behavior longTimeout = Behavior.DEFAULT.deriveWithChanges("long-timeout", builder ->
    builder.forAllOperations()
        .abandonCallAfter(Duration.ofSeconds(10)) // 10-second timeout
        .withSocketTimeout(Duration.ofSeconds(10))
    .done()
);
Session session = cluster.createSession(longTimeout);

// Now perform the operation
session.query(myDataSet).where("...").execute();
```

**Option 2: Check Server Performance**
Use `asadm` or `asinfo` to check for "hot keys," slow queries, or high resource utilization on the server.

**Prevention:**
- Tune client policies to have realistic timeouts.
- For large scans, consider using partition-based queries to break the work into smaller chunks.
- Monitor server and network health.

---

### 3. Not Authenticated

**Error Message:** `Not authenticated`

**Exception Class:** `com.aerospike.exception.AuthenticationException` (or `AerospikeException` with `ResultCode.NOT_AUTHENTICATED`)

**Common Causes:**
1.  **Missing Credentials**: The cluster requires authentication, but the client did not provide a username and password.
2.  **Incorrect Credentials**: The provided username or password is incorrect.
3.  **Security Not Enabled**: The client provided credentials, but the server does not have security enabled.

**How to Fix:**

Ensure you provide the correct credentials when building the `ClusterDefinition`.
```java
// Correctly provide credentials
Cluster cluster = new ClusterDefinition("secure.host.com", 3000)
    .withNativeCredentials("admin", "correct-password")
    .connect();
```
Verify that the user exists and security is enabled on the server.

---

## Data Operation Errors

Errors related to creating, reading, updating, or deleting records.

### 4. Record Exists

**Error Message:** `Error code 5: Record already exists`

**Exception Class:** `com.aerospike.client.AerospikeException.RecordExists`

**Common Causes:**
1.  **Using `insert()` Instead of `upsert()`**: `insert()` is designed to fail if the record already exists. This is its primary function.

**How to Fix:**

**Option 1: Use `upsert()`**
If your intention is to create the record if it doesn't exist or update it if it does, use `upsert()`.
```java
// This will create or replace the record
session.upsert(users.id("alice"))
    .bin("email").setTo("alice@new.com")
    .execute();
```

**Option 2: Handle the Exception**
If you must use `insert()` (e.g., to guarantee a new user registration), catch the exception and handle it gracefully.
```java
try {
    session.insert(users.id("alice"))
        .bin("name").setTo("Alice")
        .execute();
} catch (AerospikeException.RecordExists e) {
    // This is expected if the user already exists.
    logger.warn("User 'alice' already exists. Registration failed.");
    // Return an error to the user
}
```

---

### 5. Record Not Found

**Error Message:** `Error code 2: Key not found`

**Exception Class:** `com.aerospike.client.AerospikeException.RecordNotFound`

**Common Causes:**
1.  **Reading or Deleting a Non-Existent Key**: The key you are trying to operate on does not exist in the database.
2.  **Using `update()` Instead of `upsert()`**: `update()` operations will fail if the record does not exist.

**How to Fix:**

**Option 1: Check for Existence Before Operation**
The Fluent Client's `query()` method returns a `RecordStream`, which is empty if the record is not found.
```java
RecordStream result = session.query(users.id("unknown-user")).execute();
if (result.hasNext()) {
    // Record exists, proceed with update or other logic
    session.update(users.id("unknown-user")).bin("last_login").setTo(now).execute();
} else {
    // Record does not exist
    System.out.println("User not found.");
}
```

**Option 2: Use `upsert()`**
If the desired behavior is to create the record if it's missing, use `upsert()`.

---

### 6. Generation Error

**Error Message:** `Error code 3: Generation error`

**Exception Class:** `com.aerospike.exception.GenerationException`

**Common Causes:**
1.  **Optimistic Locking Failure**: You are using `.ensureGenerationIs(gen)`, and the record on the server has been modified by another process since you last read it. Its generation has increased, so your expected generation is now stale.

**How to Fix:**

Implement a read-modify-write loop with retries.
```java
int maxRetries = 3;
for (int i = 0; i < maxRetries; i++) {
    try {
        RecordStream result = session.query(accounts.id("acc123")).execute();
        if (result.hasNext()) {
            RecordResult record = result.next();
            int currentBalance = record.recordOrThrow().getInt("balance");
            int generation = record.recordOrThrow().generation;

            // Perform modification
            session.update(accounts.id("acc123"))
                .bin("balance").setTo(currentBalance + 10)
                .ensureGenerationIs(generation) // Set expected generation
                .execute();

            return; // Success
        }
    } catch (GenerationException e) {
        if (i == maxRetries - 1) {
            throw new RuntimeException("Failed to update account after multiple retries.", e);
        }
        // Wait and retry
    }
}
```

---

## Query & Index Errors

### 7. Index Not Found

**Error Message:** `Error code 201: Index not found`

**Exception Class:** `com.aerospike.client.AerospikeException`

**Common Causes:**
1.  **Querying a Bin Without a Secondary Index**: You are attempting to filter a query on a bin value, but no secondary index has been created for that bin on the server.
2.  **Typo in Index or Bin Name**: The name of the bin in your query does not match the name of the indexed bin.

**How to Fix:**

**Option 1: Create the Secondary Index**
Use `aql` or another management tool to create the necessary index.
```sql
-- In AQL
CREATE INDEX user_age_idx ON test.users (age) NUMERIC
```

**Option 2: Use a Scan (Not for Production)**
If you are in a development environment, you can allow the query to fall back to a full scan by changing the query policy. **Warning: This is very slow and should not be used in production.**

```java
Behavior allowScan = Behavior.DEFAULT.deriveWithChanges("allow-scan", builder ->
    builder.onQuery()
        .failOnFilteredScan(false)
    .done()
);
Session scanSession = cluster.createSession(allowScan);
scanSession.query(users).where("$.age > 30").execute();
```

---

*This is a partial list. More errors will be added for Security, Configuration, and Resource categories.*
