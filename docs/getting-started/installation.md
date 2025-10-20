# Installation & Setup

Complete installation instructions for the Aerospike Fluent Client for Java.

## Prerequisites

### Required

- **Java Development Kit (JDK) 21 or higher**
- **Maven 3.6+** or **Gradle 7.0+**
- **Aerospike Server 5.0+**

### Recommended

- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Docker**: For local development and testing

---

## Quick Installation

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.aerospike</groupId>
    <artifactId>aerospike-fluent-client-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```groovy
dependencies {
    implementation 'com.aerospike:aerospike-fluent-client-java:0.1.0'
}
```

### Gradle (Kotlin DSL)

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.aerospike:aerospike-fluent-client-java:0.1.0")
}
```

---

## Detailed Setup

### Step 1: Install Java 21+

#### Verify Current Version

```bash
java -version
```

You should see:
```
openjdk version "21.0.1" 2023-10-17
OpenJDK Runtime Environment (build 21.0.1+12-29)
OpenJDK 64-Bit Server VM (build 21.0.1+12-29, mixed mode, sharing)
```

#### Install Java 21

**Option 1: Using SDKMAN (Recommended for Unix/macOS)**

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash

# Install Java 21
sdk install java 21-tem

# Set as default
sdk default java 21-tem
```

**Option 2: Using Package Manager**

**macOS (Homebrew)**:
```bash
brew install openjdk@21
```

**Ubuntu/Debian**:
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

**Windows**:
Download from [Adoptium](https://adoptium.net/temurin/releases/?version=21) and run the installer.

#### Set JAVA_HOME

**macOS/Linux**:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  # Linux
```

Add to `~/.bashrc` or `~/.zshrc` to make permanent.

**Windows**:
1. Right-click "This PC" → Properties → Advanced system settings
2. Click "Environment Variables"
3. Add `JAVA_HOME` pointing to your JDK installation (e.g., `C:\Program Files\Java\jdk-21`)

### Step 2: Install Build Tool

#### Maven

**macOS**:
```bash
brew install maven
```

**Ubuntu/Debian**:
```bash
sudo apt install maven
```

**Windows**:
Download from [Maven website](https://maven.apache.org/download.cgi) and follow installation instructions.

Verify:
```bash
mvn -version
# Should show Maven 3.6.0 or higher
```

#### Gradle

**macOS**:
```bash
brew install gradle
```

**Ubuntu/Debian**:
```bash
sudo apt install gradle
```

**Windows**:
Download from [Gradle website](https://gradle.org/install/) and follow installation instructions.

Verify:
```bash
gradle -version
# Should show Gradle 7.0 or higher
```

### Step 3: Install Aerospike Server

#### Option 1: Docker (Recommended for Development)

**Start Aerospike**:
```bash
docker run -d \
  --name aerospike \
  -p 3000:3000 \
  -p 3001:3001 \
  -p 3002:3002 \
  -p 3003:3003 \
  aerospike/aerospike-server
```

**Verify**:
```bash
docker logs aerospike | grep "service ready"
```

**Stop**:
```bash
docker stop aerospike
```

**Restart**:
```bash
docker start aerospike
```

**Remove**:
```bash
docker rm -f aerospike
```

#### Option 2: Native Installation

**macOS**:
```bash
brew install aerospike-server
aerospike start
```

**Ubuntu/Debian**:
```bash
wget -O aerospike.tgz https://download.aerospike.com/artifacts/aerospike-server-community/7.0.0.1/aerospike-server-community_7.0.0.1_tools-9.4.0_ubuntu22.04_x86_64.tgz
tar -xvf aerospike.tgz
cd aerospike-server-community_*
sudo ./asinstall
sudo systemctl start aerospike
```

**Windows**:
Use Docker Desktop with the Docker instructions above.

**Verify Installation**:
```bash
# Using aql (Aerospike Query Language tool)
aql
aql> show namespaces
aql> exit
```

#### Option 3: Aerospike Cloud

Sign up for free at [Aerospike Cloud](https://aerospike.com/cloud/) and get connection details.

---

## Project Setup

### Create a New Maven Project

```bash
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=my-aerospike-app \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.4 \
  -DinteractiveMode=false

cd my-aerospike-app
```

Edit `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>my-aerospike-app</artifactId>
    <version>1.0-SNAPSHOT</version>
    
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <dependencies>
        <!-- Aerospike Fluent Client -->
        <dependency>
            <groupId>com.aerospike</groupId>
            <artifactId>aerospike-fluent-client-java</artifactId>
            <version>0.1.0</version>
        </dependency>
        
        <!-- Optional: Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>2.0.9</version>
        </dependency>
        
        <!-- Optional: Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Create a New Gradle Project

```bash
mkdir my-aerospike-app
cd my-aerospike-app
gradle init --type java-application --dsl groovy --test-framework junit-jupiter
```

Edit `build.gradle`:

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
    // Aerospike Fluent Client
    implementation 'com.aerospike:aerospike-fluent-client-java:0.1.0'
    
    // Optional: Logging
    implementation 'org.slf4j:slf4j-simple:2.0.9'
    
    // Testing
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
}

application {
    mainClass = 'com.example.App'
}

test {
    useJUnitPlatform()
}
```

---

## IDE Configuration

### IntelliJ IDEA

1. **Import Project**:
   - File → Open → Select your `pom.xml` or `build.gradle`
   - IntelliJ will auto-detect the project type

2. **Set Java SDK**:
   - File → Project Structure → Project
   - Set Project SDK to Java 21
   - Set Language level to "21 - Pattern matching for switch"

3. **Enable Annotation Processing** (for future features):
   - File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"

4. **Recommended Plugins**:
   - Aerospike Explorer (if available)
   - Maven Helper (for Maven projects)
   - Gradle (for Gradle projects)

### Eclipse

1. **Import Project**:
   - File → Import → Existing Maven Project (or Gradle)
   - Select your project directory

2. **Set Java Compiler**:
   - Right-click project → Properties → Java Compiler
   - Set Compiler compliance level to 21

3. **Install Buildship** (for Gradle):
   - Help → Eclipse Marketplace
   - Search "Buildship Gradle Integration"

### VS Code

1. **Install Extensions**:
   - Java Extension Pack
   - Maven for Java (or Gradle for Java)

2. **Open Project**:
   - File → Open Folder → Select project directory

3. **Configure Java**:
   - Cmd/Ctrl + Shift + P → "Java: Configure Java Runtime"
   - Set Java 21 as the runtime

---

## Verification

### Test Your Installation

Create `src/main/java/com/example/VerifyInstall.java`:

```java
package com.example;

import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.Session;
import com.aerospike.DataSet;
import com.aerospike.policy.Behavior;

public class VerifyInstall {
    public static void main(String[] args) {
        System.out.println("Testing Aerospike Fluent Client installation...\n");
        
        try (Cluster cluster = new ClusterDefinition("localhost", 3000)
                .connect()) {
            
            System.out.println("✅ Connection successful!");
            
            Session session = cluster.createSession(Behavior.DEFAULT);
            DataSet test = DataSet.of("test", "verification");
            
            // Write a test record
            session.upsert(test.id("test-key"))
                .bin("timestamp").setTo(System.currentTimeMillis())
                .execute();
            
            System.out.println("✅ Write operation successful!");
            
            // Read it back
            var result = session.query(test.id("test-key")).execute();
            if (result.hasNext()) {
                System.out.println("✅ Read operation successful!");
            }
            
            // Clean up
            session.delete(test.id("test-key")).execute();
            System.out.println("✅ Delete operation successful!");
            
            System.out.println("\n🎉 Installation verified successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            System.err.println("\nTroubleshooting:");
            System.err.println("1. Ensure Aerospike server is running");
            System.err.println("2. Check connection details (host:port)");
            System.err.println("3. Verify firewall settings");
            e.printStackTrace();
        }
    }
}
```

Run it:

**Maven**:
```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.VerifyInstall"
```

**Gradle**:
```bash
gradle run --args="com.example.VerifyInstall"
```

Expected output:
```
Testing Aerospike Fluent Client installation...

✅ Connection successful!
✅ Write operation successful!
✅ Read operation successful!
✅ Delete operation successful!

🎉 Installation verified successfully!
```

---

## Configuration Files

### Logging Configuration

Create `src/main/resources/simplelogger.properties`:

```properties
# Set root logger level
org.slf4j.simpleLogger.defaultLogLevel=info

# Aerospike client logs
org.slf4j.simpleLogger.log.com.aerospike=info

# Your application logs
org.slf4j.simpleLogger.log.com.example=debug

# Show date/time
org.slf4j.simpleLogger.showDateTime=true
org.slf4j.simpleLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss
```

### Application Configuration

Create `src/main/resources/application.properties`:

```properties
# Aerospike connection
aerospike.host=localhost
aerospike.port=3000
aerospike.namespace=test

# Optional: Authentication
# aerospike.username=admin
# aerospike.password=admin123

# Optional: Timeout settings
aerospike.timeout.connect=1000
aerospike.timeout.read=5000
aerospike.timeout.write=5000
```

---

## Troubleshooting

### Java Version Issues

**Problem**: `Unsupported class file major version 65`

**Solution**: This means your code was compiled with Java 21 but you're running with an older version.

```bash
# Check Java version
java -version

# Update JAVA_HOME
export JAVA_HOME=/path/to/jdk-21
```

### Dependency Resolution

**Problem**: `Could not resolve dependency: com.aerospike:aerospike-fluent-client-java:0.1.0`

**Solution**:
1. Check internet connection
2. Clear local cache:
   ```bash
   # Maven
   mvn dependency:purge-local-repository
   
   # Gradle
   gradle clean build --refresh-dependencies
   ```

### Connection Issues

**Problem**: `Connection refused: localhost/127.0.0.1:3000`

**Solutions**:
1. Verify Aerospike is running:
   ```bash
   docker ps | grep aerospike
   # or
   sudo systemctl status aerospike
   ```

2. Check port availability:
   ```bash
   netstat -an | grep 3000
   lsof -i :3000
   ```

3. Test with `telnet`:
   ```bash
   telnet localhost 3000
   ```

### Docker on macOS/Windows

**Problem**: Docker connectivity issues

**Solution**:
- Use `host.docker.internal` instead of `localhost`
- Or use Docker's bridge network IP

```java
new ClusterDefinition("host.docker.internal", 3000)
```

---

## Docker Compose Setup

For development with multiple services, create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  aerospike:
    image: aerospike/aerospike-server
    ports:
      - "3000:3000"
      - "3001:3001"
      - "3002:3002"
      - "3003:3003"
    environment:
      - NAMESPACE=test
    volumes:
      - aerospike-data:/opt/aerospike/data

  # Optional: AMS (Aerospike Management System)
  ams:
    image: aerospike/aerospike-ams
    ports:
      - "8081:8081"
    depends_on:
      - aerospike

volumes:
  aerospike-data:
```

Start:
```bash
docker-compose up -d
```

Stop:
```bash
docker-compose down
```

---

## Production Considerations

### TLS/SSL Configuration

For production deployments with TLS:

```java
Cluster cluster = new ClusterDefinition("production.example.com", 4333)
    .withNativeCredentials("username", "password")
    .withTls(tls -> tls
        .enabledProtocols("TLSv1.3")
        .trustStorePath("/path/to/truststore.jks")
        .trustStorePassword("truststorepass")
        .keyStorePath("/path/to/keystore.jks")
        .keyStorePassword("keystorepass")
    )
    .connect();
```

See [Configuration Guide](../guides/configuration/behavior-java.md) for details.

### Cluster Configuration

For multi-node production clusters:

```java
Cluster cluster = new ClusterDefinition(
        new Host("node1.example.com", 3000),
        new Host("node2.example.com", 3000),
        new Host("node3.example.com", 3000)
    )
    .withNativeCredentials("app_user", "secure_password")
    .validateClusterNameIs("production-cluster")
    .preferredRacks(1, 2)
    .connect();
```

---

## Next Steps

Now that you have everything installed:

1. **Complete the [Quick Start](./quickstart.md)** - Build your first application
2. **Learn [Core Concepts](../concepts/README.md)** - Understand the architecture
3. **Explore [CRUD Operations](../guides/crud/creating-records.md)** - Master basic operations
4. **Review [API Reference](../api/README.md)** - Detailed documentation

---

## Additional Resources

- **[Compatibility Matrix](../resources/compatibility-matrix.md)** - Version compatibility details
- **[Troubleshooting Guide](../troubleshooting/README.md)** - Common issues and solutions
- **[FAQ](../troubleshooting/faq.md)** - Frequently asked questions
- **[Aerospike Documentation](https://aerospike.com/docs/)** - Official Aerospike docs

**Need help?** [Open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues)
