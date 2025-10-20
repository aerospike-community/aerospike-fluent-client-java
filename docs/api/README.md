# API Reference

This section provides a detailed reference for the core classes and interfaces in the Aerospike Fluent Client for Java.

## Navigation

### Core Classes
- **[ClusterDefinition](./cluster-definition.md)**: The entry point for defining and configuring a connection to an Aerospike cluster.
- **[Cluster](./cluster.md)**: Represents an active connection to a cluster, responsible for managing resources and creating sessions.
- **[Session](./session.md)**: The primary interface for performing database operations.
- **[Behavior](./behavior.md)**: A collection of policies that control the client's operational behavior (timeouts, retries, etc.).

### Data and Query Builders
- **[DataSet](./dataset.md)**: A factory for creating keys for a specific namespace and set.
- **[TypeSafeDataSet](./typesafe-dataset.md)**: A type-safe version of `DataSet` for working with Java objects.
- **[OperationBuilder](./operation-builder.md)**: The fluent builder for constructing create, update, and delete operations.
- **[QueryBuilder](./query-builder.md)**: The fluent builder for constructing read, scan, and query operations.

### Results and Streaming
- **[RecordStream](./record-stream.md)**: A forward-only iterator for processing results from scans and queries.
- **[KeyRecord](./key-record.md)**: A container for a `Key` and its corresponding `Record` returned by a query.

### Object Mapping
- **[RecordMapper](./record-mapper.md)**: The interface for mapping Java objects to and from Aerospike records.
- **[RecordMappingFactory](./record-mapping-factory.md)**: A factory for providing `RecordMapper` instances to the client.

### DSL
- **[Dsl](./dsl.md)**: The entry point for the type-safe Domain Specific Language for building query filters.
- **[BooleanExpression](./boolean-expression.md)**: Represents a type-safe filter expression created by the DSL.
