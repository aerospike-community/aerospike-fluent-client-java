# Documentation Guide

Welcome to the complete documentation for the Aerospike Fluent Client for Java!

## Documentation Structure

This documentation is organized into the following sections:

### 📘 [Getting Started](./getting-started/README.md)
**Time**: 30 minutes | **Level**: Beginner

Start here if you're new to the Fluent Client.

- **[Overview](./getting-started/overview.md)** - What is the Fluent Client and why use it?
- **[Quick Start](./getting-started/quickstart.md)** - Get running in 10 minutes
- **[Installation](./getting-started/installation.md)** - Detailed setup instructions

### 💡 [Core Concepts](./concepts/README.md)
**Time**: 1 hour | **Level**: Beginner to Intermediate

Understand the fundamental building blocks.

- **[Connection Management](./concepts/connection-management.md)** - ClusterDefinition & Cluster
- **[Sessions & Behavior](./concepts/sessions-and-behavior.md)** - Configuring operations
- **[DataSets & Keys](./concepts/datasets-and-keys.md)** - Organizing your data
- **[Type-Safe Operations](./concepts/type-safe-operations.md)** - Fluent, compile-time safe API
- **[Object Mapping](./concepts/object-mapping.md)** - Working with POJOs

### 📖 [How-To Guides](./guides/README.md)
**Time**: Variable | **Level**: All Levels

Step-by-step instructions for specific tasks.

**CRUD Operations**:
- [Creating Records](./guides/crud/creating-records.md)
- [Reading Records](./guides/crud/reading-records.md)
- [Updating Records](./guides/crud/updating-records.md)
- [Deleting Records](./guides/crud/deleting-records.md)

**Querying**: Simple queries, DSL, filtering, pagination, partition targeting

**Complex Data Types**: Lists, maps, nested operations

**Object Mapping**: Creating mappers, TypeSafeDataSets, custom serialization

**Configuration**: Java and YAML configuration, duration formats, dynamic reloading

**Advanced**: Transactions, info commands, index monitoring, namespace info

**Performance**: Batch operations, query optimization, connection pooling, timeouts

**Migration**: From traditional client, API comparison

### 📘 [API Reference](./api/README.md)
**Time**: Reference | **Level**: All Levels

Complete API documentation for all classes and methods.

Organized by functional area:
- Connection & Session
- Data Operations
- Object Mapping
- Configuration
- DSL (Query Language)
- Info & Monitoring
- Exceptions

### 🔧 [Troubleshooting & FAQ](./troubleshooting/README.md)
**Time**: As Needed | **Level**: All Levels

Solutions to common problems and frequently asked questions.

- Common error messages and fixes
- Connection issue debugging
- Performance troubleshooting
- Configuration help
- Comprehensive FAQ

### 💼 [Examples & Recipes](./examples/README.md)
**Time**: Variable | **Level**: Intermediate

Real-world examples and reusable code snippets.

- Complete application examples (e-commerce, sessions, analytics)
- Code snippet library (counters, locks, time-series, caching)
- Design patterns (repository, unit of work, retry)
- Testing examples

### 📋 [Additional Resources](./resources/README.md)
**Time**: Reference | **Level**: All Levels

Release notes, compatibility, and more.

- Release notes and changelog
- Compatibility matrix
- Migration guides
- Glossary of terms
- Contributing guidelines

---

## Learning Paths

### Path 1: Quick Start (30 minutes)

Perfect if you want to get something working immediately.

1. [Quick Start Guide](./getting-started/quickstart.md) (10 min)
2. [Creating Records](./guides/crud/creating-records.md) (10 min)
3. [Reading Records](./guides/crud/reading-records.md) (10 min)

### Path 2: Comprehensive (3 hours)

For a thorough understanding of the Fluent Client.

1. [Overview](./getting-started/overview.md) (5 min)
2. [Installation](./getting-started/installation.md) (15 min)
3. [Quick Start](./getting-started/quickstart.md) (10 min)
4. All [Core Concepts](./concepts/README.md) (1 hour)
5. [CRUD Guides](./guides/crud/README.md) (45 min)
6. [Querying Data](./guides/querying/simple-queries.md) (30 min)
7. [Object Mapping](./guides/object-mapping/creating-mappers.md) (15 min)

### Path 3: Practical Projects (Variable)

Learn by building.

1. [Quick Start](./getting-started/quickstart.md) (10 min)
2. [Core Concepts Overview](./concepts/README.md) (15 min)
3. Choose an [Example Project](./examples/README.md) (1-2 hours)
4. Reference [API docs](./api/README.md) as needed

### Path 4: Migration (1 hour)

Coming from the traditional Aerospike client?

1. [Overview - Traditional vs Fluent](./getting-started/overview.md#traditional-vs-fluent) (10 min)
2. [Migration Guide](./guides/migration/migrating-from-traditional.md) (30 min)
3. [API Comparison](./guides/migration/api-comparison.md) (20 min)

---

## Quick Access by Task

**I want to...**

→ **Get started quickly**: [Quick Start](./getting-started/quickstart.md)

→ **Understand the basics**: [Core Concepts](./concepts/README.md)

→ **Store data**: [Creating Records](./guides/crud/creating-records.md)

→ **Retrieve data**: [Reading Records](./guides/crud/reading-records.md)

→ **Query/filter data**: [Simple Queries](./guides/querying/simple-queries.md)

→ **Work with objects**: [Object Mapping](./guides/object-mapping/creating-mappers.md)

→ **Configure behavior**: [Behavior Configuration](./guides/configuration/behavior-java.md)

→ **Use transactions**: [Transactions Guide](./guides/advanced/transactions.md)

→ **Optimize performance**: [Batch Operations](./guides/performance/batch-operations.md)

→ **Migrate from traditional client**: [Migration Guide](./guides/migration/migrating-from-traditional.md)

→ **Look up a method**: [API Reference](./api/README.md)

→ **Solve a problem**: [Troubleshooting](./troubleshooting/README.md)

→ **See examples**: [Examples & Recipes](./examples/README.md)

---

## Documentation Features

### ✨ What Makes This Documentation Special

**1. Beginner-Friendly**
- Clear explanations without jargon
- Step-by-step instructions
- Complete, runnable examples
- Learning paths for different goals

**2. Comprehensive**
- Covers all features
- Multiple difficulty levels
- Both conceptual and practical content
- Cross-referenced throughout

**3. Practical**
- Real-world examples
- Copy-paste ready code
- Common pitfalls highlighted
- Best practices included

**4. Well-Organized**
- Logical structure
- Easy navigation
- Quick reference tables
- Search-friendly

**5. Production-Ready**
- Error handling patterns
- Performance considerations
- Testing examples
- Troubleshooting guides

---

## Using This Documentation

### Navigation Tips

1. **Use the README files** - Each section has a README that provides an overview
2. **Follow cross-links** - Documents link to related content
3. **Check the glossary** - Unfamiliar terms? See [Glossary](./resources/README.md#glossary)
4. **Use search** - If your documentation system supports it

### Getting Help

1. **Search the docs** - Use your browser's find function (Cmd/Ctrl+F)
2. **Check FAQ** - [Frequently Asked Questions](./troubleshooting/README.md#frequently-asked-questions)
3. **Review examples** - [Examples section](./examples/README.md) has practical code
4. **Open an issue** - [GitHub Issues](https://github.com/aerospike/aerospike-fluent-client-java/issues)

### Contributing to Documentation

Found an error? Want to improve something?

1. **Small fixes**: Edit directly and submit PR
2. **Larger changes**: Open an issue first to discuss
3. **New examples**: Always welcome!
4. **See**: [Contributing Guide](./resources/README.md#contributing)

---

## Documentation Conventions

### Code Examples

All code examples follow these conventions:

```java
// Complete, runnable examples include necessary imports
import com.aerospike.*;

// Use meaningful variable names
DataSet users = DataSet.of("test", "users");
Session session = cluster.createSession(Behavior.DEFAULT);

// Show both success and error cases
try {
    session.insert(key).bin("name").setTo("Alice").execute();
} catch (AerospikeException.RecordExists e) {
    // Handle error
}
```

### Placeholders

When you see these, replace with your values:

- `<YOUR_VALUE>` - Replace with your specific value
- `localhost` - Your Aerospike host
- `test` - Your namespace
- `users` - Your set name

### Status Icons

- ✅ Supported / Recommended
- ⚠️ Partially supported / Use with caution
- ❌ Not supported / Don't use
- 🚀 New feature
- 🔒 Requires authentication/authorization

---

## Version Information

**Current Documentation Version**: 0.1.0 (Developer Preview)

**Last Updated**: 2025

**Fluent Client Version**: 0.1.0

**Status**: Developer Preview - Not yet production-ready

---

## Feedback

We want to make this documentation as helpful as possible!

**How to provide feedback**:

1. **Found an error?** Open a [documentation issue](https://github.com/aerospike/aerospike-fluent-client-java/issues)
2. **Have a suggestion?** Open a feature request
3. **Want to contribute?** See [Contributing Guide](./resources/README.md#contributing)

**Common feedback we appreciate**:

- Confusing explanations
- Missing examples
- Outdated information
- Broken links
- Typos and errors
- Missing topics

---

## What's Next?

Ready to start? Choose your path:

### New to Aerospike Fluent Client?
→ Start with [Quick Start Guide](./getting-started/quickstart.md)

### Coming from traditional client?
→ See [Migration Guide](./guides/migration/migrating-from-traditional.md)

### Want deep understanding?
→ Read [Core Concepts](./concepts/README.md)

### Learn by doing?
→ Explore [Examples](./examples/README.md)

### Need specific information?
→ Browse [API Reference](./api/README.md)

---

**Happy coding with Aerospike Fluent Client!** 🚀
