# Additional Resources

Release notes, compatibility information, and additional resources for the Aerospike Fluent Client.

## Contents

- **[Release Notes](#release-notes)** - Version history and changes
- **[Compatibility Matrix](#compatibility-matrix)** - Version compatibility
- **[Migration Guides](#migration-guides)** - Upgrade instructions
- **[Glossary](#glossary)** - Terms and definitions
- **[Contributing](#contributing)** - How to contribute

---

## Release Notes

### Version 0.1.0 (Developer Preview) - Current

**Status**: Developer Preview (Not Production Ready)

**Release Date**: 2025

**New Features**:
- ✨ Fluent, chainable API for database operations
- ✨ Type-safe operations with compile-time checking
- ✨ DSL for intuitive query building
- ✨ Object mapping framework (RecordMapper)
- ✨ YAML-based configuration with dynamic reloading
- ✨ TypeSafeDataSet for POJO operations
- ✨ TransactionalSession with automatic retry
- ✨ InfoCommands for high-level cluster monitoring
- ✨ Automatic index discovery (IndexesMonitor)
- ✨ Comprehensive behavior configuration

**Dependencies**:
- aerospike-client-jdk8: 9.0.5
- aerospike-expression-dsl: 0.1.0
- jackson-databind: 2.16.1
- jackson-dataformat-yaml: 2.16.1

**Requirements**:
- Java 21 or higher
- Aerospike Server 5.0+

**Known Limitations**:
- Developer preview status - not recommended for production
- Some advanced traditional client features not yet wrapped
- API may change based on community feedback

**Breaking Changes**:
- N/A (Initial release)

---

## Compatibility Matrix

### Java Version Compatibility

| Java Version | Supported | Recommended |
|--------------|-----------|-------------|
| Java 21+ | ✅ Yes | ✅ Yes |
| Java 17 | ❌ No | - |
| Java 11 | ❌ No | - |
| Java 8 | ❌ No | - |

**Note**: Java 21 is required due to use of modern language features.

### Aerospike Server Compatibility

| Server Version | Supported | Notes |
|----------------|-----------|-------|
| 7.x | ✅ Yes | Fully supported |
| 6.x | ✅ Yes | Fully supported |
| 5.x | ✅ Yes | Minimum version |
| 4.x | ⚠️ Partial | Most features work, some limitations |
| < 4.0 | ❌ No | Not supported |

### Traditional Client Compatibility

Built on Aerospike Java Client 9.0.5

| Traditional Client Version | Compatible |
|----------------------------|------------|
| 9.x | ✅ Yes |
| 8.x | ✅ Yes |
| 7.x | ⚠️ Mostly |
| < 7.0 | ❌ No |

### Platform Compatibility

| Platform | Supported | Notes |
|----------|-----------|-------|
| Linux | ✅ Yes | All distributions |
| macOS | ✅ Yes | Intel & Apple Silicon |
| Windows | ✅ Yes | Via WSL2 recommended |
| Docker | ✅ Yes | Fully supported |
| Kubernetes | ✅ Yes | Fully supported |

### Build Tool Compatibility

| Tool | Minimum Version | Supported |
|------|----------------|-----------|
| Maven | 3.6.0 | ✅ Yes |
| Gradle | 7.0 | ✅ Yes |

---

## Migration Guides

### Migrating from Traditional Aerospike Client

See [Migration Guide](../guides/migration/migrating-from-traditional.md) for detailed instructions.

**Quick Summary**:

**Before (Traditional Client)**:
```java
AerospikeClient client = new AerospikeClient("localhost", 3000);
WritePolicy policy = new WritePolicy();
Key key = new Key("test", "users", "alice");
Bin bin = new Bin("name", "Alice");
client.put(policy, key, bin);
client.close();
```

**After (Fluent Client)**:
```java
try (Cluster cluster = new ClusterDefinition("localhost", 3000).connect()) {
    Session session = cluster.createSession(Behavior.DEFAULT);
    DataSet users = DataSet.of("test", "users");
    
    session.upsert(users.id("alice"))
        .bin("name").setTo("Alice")
        .execute();
}
```

### Upgrading Between Fluent Client Versions

**Future versions will include**:
- Detailed upgrade guides
- Breaking change documentation
- Automated migration tools
- Backward compatibility notes

---

## Glossary

### Aerospike Terms

**Namespace**
- Similar to a database in relational systems
- Top-level data container
- Configured on Aerospike server
- Example: `test`, `production`, `cache`

**Set**
- Similar to a table in relational systems
- Optional grouping within a namespace
- Example: `users`, `products`, `orders`

**Record**
- Individual data item
- Identified by a unique key
- Contains bins (name-value pairs)

**Bin**
- Name-value pair within a record
- Similar to a column, but schema-free
- Each record can have different bins

**Key**
- Unique identifier for a record
- Can be string, integer, or byte array
- Hashed to determine partition

**Generation**
- Version number of a record
- Increments on each write
- Used for optimistic locking

**TTL (Time To Live)**
- Expiration time for a record
- In seconds
- Record deleted when TTL expires

**Primary Index**
- Automatic index on keys
- All keys indexed by default
- Enables single-key lookups

**Secondary Index**
- Optional index on bin values
- Enables queries by bin value
- Must be created explicitly

**UDF (User-Defined Function)**
- Server-side code execution
- Written in Lua
- For complex operations

**XDR (Cross-Datacenter Replication)**
- Replication between clusters
- For disaster recovery and geo-distribution

### Fluent Client Terms

**Fluent API**
- Method chaining pattern
- Improves readability
- Example: `.bin("x").setTo(1).execute()`

**Builder**
- Pattern for constructing operations
- Type-safe API
- Examples: OperationBuilder, QueryBuilder

**Session**
- Main interface for database operations
- Created from Cluster
- Configured with Behavior

**Behavior**
- Configuration for operations
- Controls timeouts, retries, consistency
- Can be defined in Java or YAML

**DataSet**
- Represents namespace + set
- Provides key generation methods
- Immutable and reusable

**TypeSafeDataSet**
- DataSet bound to a Java class
- Enables object-oriented operations
- Used with RecordMapper

**RecordMapper**
- Interface for object ↔ record conversion
- Serialization/deserialization logic
- Custom per domain object

**DSL (Domain Specific Language)**
- Type-safe query expressions
- Alternative to string-based queries
- Compile-time checking

**RecordStream**
- Stream of query results
- Supports pagination
- Lazy evaluation

---

## Contributing

### How to Contribute

We welcome contributions! Here's how:

**1. Report Issues**
- Use [GitHub Issues](https://github.com/aerospike/aerospike-fluent-client-java/issues)
- Include minimal reproduction case
- Provide environment details

**2. Suggest Features**
- Open a feature request issue
- Describe use case and benefits
- Discuss design before implementation

**3. Submit Pull Requests**
- Fork the repository
- Create a feature branch
- Write tests for new code
- Follow existing code style
- Update documentation
- Submit PR with clear description

**4. Improve Documentation**
- Fix typos and errors
- Add examples
- Clarify explanations
- Improve organization

### Development Setup

```bash
# Clone repository
git clone https://github.com/aerospike/aerospike-fluent-client-java.git
cd aerospike-fluent-client-java

# Build
mvn clean install

# Run tests
mvn test

# Generate Javadoc
mvn javadoc:javadoc
```

### Code Style

- Follow Java naming conventions
- Use meaningful variable names
- Add Javadoc for public APIs
- Write unit tests for new features
- Keep methods focused and small

### Testing Guidelines

- Write tests for all new features
- Include positive and negative test cases
- Test error handling
- Use descriptive test names
- Maintain high code coverage

---

## Community Resources

### Official Links

- **GitHub**: [aerospike/aerospike-fluent-client-java](https://github.com/aerospike/aerospike-fluent-client-java)
- **Issues**: [GitHub Issues](https://github.com/aerospike/aerospike-fluent-client-java/issues)
- **Aerospike Website**: [aerospike.com](https://aerospike.com)
- **Documentation**: [docs.aerospike.com](https://docs.aerospike.com)

### Community

- **Discussion Forum**: [discuss.aerospike.com](https://discuss.aerospike.com)
- **Stack Overflow**: Tag `aerospike`
- **Twitter**: [@aerospikedb](https://twitter.com/aerospikedb)

### Learning Resources

- **Aerospike University**: Free online courses
- **Blog**: [aerospike.com/blog](https://aerospike.com/blog)
- **Webinars**: Regular technical webinars
- **YouTube**: [Aerospike Channel](https://youtube.com/aerospikedb)

### Getting Help

1. **Check documentation** (you're here!)
2. **Search existing issues** on GitHub
3. **Ask on discussion forum** for general questions
4. **Open GitHub issue** for bugs
5. **Contact support** for enterprise customers

---

## License

This project is licensed under the Apache License 2.0.

See [LICENSE](../../LICENSE) file for full text.

---

## Acknowledgments

Built on the solid foundation of the Aerospike Java Client.

Special thanks to:
- Aerospike engineering team
- Community contributors
- Early adopters providing feedback

---

**Questions?** Check the [FAQ](../troubleshooting/faq.md) or [open an issue](https://github.com/aerospike/aerospike-fluent-client-java/issues)
