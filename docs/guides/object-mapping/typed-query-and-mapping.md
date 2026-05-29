# Typed query streams, mapping context, and batch reads

This guide is the **authoritative overview** for typed object mapping in the fluent client: how typed datasets and keys relate to streams, how the factory supplies mappers, and how to read **multiple entity types** in one Aerospike batch without abusing generics.

For step-by-step CRUD examples, see **[Using TypedDataSets](./using-typesafe-datasets.md)**.

## Goals

- Use a single compile-time type `T` for a dataset or key list.
- Deserialize with **`RecordMappingFactory`** (no per-call mapper) on **`TypedRecordStream<T>`** and **`TypedNavigatableRecordStream<T>`**.
- Give mappers access to **`Session`** and entity **`Class<T>`** via **`RecordReadContext<T>`** for dependent loads.
- Keep **one type per stream**; heterogeneous reads use **`mixedRead()`**, not `TypedRecordStream<?>`.

## Migration: `TypeSafeDataSet` → `TypedDataSet`

The type was renamed to match “typed” wording used elsewhere (`TypedKey`, `TypedRecordStream`). Replace:

```java
// before
TypeSafeDataSet<Customer> ds = TypeSafeDataSet.of("ns", "set", Customer.class);

// after
TypedDataSet<Customer> ds = TypedDataSet.of("ns", "set", Customer.class);
```

## API matrix

| Entry | Query builder | Stream type |
|--------|----------------|-------------|
| `DataSet`, `Key`, `List<Key>` | `QueryBuilder` | `RecordStream` |
| `TypedDataSet<T>`, `TypedKey<T>`, varargs `TypedKey<T>…` | `TypedQueryBuilder<T>` | `TypedRecordStream<T>` |
| `List<TypedKey<T>>` (same `T`) | `session.queryTypedKeys(List<TypedKey<T>>)` | `TypedQueryBuilder<T>` → `TypedRecordStream<T>` |

**Why `queryTypedKeys`?** Java cannot overload `query(List<Key>)` and `query(List<TypedKey<T>>)` safely because of erasure; the fluent client uses a dedicated name for typed key lists.

### CUD and batch chains

- Single typed key: `insert(TypedKey<T>)`, `delete(TypedKey<T>)`, etc. (delegates to `getNativeKey()`).
- List of typed keys: `insertKeys`, `updateKeys`, `deleteKeys`, `existsKeys`, `queryTypedKeys`, … on **`Session`** and chain builders (`ChainableOperationBuilder`, `ChainableNoBinsBuilder`, `ChainableQueryBuilder`).

Use **`Session.nativeKeysFromTyped(List<? extends TypedKey<?>>)`** when you need a `List<Key>` for a low-level API.

## `RecordReadContext` and `RecordMapper`

When the client maps bins to objects on a typed path, it calls:

```text
mapper.fromMap(bins, key, generation, ctx)
```

with **`RecordReadContext<T>`** containing:

- **`getSession()`** — for transactional or follow-up reads.
- **`getEntityType()`** — the declared `Class<T>`.
- **`getRecordMappingFactory()`** — same factory as `session.getRecordMappingFactory()`.

Implementations that do not need context can ignore the default **`RecordMapper.fromMap(..., ctx)`**, which delegates to the three-argument `fromMap`.

## Missing mapper

If no mapper is registered for `T`, **`MappingSupport.requireMapper`** throws **`IllegalStateException`** with the class name in the message. This applies to **`toObjectList()`**, **`forEachObject`**, **`streamObjects`**, **`getFirstObject*`**, and **`MixedTypedBatchReadResult#toObjects`**.

## Navigable typed streams

From **`TypedRecordStream<T>`**, call **`asNavigatableStream()`** or **`asNavigatableStream(long limit)`** to obtain **`TypedNavigatableRecordStream<T>`**, which composes **`NavigatableRecordStream`** (sort, paging, reset) and exposes the same factory-based **`toObjectList()`** / **`forEachObject`** / **`streamObjects`** without passing a mapper each time.

Close ownership follows the underlying navigable stream; prefer try-with-resources where applicable.

## Heterogeneous single batch: `mixedRead()`

**`TypedRecordStream<T>` is not a multiplexed stream.** It always maps to one `T`. For **multiple entity types in one batch**, use:

```java
MixedTypedBatchReadResult r = session.mixedRead()
    .segment(customerDataSet.ids(1L, 2L))
    .segment(orderDataSet.ids(10L))
    .execute();

List<Customer> c = r.toObjects(0, Customer.class);
List<Order> o = r.toObjects(1, Order.class);
```

Until you adopt this API, you can issue **two** `queryTypedKeys` calls or one `query(List<Key>)` and demux by namespace/set manually.

## Prior work vs this API set

The following are part of the typed mapping surface and work together with this guide:

- **`TypedKey<T>`** — key plus `Class<T>` for typed query entry.
- **`TypedRecordStream<T>`** / **`TypedNavigatableRecordStream<T>`** — factory-based mapping and navigable views.
- **`OperationObjectBuilder`** / **`ObjectBuilder`** — object write paths tied to `TypedDataSet` and mappers.

Cross-cutting **async / `CompletableFuture`** mapping is described separately in [`COMPLETABLE_FUTURE_SUPPORT.md`](../../../COMPLETABLE_FUTURE_SUPPORT.md) at the repo root when applicable.
