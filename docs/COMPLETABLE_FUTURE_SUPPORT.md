# CompletableFuture Support in RecordStream

This document describes the `CompletableFuture` integration in the Aerospike Fluent Client, including design decisions, usage patterns, and integration with reactive streams.

## Table of Contents

- [Design Decision: CompletableFuture vs CompletionStage](#design-decision-completablefuture-vs-completionstage)
- [API Overview](#api-overview)
- [Single Record Operations](#single-record-operations)
- [Batch (Multi-Key) Operations](#batch-multi-key-operations)
- [Object Mapping](#object-mapping)
- [Custom Executors](#custom-executors)
- [Error Handling](#error-handling)
- [Integration with Reactive Streams (Flux)](#integration-with-reactive-streams-flux)
- [Best Practices](#best-practices)

---

## Design Decision: CompletableFuture vs CompletionStage

### The Question

When adding async support to `RecordStream`, we needed to decide between returning:
- `CompletionStage<T>` - the interface
- `CompletableFuture<T>` - the concrete implementation

### Why We Chose CompletableFuture

We chose to return `CompletableFuture` directly for the following reasons:

#### 1. Industry Standard Practice

Major Java frameworks and libraries return `CompletableFuture` directly:

| Library/Framework | Returns |
|-------------------|---------|
| **AWS SDK v2** | `CompletableFuture<T>` |
| **Java HTTP Client (JDK 11+)** | `CompletableFuture<T>` |
| **Spring WebClient** | `CompletableFuture<T>` (via Mono.toFuture()) |
| **Vert.x** | `Future<T>` with `toCompletionStage()` |
| **Apache HttpClient 5** | `Future<T>` |

The AWS SDK v2 team, in particular, explicitly chose `CompletableFuture` for ergonomics despite it exposing completion methods that callers shouldn't use.

#### 2. Missing Convenience Methods on CompletionStage

`CompletionStage` lacks several commonly-used methods that are only available on `CompletableFuture`:

```java
// These methods are NOT on CompletionStage:
future.join();                    // Blocking get without checked exceptions
future.isDone();                  // Check completion status
future.isCompletedExceptionally(); // Check for errors
future.getNow(defaultValue);      // Non-blocking peek
future.orTimeout(5, SECONDS);     // Timeout handling (Java 9+)
future.completeOnTimeout(val, 5, SECONDS); // Default on timeout
```

With `CompletionStage`, users would need to call `.toCompletableFuture()` constantly:

```java
// Awkward with CompletionStage
CompletionStage<Customer> stage = rs.asCompletionStage(mapper);
Customer c = stage.toCompletableFuture().join(); // Extra step every time

// Clean with CompletableFuture
CompletableFuture<Customer> future = rs.asCompletableFuture(mapper);
Customer c = future.join(); // Direct access
```

#### 3. The `join()` Method

The `join()` method is significantly more convenient than `get()`:
- `get()` throws checked `InterruptedException` and `ExecutionException`
- `join()` wraps exceptions in unchecked `CompletionException`

This matters for lambda-heavy code:

```java
// With get() - requires try-catch everywhere
try {
    Customer c = future.get();
} catch (InterruptedException | ExecutionException e) {
    throw new RuntimeException(e);
}

// With join() - clean
Customer c = future.join();
```

#### 4. Minimal Real Risk

While `CompletableFuture` exposes methods like `complete()` and `completeExceptionally()` that callers shouldn't use:
- It's obviously incorrect usage
- It only affects the caller's own code
- It's self-inflicted, not a security concern
- Documentation can warn against it

### The Trade-off

Returning `CompletionStage` would have been "more correct" from a pure API design standpoint, as it prevents callers from calling completion methods. However, the practical benefits of `CompletableFuture` outweigh this theoretical purity.

---

## API Overview

`RecordStream` provides the following `CompletableFuture` methods:

### Single Record Methods
| Method | Returns | Use Case |
|--------|---------|----------|
| `asCompletableFuture()` | `CompletableFuture<RecordResult>` | Single-key operations |
| `asCompletableFuture(mapper)` | `CompletableFuture<T>` | Single-key with object mapping |
| `asCompletableFuture(executor)` | `CompletableFuture<RecordResult>` | Single-key with custom executor |
| `asCompletableFuture(mapper, executor)` | `CompletableFuture<T>` | Full control |

### Batch (Multi-Record) Methods
| Method | Returns | Use Case |
|--------|---------|----------|
| `asCompletableFutureList()` | `CompletableFuture<List<RecordResult>>` | Batch operations |
| `asCompletableFutureList(mapper)` | `CompletableFuture<List<T>>` | Batch with object mapping |
| `asCompletableFutureList(executor)` | `CompletableFuture<List<RecordResult>>` | Batch with custom executor |
| `asCompletableFutureList(mapper, executor)` | `CompletableFuture<List<T>>` | Full control |

---

## Single Record Operations

Use `asCompletableFuture()` for operations that return a single record.

### Basic Usage

```java
// Query a single record
CompletableFuture<RecordResult> future = session
    .query(customerDataSet.id(102))
    .execute()
    .asCompletableFuture();

// Blocking wait
RecordResult result = future.join();
String name = result.recordOrThrow().getString("name");

// Non-blocking callback
future.thenAccept(result -> {
    System.out.println("Name: " + result.recordOrThrow().getString("name"));
});
```

### Insert/Update/Delete Operations

```java
// Insert and get the result asynchronously
CompletableFuture<RecordResult> insertFuture = session
    .insert(customerDataSet.id(999))
    .bin("name").setTo("New Customer")
    .bin("age").setTo(25)
    .execute()
    .asCompletableFuture();

// Chain operations
insertFuture
    .thenAccept(result -> System.out.println("Insert successful for: " + result.key()))
    .exceptionally(ex -> {
        System.err.println("Insert failed: " + ex.getMessage());
        return null;
    });
```

### Composing Multiple Operations

```java
// Read-modify-write pattern
CompletableFuture<Void> workflow = session
    .query(customerDataSet.id(102))
    .execute()
    .asCompletableFuture()
    .thenCompose(result -> {
        int currentAge = result.recordOrThrow().getInt("age");
        return session
            .update(customerDataSet.id(102))
            .bin("age").setTo(currentAge + 1)
            .execute()
            .asCompletableFuture();
    })
    .thenAccept(updateResult -> {
        System.out.println("Age incremented successfully");
    });
```

---

## Batch (Multi-Key) Operations

Use `asCompletableFutureList()` for operations that return multiple records.

### Basic Batch Query

```java
// Query multiple keys
CompletableFuture<List<RecordResult>> future = session
    .query(customerDataSet.ids(1, 2, 3, 4, 5))
    .execute()
    .asCompletableFutureList();

// Process all results
future.thenAccept(results -> {
    results.forEach(result -> {
        if (result.resultCode() == ResultCode.OK) {
            System.out.println(result.key() + " -> " + result.recordOrThrow());
        } else {
            System.out.println(result.key() + " failed: " + result.message());
        }
    });
});
```

### Batch Updates

```java
// Update multiple records and wait for all to complete
CompletableFuture<List<RecordResult>> future = session
    .update(customerDataSet.ids(1, 2, 3))
    .bin("lastUpdated").setTo(System.currentTimeMillis())
    .execute()
    .asCompletableFutureList();

List<RecordResult> results = future.join();

// Check for failures
List<RecordResult> failures = results.stream()
    .filter(r -> r.resultCode() != ResultCode.OK)
    .toList();

if (!failures.isEmpty()) {
    System.err.println("Some updates failed: " + failures.size());
}
```

### Parallel Batch Operations

```java
// Execute multiple batch operations in parallel
CompletableFuture<List<RecordResult>> customers = session
    .query(customerDataSet.ids(1, 2, 3))
    .execute()
    .asCompletableFutureList();

CompletableFuture<List<RecordResult>> orders = session
    .query(orderDataSet.ids(100, 101, 102))
    .execute()
    .asCompletableFutureList();

// Wait for both
CompletableFuture.allOf(customers, orders).thenRun(() -> {
    System.out.println("Loaded " + customers.join().size() + " customers");
    System.out.println("Loaded " + orders.join().size() + " orders");
});
```

---

## Object Mapping

Both single and batch operations support `RecordMapper` for automatic object conversion.

### Defining a Mapper

```java
public class CustomerMapper implements RecordMapper<Customer> {
    
    @Override
    public Customer fromMap(Map<String, Object> map, Key recordKey, int generation) {
        return new Customer(
            (Long) map.get("id"),
            (String) map.get("name"),
            (Integer) map.get("age"),
            (String) map.get("email")
        );
    }
    
    @Override
    public Map<String, Value> toMap(Customer customer) {
        Map<String, Value> map = new HashMap<>();
        map.put("id", Value.get(customer.getId()));
        map.put("name", Value.get(customer.getName()));
        map.put("age", Value.get(customer.getAge()));
        map.put("email", Value.get(customer.getEmail()));
        return map;
    }
    
    @Override
    public Object id(Customer customer) {
        return customer.getId();
    }
}
```

### Single Record with Mapping

```java
RecordMapper<Customer> mapper = new CustomerMapper();

CompletableFuture<Customer> future = session
    .query(customerDataSet.id(102))
    .execute()
    .asCompletableFuture(mapper);

// Get the typed object
Customer customer = future.join();
System.out.println("Customer: " + customer.getName());
```

### Batch with Mapping

```java
RecordMapper<Customer> mapper = new CustomerMapper();

CompletableFuture<List<Customer>> future = session
    .query(customerDataSet.ids(1, 2, 3, 4, 5))
    .execute()
    .asCompletableFutureList(mapper);

List<Customer> customers = future.join();
customers.forEach(c -> System.out.println(c.getName() + " is " + c.getAge()));
```

---

## Custom Executors

By default, `CompletableFuture` operations use `ForkJoinPool.commonPool()`. You can specify a custom executor for better control.

### Using a Custom Thread Pool

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

CompletableFuture<RecordResult> future = session
    .query(customerDataSet.id(102))
    .execute()
    .asCompletableFuture(executor);

// Don't forget to shutdown the executor when done
executor.shutdown();
```

### Virtual Threads (Java 21+)

```java
ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

CompletableFuture<Customer> future = session
    .query(customerDataSet.id(102))
    .execute()
    .asCompletableFuture(mapper, virtualExecutor);
```

### Dedicated Aerospike Executor

```java
// Create a dedicated executor for Aerospike operations
ExecutorService aerospikeExecutor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() * 2,
    r -> {
        Thread t = new Thread(r, "aerospike-async");
        t.setDaemon(true);
        return t;
    }
);

// Use throughout your application
CompletableFuture<List<Customer>> future = session
    .query(customerDataSet.ids(1, 2, 3))
    .execute()
    .asCompletableFutureList(mapper, aerospikeExecutor);
```

---

## Error Handling

### Exception Types

When using `CompletableFuture` methods:
- `NoSuchElementException` - thrown if the stream is empty
- Aerospike exceptions - wrapped and thrown when `recordOrThrow()` is called internally
- Mapping exceptions - thrown if the `RecordMapper` fails

### Handling Errors

```java
CompletableFuture<Customer> future = session
    .query(customerDataSet.id(102))
    .execute()
    .asCompletableFuture(mapper);

future
    .thenAccept(customer -> {
        System.out.println("Found: " + customer.getName());
    })
    .exceptionally(ex -> {
        Throwable cause = ex.getCause();
        if (cause instanceof NoSuchElementException) {
            System.out.println("Customer not found");
        } else if (cause instanceof AeroException ae) {
            System.err.println("Aerospike error: " + ae.getResultCode());
        } else {
            System.err.println("Unexpected error: " + cause.getMessage());
        }
        return null;
    });
```

### Handling Partial Failures in Batches

```java
// For batches where some records may fail, use asCompletableFutureList()
// without a mapper to access individual result codes
CompletableFuture<List<RecordResult>> future = session
    .query(customerDataSet.ids(1, 2, 3, 999)) // 999 doesn't exist
    .execute()
    .asCompletableFutureList();

future.thenAccept(results -> {
    for (RecordResult result : results) {
        if (result.resultCode() == ResultCode.OK) {
            System.out.println("Success: " + result.key());
        } else {
            System.out.println("Failed: " + result.key() + " - " + result.message());
        }
    }
});
```

### Timeout Handling

```java
CompletableFuture<Customer> future = session
    .query(customerDataSet.id(102))
    .execute()
    .asCompletableFuture(mapper)
    .orTimeout(5, TimeUnit.SECONDS);

try {
    Customer customer = future.join();
} catch (CompletionException e) {
    if (e.getCause() instanceof TimeoutException) {
        System.err.println("Query timed out");
    }
}
```

---

## Integration with Reactive Streams (Flux)

For large queries that could return millions or billions of records, `CompletableFuture` is not appropriate as it would require loading all records into memory. Instead, you can integrate with Project Reactor's `Flux` for true reactive streaming with backpressure support.

**Note:** The Aerospike Fluent Client does not include a dependency on Project Reactor to keep the library lightweight. Below is a sample adapter that you can include in your own project.

### Maven Dependency

```xml
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-core</artifactId>
    <version>3.6.0</version>
</dependency>
```

### Sample Flux Adapter

```java
package com.example.adapters;

import com.aerospike.RecordMapper;
import com.aerospike.RecordResult;
import com.aerospike.RecordStream;
import com.aerospike.client.ResultCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Adapter to bridge Aerospike RecordStream to Project Reactor Flux.
 * 
 * This adapter enables reactive stream processing of Aerospike query results
 * with full backpressure support. The RecordStream is consumed lazily as
 * downstream subscribers request elements.
 */
public final class RecordStreamFluxAdapter {
    
    private RecordStreamFluxAdapter() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Converts a RecordStream to a Flux of RecordResults.
     * 
     * The returned Flux supports backpressure. The RecordStream will be
     * consumed lazily as downstream subscribers request elements. The stream
     * is automatically closed when the Flux terminates.
     * 
     * @param recordStream the RecordStream to convert
     * @return a Flux that emits RecordResult elements
     */
    public static Flux<RecordResult> toFlux(RecordStream recordStream) {
        return Flux.create(sink -> {
            // Handle cancellation - close the stream if subscriber cancels
            sink.onCancel(() -> recordStream.close());
            sink.onDispose(() -> recordStream.close());
            
            emitRecords(recordStream, sink);
        }, FluxSink.OverflowStrategy.BUFFER);
    }
    
    /**
     * Converts a RecordStream to a Flux of mapped domain objects.
     * 
     * @param <T> the target type
     * @param recordStream the RecordStream to convert
     * @param mapper the mapper to convert records to domain objects
     * @return a Flux that emits mapped objects
     */
    public static <T> Flux<T> toFlux(RecordStream recordStream, RecordMapper<T> mapper) {
        return toFlux(recordStream)
            .filter(result -> result.resultCode() == ResultCode.OK)
            .map(result -> {
                var rec = result.recordOrThrow();
                return mapper.fromMap(rec.bins, result.key(), rec.generation);
            });
    }
    
    /**
     * Converts a RecordStream to a Flux, filtering out failed records.
     * 
     * Unlike {@link #toFlux(RecordStream)}, this method silently skips
     * records with non-OK result codes instead of propagating them.
     * 
     * @param recordStream the RecordStream to convert
     * @return a Flux that emits only successful RecordResults
     */
    public static Flux<RecordResult> toFluxIgnoreErrors(RecordStream recordStream) {
        return toFlux(recordStream)
            .filter(result -> result.resultCode() == ResultCode.OK);
    }
    
    /**
     * Converts a RecordStream to a Flux with a custom overflow strategy.
     * 
     * @param recordStream the RecordStream to convert
     * @param overflowStrategy how to handle backpressure overflow
     * @return a Flux that emits RecordResult elements
     */
    public static Flux<RecordResult> toFlux(
            RecordStream recordStream, 
            FluxSink.OverflowStrategy overflowStrategy) {
        
        return Flux.create(sink -> {
            sink.onCancel(() -> recordStream.close());
            sink.onDispose(() -> recordStream.close());
            emitRecords(recordStream, sink);
        }, overflowStrategy);
    }
    
    private static void emitRecords(RecordStream recordStream, FluxSink<RecordResult> sink) {
        try {
            while (recordStream.hasNext() && !sink.isCancelled()) {
                RecordResult result = recordStream.next();
                sink.next(result);
            }
            if (!sink.isCancelled()) {
                sink.complete();
            }
        } catch (Exception e) {
            sink.error(e);
        } finally {
            recordStream.close();
        }
    }
}
```

### Usage Examples

#### Basic Streaming

```java
RecordStream rs = session.query(customerDataSet).execute();
Flux<RecordResult> flux = RecordStreamFluxAdapter.toFlux(rs);

flux.filter(r -> r.resultCode() == ResultCode.OK)
    .map(r -> r.recordOrThrow().getString("name"))
    .take(100)  // Only process first 100
    .subscribe(
        name -> System.out.println("Customer: " + name),
        error -> System.err.println("Error: " + error),
        () -> System.out.println("Done!")
    );
```

#### With Object Mapping

```java
RecordMapper<Customer> mapper = new CustomerMapper();
RecordStream rs = session.query(customerDataSet).execute();
Flux<Customer> flux = RecordStreamFluxAdapter.toFlux(rs, mapper);

flux.filter(c -> c.getAge() > 21)
    .buffer(100)  // Process in batches of 100
    .flatMap(batch -> processCustomerBatch(batch))
    .subscribe();
```

#### Backpressure and Parallel Processing

```java
RecordStream rs = session.query(largeDataSet).execute();

RecordStreamFluxAdapter.toFlux(rs)
    .limitRate(1000)  // Request 1000 records at a time
    .parallel(4)      // Process on 4 parallel rails
    .runOn(Schedulers.parallel())
    .map(r -> processRecord(r))
    .sequential()
    .subscribe();
```

#### Error Recovery

```java
RecordStreamFluxAdapter.toFlux(rs)
    .onErrorResume(e -> {
        log.error("Stream error, recovering", e);
        return Flux.empty();
    })
    .doOnComplete(() -> log.info("Processing complete"))
    .doFinally(signal -> log.info("Stream terminated: " + signal))
    .subscribe();
```

---

## Best Practices

### 1. Choose the Right Method

| Scenario | Method | Reason |
|----------|--------|--------|
| Single key lookup | `asCompletableFuture()` | Returns single result |
| Single key with mapping | `asCompletableFuture(mapper)` | Direct object conversion |
| Batch of known keys | `asCompletableFutureList()` | All results in memory |
| Large scans | `stream()` or Flux adapter | Avoid OOM |

### 2. Don't Block Unnecessarily

```java
// Avoid - blocks the thread
Customer c1 = future1.join();
Customer c2 = future2.join();

// Better - compose futures
CompletableFuture.allOf(future1, future2)
    .thenRun(() -> {
        Customer c1 = future1.join();
        Customer c2 = future2.join();
        // Process both
    });
```

### 3. Handle Timeouts

```java
// Always consider adding timeouts for production code
future.orTimeout(10, TimeUnit.SECONDS)
    .exceptionally(ex -> {
        if (ex.getCause() instanceof TimeoutException) {
            return defaultValue;
        }
        throw (RuntimeException) ex;
    });
```

### 4. Use Virtual Threads for High Concurrency (Java 21+)

```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// Execute many concurrent operations efficiently
List<CompletableFuture<Customer>> futures = customerIds.stream()
    .map(id -> session.query(dataSet.id(id))
        .execute()
        .asCompletableFuture(mapper, executor))
    .toList();

List<Customer> customers = futures.stream()
    .map(CompletableFuture::join)
    .toList();
```

### 5. Don't Call Completion Methods

While `CompletableFuture` exposes methods like `complete()`, `completeExceptionally()`, and `obtrudeValue()`, you should **never** call these on futures returned by `RecordStream`. They are for the producer (the library), not the consumer (your code).

```java
// NEVER DO THIS
CompletableFuture<RecordResult> future = rs.asCompletableFuture();
future.complete(someOtherResult);  // DON'T!
future.completeExceptionally(new Exception()); // DON'T!
```

---

## Summary

The `CompletableFuture` integration in `RecordStream` provides a standard Java way to work with asynchronous Aerospike operations:

- **Single records**: Use `asCompletableFuture()` variants
- **Batches**: Use `asCompletableFutureList()` variants
- **Object mapping**: Pass a `RecordMapper` to any method
- **Large queries**: Use the Flux adapter for reactive streaming

This design follows industry standards (AWS SDK v2, Java HTTP Client) by returning `CompletableFuture` directly, providing maximum convenience while maintaining a clean, idiomatic Java API.
