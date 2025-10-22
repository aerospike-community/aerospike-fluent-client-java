# Quick Start Guide

Get up and running with the Aerospike Fluent Client in **10 minutes**!

## What You'll Build

A simple Java program that:
1. Connects to Aerospike
2. Writes a user record
3. Reads it back
4. Performs a query

By the end, you'll have a working foundation for building Aerospike applications.

---

## Prerequisites

Before starting, ensure you have:

- ✅ **Java 21+** installed ([Download](https://adoptium.net/))
- ✅ **Maven 3.6+** or **Gradle 7.0+**
- ✅ **Aerospike Server** running (see [Quick Setup](#step-1-start-aerospike-server) below)

Check your Java version:
```bash
java -version
# Should show: java version "21" or higher
```

---

## Step 1: Start Aerospike Server

If you don't have Aerospike running, the fastest way is with Docker:

```bash
docker run -d --name aerospike -p 3000:3000 aerospike/aerospike-server
```

Verify it's running:
```bash
docker logs aerospike | grep "service ready"
# Should see: service ready: soon there will be cake!
```

> **Don't have Docker?** See [Installation Guide](./installation.md#aerospike-server-setup) for other options.

---

## Step 2: Create a New Project

### Using Maven

Create a new directory and `pom.xml`:

```bash
mkdir aerospike-quickstart
cd aerospike-quickstart
```

Create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>aerospike-quickstart</artifactId>
    <version>1.0-SNAPSHOT</version>
    
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>com.aerospike</groupId>
            <artifactId>aerospike-fluent-client-java</artifactId>
            <version>0.1.0</version>
        </dependency>
    </dependencies>
</project>
```

### Using Gradle

Create `build.gradle`:

```groovy
plugins {
    id 'java'
    id 'application'
}

group = 'com.example'
version = '1.0-SNAPSHOT'

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.aerospike:aerospike-fluent-client-java:0.1.0'
}

application {
    mainClass = 'com.example.QuickStart'
}
```

---

## Step 3: Write Your First Program

Create `src/main/java/com/example/QuickStart.java`:

```java
package com.example;

import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.DataSet;
import com.aerospike.Session;
import com.aerospike.RecordStream;
import com.aerospike.KeyRecord;
import com.aerospike.policy.Behavior;
import com.aerospike.exception.AerospikeException;

public class QuickStart {
    public static void main(String[] args) {
        System.out.println("🚀 Starting Aerospike Fluent Client Quick Start...\n");
        
        // Step 1: Connect to Aerospike
        System.out.println("1. Connecting to Aerospike...");
        try (Cluster cluster = new ClusterDefinition("localhost", 3000)
                .connect()) {
            
            System.out.println("   ✅ Connected!\n");
            
            // Step 2: Create a session
            Session session = cluster.createSession(Behavior.DEFAULT);
            
            // Step 3: Define a dataset (namespace + set)
            DataSet users = DataSet.of("test", "users");
            
            // Step 4: Write a record
            System.out.println("2. Writing a user record...");
            session.upsert(users.id("alice123"))
                .bin("name").setTo("Alice Johnson")
                .bin("age").setTo(30)
                .bin("email").setTo("alice@example.com")
                .bin("city").setTo("San Francisco")
                .execute();
            
            System.out.println("   ✅ Record written!\n");
            
            // Step 5: Read the record back
            System.out.println("3. Reading the record back...");
            RecordStream result = session.query(users.id("alice123"))
                .execute();
            
            if (result.hasNext()) {
                KeyRecord record = result.next();
                System.out.println("   📄 Record found:");
                System.out.println("      Name: " + record.record.getString("name"));
                System.out.println("      Age: " + record.record.getInt("age"));
                System.out.println("      Email: " + record.record.getString("email"));
                System.out.println("      City: " + record.record.getString("city"));
            }
            System.out.println();
            
            // Step 6: Write a few more records
            System.out.println("4. Writing more records...");
            session.upsert(users.id("bob456"))
                .bin("name").setTo("Bob Smith")
                .bin("age").setTo(25)
                .bin("email").setTo("bob@example.com")
                .bin("city").setTo("New York")
                .execute();
            
            session.upsert(users.id("carol789"))
                .bin("name").setTo("Carol Davis")
                .bin("age").setTo(35)
                .bin("email").setTo("carol@example.com")
                .bin("city").setTo("Seattle")
                .execute();
            
            System.out.println("   ✅ Additional records written!\n");
            
            // Step 7: Query records
            System.out.println("5. Querying records where age > 28...");
            RecordStream queryResults = session.query(users)
                .where("$.age > 28")
                .execute();
            
            int count = 0;
            while (queryResults.hasNext()) {
                KeyRecord record = queryResults.next();
                count++;
                System.out.println("   📄 " + record.record.getString("name") + 
                                 " (age " + record.record.getInt("age") + ")");
            }
            System.out.println("   Found " + count + " records\n");
            
            System.out.println("🎉 Success! You've completed the Quick Start!");
            System.out.println("\n📚 Next steps:");
            System.out.println("   - Learn about DataSets: docs/concepts/datasets-and-keys.md");
            System.out.println("   - Explore CRUD operations: docs/guides/crud/");
            System.out.println("   - Try object mapping: docs/guides/object-mapping/");
            
        } catch (AerospikeException e) {
            System.err.println("❌ Aerospike Error: (" + e.getResultCode() + ") " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

---

## Step 4: Run the Program

### Using Maven

```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.QuickStart"
```

### Using Gradle

```bash
gradle run
```

### Expected Output

```
🚀 Starting Aerospike Fluent Client Quick Start...

1. Connecting to Aerospike...
   ✅ Connected!

2. Writing a user record...
   ✅ Record written!

3. Reading the record back...
   📄 Record found:
      Name: Alice Johnson
      Age: 30
      Email: alice@example.com
      City: San Francisco

4. Writing more records...
   ✅ Additional records written!

5. Querying records where age > 28...
   📄 Alice Johnson (age 30)
   📄 Carol Davis (age 35)
   Found 2 records

🎉 Success! You've completed the Quick Start!
```

---

## What Just Happened?

Let's break down the key concepts:

### 1. **Connection Management**

```java
try (Cluster cluster = new ClusterDefinition("localhost", 3000)
        .connect()) {
    // Use cluster...
}
```

- `ClusterDefinition` configures the connection
- `.connect()` establishes the connection
- `try-with-resources` ensures proper cleanup

**Learn more**: [Connection Management](../concepts/connection-management.md)

### 2. **Session Creation**

```java
Session session = cluster.createSession(Behavior.DEFAULT);
```

- `Session` is your main interface for operations
- `Behavior.DEFAULT` uses sensible default settings
- Sessions are lightweight and reusable

**Learn more**: [Sessions & Behavior](../concepts/sessions-and-behavior.md)

### 3. **DataSets & Keys**

```java
DataSet users = DataSet.of("test", "users");
```

- `DataSet` represents a namespace + set combination
- Provides methods to create keys: `.id("alice123")`
- Type-safe key generation

**Learn more**: [DataSets & Keys](../concepts/datasets-and-keys.md)

### 4. **Fluent Operations**

```java
session.upsert(users.id("alice123"))
    .bin("name").setTo("Alice")
    .bin("age").setTo(30)
    .execute();
```

- Chain methods for readability
- `upsert()` creates or updates
- `execute()` sends to database

**Learn more**: [Creating Records](../guides/crud/creating-records.md)

### 5. **Queries with DSL**

```java
session.query(users)
    .where("$.age > 28")
    .execute();
```

- Intuitive query syntax
- `$` references the record
- Type-safe expressions

**Learn more**: [Using the DSL](../guides/querying/using-dsl.md)

---

## Try It Yourself

Now that you have a working program, try these modifications:

### Challenge 1: Add More Fields

Add a `country` field to your user records:

```java
session.upsert(users.id("alice123"))
    .bin("name").setTo("Alice Johnson")
    .bin("age").setTo(30)
    .bin("email").setTo("alice@example.com")
    .bin("country").setTo("USA")  // ← New field
    .execute();
```

### Challenge 2: Complex Query

Find users in a specific city:

```java
RecordStream results = session.query(users)
    .where("$.city == 'San Francisco'")
    .execute();
```

### Challenge 3: Update a Record

Increment a user's age:

```java
session.update(users.id("alice123"))
    .bin("age").add(1)
    .execute();
```

### Challenge 4: Delete a Record

```java
session.delete(users.id("bob456"))
    .execute();
```

---

## Common Issues & Solutions

### ❌ "Cannot connect to Aerospike"

**Problem**: Connection refused on localhost:3000

**Solutions**:
1. Check if Aerospike is running: `docker ps | grep aerospike`
2. Start Aerospike: `docker start aerospike`
3. Check port mapping: Docker should expose port 3000

### ❌ "Namespace 'test' not found"

**Problem**: The namespace doesn't exist in your Aerospike configuration.

**Solution**: The default Docker image includes a `test` namespace. If using a custom setup, add it to `aerospike.conf`:

```
namespace test {
    replication-factor 1
    memory-size 1G
    storage-engine memory
}
```

### ❌ "Java version not supported"

**Problem**: You're using Java < 21.

**Solution**: Upgrade to Java 21 or higher. Download from [Adoptium](https://adoptium.net/).

### ❌ "Dependency not found"

**Problem**: Maven/Gradle can't find the Fluent Client.

**Solution**: 
1. Check your internet connection
2. Run `mvn clean install` or `gradle clean build`
3. If using a custom repository, ensure it's configured

---

## Next Steps

Congratulations! You've successfully:
- ✅ Connected to Aerospike
- ✅ Written records
- ✅ Read records
- ✅ Queried data

### Continue Your Journey

#### Beginner
- **[Core Concepts](../concepts/README.md)** - Understand the fundamentals
- **[CRUD Operations](../guides/crud/creating-records.md)** - Master basic operations
- **[Reading Records](../guides/crud/reading-records.md)** - Different ways to read data

#### Intermediate
- **[Object Mapping](../guides/object-mapping/creating-mappers.md)** - Work with POJOs
- **[Querying Data](../guides/querying/simple-queries.md)** - Advanced queries
- **[Configuration](../guides/configuration/behavior-java.md)** - Customize behavior

#### Advanced
- **[Transactions](../guides/advanced/transactions.md)** - Multi-record transactions
- **[Performance Tuning](../guides/performance/batch-operations.md)** - Optimize for speed
- **[API Reference](../api/README.md)** - Detailed documentation

### Get Help

- **Questions?** Check the [FAQ](../troubleshooting/faq.md)
- **Issues?** See [Troubleshooting](../troubleshooting/common-errors.md)
- **Contribute**: [Contributing Guide](../resources/contributing.md)

---

**Ready for more?** → [Installation & Setup Guide](./installation.md) for production configuration
