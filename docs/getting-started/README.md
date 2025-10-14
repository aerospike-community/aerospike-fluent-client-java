# Getting Started with Aerospike Fluent Client

Welcome! This section will help you get up and running with the Aerospike Fluent Client for Java.

## Learning Path

Follow these guides in order for the best learning experience:

### 1. [Overview](./overview.md)
**Time**: 5 minutes

Understand what the Fluent Client is, why it exists, and whether it's right for your project.

**You'll learn:**
- What problems the Fluent Client solves
- Key differences from the traditional client
- When to use the Fluent Client

### 2. [Quick Start](./quickstart.md)
**Time**: 10 minutes

Get your first program working with hands-on code examples.

**You'll learn:**
- How to add the dependency
- How to connect to Aerospike
- How to write and read your first record

### 3. [Installation & Setup](./installation.md)
**Time**: 15 minutes

Detailed setup instructions for different environments and use cases.

**You'll learn:**
- Maven and Gradle configuration
- Java version requirements
- IDE setup
- Docker development environment

---

## Prerequisites

Before you begin, ensure you have:

- **Java 21 or higher** installed
- **Maven 3.6+** or **Gradle 7.0+** for building
- **Aerospike Server** running (see [Quick Setup](#quick-aerospike-setup) below)
- Basic knowledge of Java and databases

### Quick Aerospike Setup

Don't have Aerospike installed? Get it running in seconds with Docker:

```bash
docker run -d --name aerospike -p 3000:3000 aerospike/aerospike-server
```

Verify it's running:
```bash
docker logs aerospike
```

---

## What's Next?

After completing these guides, explore:

- **[Core Concepts](../concepts/README.md)** - Understand the architecture
- **[CRUD Operations](../guides/crud/creating-records.md)** - Learn basic operations
- **[Query Guide](../guides/querying/simple-queries.md)** - Query your data
- **[API Reference](../api/README.md)** - Detailed method documentation

---

## Need Help?

- **Stuck?** Check the [FAQ](../troubleshooting/faq.md)
- **Errors?** See [Common Errors](../troubleshooting/common-errors.md)
- **Questions?** [Open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues)
