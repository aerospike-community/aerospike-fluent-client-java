# Troubleshooting Connection Issues

This guide helps you diagnose and resolve common issues related to connecting to an Aerospike cluster.

## Common Error Messages

### 1. "Connection refused" or "Node not found"

**Exception:** `com.aerospike.client.AerospikeException$Connection: Error -8,10,10,2068,0,0: Node not found`

**Common Causes:**
- **Incorrect Host/Port**: The hostname or port in your `ClusterDefinition` is incorrect.
- **Aerospike Server Not Running**: The Aerospike database is not running on the specified host.
- **Firewall**: A firewall is blocking the connection between your application and the Aerospike server on the specified port (default is 3000).
- **Network Issues**: General network connectivity problems between the client and server.

**Solutions:**
- **Verify Host and Port**: Double-check the hostname and port in your `ClusterDefinition`.
- **Check Server Status**: Ensure the Aerospike server is running. You can use `systemctl status aerospike` on the server or `asinfo -v "status"` from a tool node.
- **Check Firewall Rules**: Ensure that the Aerospike port is open on any firewalls between your application and the database.
- **Test Connectivity**: Use tools like `ping` and `telnet` to verify basic network connectivity from the client machine to the server.
  ```bash
  ping <server-hostname>
  telnet <server-hostname> 3000
  ```

### 2. "Authentication failed"

**Exception:** `com.aerospike.client.AerospikeException: Error -1,10,10,2068,0,0: Authentication failed`

**Common Causes:**
- **Incorrect Username/Password**: The credentials provided in `withNativeCredentials()` are incorrect.
- **Security Not Enabled**: The client is attempting to authenticate, but the server does not have security enabled.
- **User Does Not Exist**: The user has not been created in the Aerospike database.

**Solutions:**
- **Verify Credentials**: Check your username and password.
- **Check Server Configuration**: Ensure that security is enabled in your `aerospike.conf` file if you are providing credentials.
- **Verify User in `asadm`**: Use `asadm` to verify that the user exists and has the correct permissions.

### 3. "Cluster name mismatch"

**Exception:** `com.aerospike.client.AerospikeException: Error -1,10,10,2068,0,0: Cluster name mismatch. Expected 'X', but got 'Y'`

**Common Cause:**
- You have used `validateClusterNameIs()` in your `ClusterDefinition`, but the name provided does not match the `cluster-name` configured on the server.

**Solution:**
- **Check `cluster-name`**: Verify the `cluster-name` in your `aerospike.conf` and ensure it matches the name in your client configuration. Or, remove the `validateClusterNameIs()` call if you do not need to validate the cluster name.

---

## Best Practices for Connection Management

- **Use `try-with-resources`**: Always wrap your `Cluster` object in a `try-with-resources` block to ensure the connection is closed properly.
- **Singleton `Cluster`**: For most applications, you should create a single `Cluster` object and share it. The `Cluster` object is thread-safe.
- **Use Health Checks**: Implement a health check in your application that periodically verifies the connection to the cluster (e.g., by calling `cluster.isConnected()`).

---

## Next Steps

- **[Common Errors](./common-errors.md)**: A more general guide to common errors.
- **[Configuration Guides](../../guides/configuration/)**: Review how to configure your client.
