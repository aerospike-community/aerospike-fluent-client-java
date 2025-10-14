# How-To Guides

Step-by-step instructions for common tasks with the Aerospike Fluent Client.

## 📝 CRUD Operations

Master the basics of creating, reading, updating, and deleting records.

- **[Creating Records](./crud/creating-records.md)** - Insert and upsert operations
- **[Reading Records](./crud/reading-records.md)** - Get, batch read, and scan operations
- **[Updating Records](./crud/updating-records.md)** - Update operations and atomic modifications
- **[Deleting Records](./crud/deleting-records.md)** - Delete operations

## 🔍 Querying Data

Learn how to query and filter your data effectively.

- **[Simple Queries](./querying/simple-queries.md)** - Basic query operations
- **[Using the DSL](./querying/using-dsl.md)** - Type-safe query expressions
- **[Filtering with WHERE](./querying/filtering.md)** - Complex filtering patterns
- **[Sorting & Pagination](./querying/sorting-pagination.md)** - Managing large result sets
- **[Partition Targeting](./querying/partition-targeting.md)** - Performance optimization

## 📦 Complex Data Types (CDT)

Work with lists, maps, and nested structures.

- **[Working with Lists](./cdt/lists.md)** - List operations and manipulation
- **[Working with Maps](./cdt/maps.md)** - Map operations and manipulation
- **[Nested Operations](./cdt/nested-operations.md)** - Complex nested structures

## 🎯 Object Mapping

Map Java objects to Aerospike records.

- **[Creating Mappers](./object-mapping/creating-mappers.md)** - Implement RecordMapper
- **[Using TypeSafeDataSets](./object-mapping/typesafe-datasets.md)** - Type-safe CRUD operations
- **[Custom Serialization](./object-mapping/custom-serialization.md)** - Advanced mapping patterns

## ⚙️ Configuration

Configure behavior and operational settings.

- **[Behavior Configuration (Java)](./configuration/behavior-java.md)** - Programmatic configuration
- **[YAML Configuration](./configuration/yaml-configuration.md)** - File-based configuration
- **[Duration Formats](./configuration/duration-formats.md)** - Time value formats
- **[Dynamic Reloading](./configuration/dynamic-reloading.md)** - Runtime configuration updates

## 🚀 Advanced Features

Leverage advanced capabilities of the Fluent Client.

- **[Transactions](./advanced/transactions.md)** - Multi-record transactions with TransactionalSession
- **[Info Commands](./advanced/info-commands.md)** - Cluster monitoring and information
- **[Index Monitoring](./advanced/index-monitoring.md)** - Automatic index discovery
- **[Namespace Information](./advanced/namespace-info.md)** - Real-time namespace statistics

## ⚡ Performance Tuning

Optimize your application for speed and efficiency.

- **[Batch Operations](./performance/batch-operations.md)** - High-throughput patterns
- **[Query Optimization](./performance/query-optimization.md)** - Query performance tips
- **[Connection Pooling](./performance/connection-pooling.md)** - Resource management
- **[Timeout Configuration](./performance/timeout-configuration.md)** - Fine-tuning timeouts

## 🔄 Migration & Comparison

Moving from the traditional client or comparing approaches.

- **[Migrating from Traditional Client](./migration/migrating-from-traditional.md)** - Step-by-step migration guide
- **[API Comparison](./migration/api-comparison.md)** - Side-by-side code comparisons

---

## Quick Access by Task

### I want to...

**Store data**
→ [Creating Records](./crud/creating-records.md)

**Retrieve data**
→ [Reading Records](./crud/reading-records.md)

**Search/filter records**
→ [Simple Queries](./querying/simple-queries.md) or [Filtering](./querying/filtering.md)

**Work with objects**
→ [Object Mapping](./object-mapping/creating-mappers.md)

**Handle lists/maps**
→ [Lists](./cdt/lists.md) or [Maps](./cdt/maps.md)

**Improve performance**
→ [Batch Operations](./performance/batch-operations.md)

**Configure timeouts**
→ [Behavior Configuration](./configuration/behavior-java.md)

**Use transactions**
→ [Transactions](./advanced/transactions.md)

**Monitor cluster**
→ [Info Commands](./advanced/info-commands.md)

---

## Guide Format

Each guide follows a consistent structure:

1. **Goal** - What you'll learn
2. **Prerequisites** - What you need to know
3. **Step-by-step instructions** - Clear, numbered steps
4. **Complete working example** - Copy-paste ready code
5. **Variations** - Alternative approaches
6. **Common pitfalls** - What to avoid
7. **Best practices** - Recommended patterns
8. **Next steps** - Related guides

---

## Need Help?

- **Can't find what you're looking for?** Check the [API Reference](../api/README.md)
- **Having issues?** See [Troubleshooting](../troubleshooting/README.md)
- **Questions?** Check the [FAQ](../troubleshooting/faq.md)
- **Want to contribute?** See [Contributing Guide](../resources/contributing.md)
