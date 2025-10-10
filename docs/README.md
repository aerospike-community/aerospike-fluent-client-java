# Aerospike Fluent Client for Java - Documentation

Welcome to the comprehensive documentation for the Aerospike Fluent Client for Java! This library provides a modern, type-safe, fluent API for working with Aerospike databases.

> **Developer Preview**: This is version 0.1.0, a developer preview not yet ready for production use. We encourage feedback through GitHub issues.

## 📚 Documentation Sections

### 🚀 [Getting Started](./getting-started/README.md)
New to the Fluent Client? Start here!

- **[Overview](./getting-started/overview.md)** - What is the Fluent Client and why use it?
- **[Quick Start](./getting-started/quickstart.md)** - Get up and running in 5 minutes
- **[Installation](./getting-started/installation.md)** - Detailed setup instructions

### 💡 [Core Concepts](./concepts/README.md)
Understand the fundamental building blocks.

- **[Connection Management](./concepts/connection-management.md)** - ClusterDefinition & Cluster
- **[Sessions & Behavior](./concepts/sessions-and-behavior.md)** - Configuring operation behavior
- **[DataSets & Keys](./concepts/datasets-and-keys.md)** - Organizing your data
- **[Type-Safe Operations](./concepts/type-safe-operations.md)** - Compile-time safety
- **[Object Mapping](./concepts/object-mapping.md)** - Working with POJOs

### 📖 [How-To Guides](./guides/README.md)
Step-by-step instructions for common tasks.

#### CRUD Operations
- **[Creating Records](./guides/crud/creating-records.md)** - Insert and upsert operations
- **[Reading Records](./guides/crud/reading-records.md)** - Get and batch read operations
- **[Updating Records](./guides/crud/updating-records.md)** - Update operations
- **[Deleting Records](./guides/crud/deleting-records.md)** - Delete operations

#### Querying Data
- **[Simple Queries](./guides/querying/simple-queries.md)** - Basic query operations
- **[Using the DSL](./guides/querying/using-dsl.md)** - Type-safe query expressions
- **[Filtering with WHERE](./guides/querying/filtering.md)** - Complex filtering
- **[Sorting & Pagination](./guides/querying/sorting-pagination.md)** - Managing result sets
- **[Partition Targeting](./guides/querying/partition-targeting.md)** - Performance optimization

#### Complex Data Types
- **[Working with Lists](./guides/cdt/lists.md)** - List operations
- **[Working with Maps](./guides/cdt/maps.md)** - Map operations
- **[Nested Operations](./guides/cdt/nested-operations.md)** - Complex CDT operations

#### Object Mapping
- **[Creating Mappers](./guides/object-mapping/creating-mappers.md)** - RecordMapper implementation
- **[Using TypeSafeDataSets](./guides/object-mapping/typesafe-datasets.md)** - Type-safe operations

#### Configuration
- **[Behavior Configuration (Java)](./guides/configuration/behavior-java.md)** - Programmatic configuration
- **[YAML Configuration](./guides/configuration/yaml-configuration.md)** - File-based configuration
- **[Duration Formats](./guides/configuration/duration-formats.md)** - Time value formats
- **[Dynamic Reloading](./guides/configuration/dynamic-reloading.md)** - Runtime configuration updates

#### Advanced Features
- **[Transactions](./guides/advanced/transactions.md)** - Multi-record transactions
- **[Info Commands](./guides/advanced/info-commands.md)** - Cluster information
- **[Index Monitoring](./guides/advanced/index-monitoring.md)** - Automatic index tracking
- **[Namespace Information](./guides/advanced/namespace-info.md)** - Real-time namespace stats

#### Performance Tuning
- **[Batch Operations](./guides/performance/batch-operations.md)** - High-throughput patterns
- **[Query Optimization](./guides/performance/query-optimization.md)** - Query performance
- **[Connection Pooling](./guides/performance/connection-pooling.md)** - Resource management
- **[Timeout Configuration](./guides/performance/timeout-configuration.md)** - Timeout tuning

#### Migration & Comparison
- **[Migrating from Traditional Client](./guides/migration/migrating-from-traditional.md)** - Migration guide
- **[API Comparison](./guides/migration/api-comparison.md)** - Side-by-side comparison

### 📘 [API Reference](./api/README.md)
Detailed API documentation for all classes and methods.

#### Connection & Session
- **[ClusterDefinition](./api/cluster-definition.md)**
- **[Cluster](./api/connection/cluster.md)**
- **[Session](./api/session.md)**
- **[TransactionalSession](./api/connection/transactional-session.md)**

#### Data Operations
- **[DataSet](./api/dataset.md)**
- **[TypeSafeDataSet](./api/operations/typesafe-dataset.md)**
- **[OperationBuilder](./api/operation-builder.md)**
- **[QueryBuilder](./api/query-builder.md)**
- **[RecordStream](./api/operations/record-stream.md)**

#### Object Mapping
- **[RecordMapper](./api/mapping/record-mapper.md)**
- **[RecordMappingFactory](./api/mapping/record-mapping-factory.md)**
- **[DefaultRecordMappingFactory](./api/mapping/default-record-mapping-factory.md)**

#### Configuration
- **[Behavior](./api/configuration/behavior.md)**
- **[BehaviorBuilder](./api/configuration/behavior-builder.md)**

#### DSL (Query Language)
- **[Dsl](./api/dsl/dsl.md)**
- **[Expressions](./api/dsl/expressions.md)**
- **[Type-Specific Bins](./api/dsl/bins.md)**

#### Info & Monitoring
- **[InfoCommands](./api/info/info-commands.md)**
- **[InfoParser](./api/info/info-parser.md)**
- **[IndexesMonitor](./api/info/indexes-monitor.md)**
- **[NamespaceInfo](./api/info/namespace-info.md)**

#### Exceptions
- **[Exception Hierarchy](./api/exceptions/exception-hierarchy.md)**

### 🔧 [Troubleshooting & FAQ](./troubleshooting/README.md)
Solutions to common problems.

- **[Common Errors](./troubleshooting/common-errors.md)** - Error reference with solutions
- **[Connection Issues](./troubleshooting/connection-issues.md)** - Connection troubleshooting
- **[Performance Issues](./troubleshooting/performance-issues.md)** - Performance debugging
- **[FAQ](./troubleshooting/faq.md)** - Frequently asked questions

### 💼 [Examples & Recipes](./examples/README.md)
Real-world examples and code snippets.

### 📋 [Additional Resources](./resources/README.md)
Release notes, compatibility, and more.

- **[Release Notes](./resources/README.md#release-notes)** - Changelog and version history
- **[Compatibility Matrix](./resources/README.md#compatibility-matrix)** - Version compatibility
- **[Migration Guides](./resources/README.md#migration-guides)** - Upgrade guides
- **[Glossary](./resources/README.md#glossary)** - Terms and definitions
- **[Contributing](./resources/README.md#contributing)** - How to contribute

---

## 🎯 Quick Links

**First time here?** → [Quick Start Guide](./getting-started/quickstart.md)

**Migrating from traditional client?** → [Migration Guide](./guides/migration/migrating-from-traditional.md)

**Looking for a specific method?** → [API Reference](./api/README.md)

**Having issues?** → [Troubleshooting](./troubleshooting/README.md)

---

## 💬 Need Help?

- **GitHub Issues**: [Report bugs or request features](https://github.com/aerospike/aerospike-fluent-client-java/issues)
- **Community Forum**: [Ask questions and share knowledge](https://discuss.aerospike.com)
- **Documentation Feedback**: Help us improve! Use the feedback widget on any page.

---

## 📄 License

This documentation and the Aerospike Fluent Client for Java are licensed under the Apache License 2.0.
See the [LICENSE](../LICENSE) file for details.
