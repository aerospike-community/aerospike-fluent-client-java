# FAQ (Frequently Asked Questions)

This page answers common questions about the Aerospike Fluent Client for Java.

## General

**Q: Is the Fluent Client ready for production?**

A: Not yet. The current version is a developer preview. It is not recommended for production use until version 1.0.

**Q: What is the relationship between the Fluent Client and the traditional Aerospike Java client?**

A: The Fluent Client is a wrapper around the traditional Java client. It uses the same underlying engine for communication with the Aerospike cluster, but provides a more modern, readable, and type-safe API.

**Q: Do I need to include the traditional client as a dependency?**

A: No. The Fluent Client includes the traditional client as a transitive dependency.

## Usage

**Q: Is the `Cluster` object thread-safe?**

A: Yes. The `Cluster` object is thread-safe and is intended to be created once and shared throughout your application.

**Q: Are `Session` objects thread-safe?**

A: No. `Session` objects are not thread-safe and should not be shared across threads. Each thread should create its own `Session` from the shared `Cluster` object.

**Q: Why do all queries return a `RecordStream`?**

A: This provides a consistent API for all read operations. Whether you are reading a single key, multiple keys, or querying a whole set, you get back a `RecordStream` that you can iterate over. This simplifies the API and encourages a more functional style of programming.

**Q: How do I configure client policies (timeouts, retries, etc.)?**

A: Policies are configured using `Behavior` objects. You can create `Behavior` objects programmatically or load them from a YAML file. See the [Sessions & Behavior](../../concepts/sessions-and-behavior.md) guide for more details.

---

## Next Steps

- **[Quick Start](../../getting-started/quickstart.md)**: Get started with the Fluent Client.
- **[Core Concepts](../../concepts/README.md)**: Learn the fundamentals of the new API.
