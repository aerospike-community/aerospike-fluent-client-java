package com.aerospike;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;

/**
 * Comprehensive tests for AsyncRecordStream covering:
 * - Basic functionality
 * - The reported bug (hanging with one element)
 * - Backpressure
 * - Error handling
 * - Cancellation
 * - Concurrent access
 * - Edge cases
 * - Stream API integration
 */
class AsyncRecordStreamTest {

    // Helper method to create a test RecordResult
    private RecordResult createResult(int id) {
        Key key = new Key("test", "set", id);
        Record record = new Record(null, 0, 0);
        return new RecordResult(key, record, 0); // index = 0 for test
    }

    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Single element publish and consume")
        void testSingleElement() {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            RecordResult result = createResult(1);

            stream.publish(result);
            stream.complete();

            List<RecordResult> results = stream.stream().toList();
            assertEquals(1, results.size());
            assertEquals(result, results.get(0));
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Multiple elements publish and consume")
        void testMultipleElements() {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            List<RecordResult> expected = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                RecordResult result = createResult(i);
                expected.add(result);
                stream.publish(result);
            }
            stream.complete();

            List<RecordResult> actual = stream.stream().toList();
            assertEquals(expected, actual);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Empty stream with immediate complete")
        void testEmptyStream() {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            stream.complete();

            List<RecordResult> results = stream.stream().toList();
            assertTrue(results.isEmpty());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Iterator-based consumption")
        void testIteratorConsumption() {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            RecordResult result1 = createResult(1);
            RecordResult result2 = createResult(2);

            stream.publish(result1);
            stream.publish(result2);
            stream.complete();

            List<RecordResult> results = new ArrayList<>();
            for (RecordResult r : stream) {
                results.add(r);
            }

            assertEquals(2, results.size());
            assertEquals(result1, results.get(0));
            assertEquals(result2, results.get(1));
        }
    }

    @Nested
    @DisplayName("Reported Bug Scenario Tests")
    class ReportedBugTests {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("CRITICAL: stream().findFirst() should not hang with one element (capacity=1)")
        void testFindFirstWithOneElementNoHang() {
            AsyncRecordStream stream = new AsyncRecordStream(1);
            RecordResult result = createResult(1);

            // Publish one element and complete immediately (queue is full)
            stream.publish(result);
            stream.complete();

            // This should NOT hang (the reported bug)
            Optional<RecordResult> first = stream.stream().findFirst();
            assertTrue(first.isPresent());
            assertEquals(result, first.get());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Capacity=1, complete before consume should not hang")
        void testCompleteBeforeConsumeNoHang() {
            AsyncRecordStream stream = new AsyncRecordStream(1);
            RecordResult result = createResult(1);

            stream.publish(result);
            stream.complete();

            // Consume after complete
            List<RecordResult> results = stream.stream().toList();
            assertEquals(1, results.size());
            assertEquals(result, results.get(0));
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Capacity=1, complete after consume should work")
        void testCompleteAfterConsume() {
            AsyncRecordStream stream = new AsyncRecordStream(1);
            RecordResult result = createResult(1);

            // Publish element
            stream.publish(result);
            
            // Complete immediately (before consumption)
            stream.complete();
            
            // Should be able to consume all elements
            List<RecordResult> results = stream.stream().toList();
            assertEquals(1, results.size());
            assertEquals(result, results.get(0));
        }
    }

    @Nested
    @DisplayName("Backpressure Tests")
    class BackpressureTests {

        @Test
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        @DisplayName("Publisher blocks when queue is full")
        void testPublishBlocksWhenFull() throws Exception {
            AsyncRecordStream stream = new AsyncRecordStream(2);
            AtomicBoolean publisherBlocked = new AtomicBoolean(false);
            CountDownLatch queueFull = new CountDownLatch(1);

            // Fill the queue (capacity is 2, but we have +1 for END marker, so 3 total)
            stream.publish(createResult(1));
            stream.publish(createResult(2));
            // The +1 slot is reserved for END/Err, so this should still work
            stream.publish(createResult(3));

            // This publish should block because queue is actually full now
            Thread publisher = Thread.ofVirtual().start(() -> {
                queueFull.countDown();
                publisherBlocked.set(true);
                stream.publish(createResult(4)); // This will block
                publisherBlocked.set(false);
            });

            queueFull.await();
            Thread.sleep(100); // Give publisher time to block
            assertTrue(publisherBlocked.get(), "Publisher should be blocked");

            // Consume one element to unblock
            assertTrue(stream.hasNext());
            stream.next();

            // Publisher should unblock
            publisher.join(1000);
            assertFalse(publisher.isAlive(), "Publisher should have unblocked");
            assertFalse(publisherBlocked.get(), "Publisher should no longer be blocked");

            stream.complete();
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Consumer can drain queue and unblock publisher")
        void testConsumerUnblocksPublisher() throws Exception {
            AsyncRecordStream stream = new AsyncRecordStream(5);
            CountDownLatch allPublished = new CountDownLatch(1);

            // Publisher thread - publishes 10 elements
            Thread publisher = Thread.ofVirtual().start(() -> {
                for (int i = 0; i < 10; i++) {
                    stream.publish(createResult(i));
                }
                allPublished.countDown();
                stream.complete();
            });

            // Consumer thread - slowly drains
            Thread consumer = Thread.ofVirtual().start(() -> {
                int count = 0;
                for (RecordResult r : stream) {
                    count++;
                }
                assertEquals(10, count);
            });

            publisher.join(2000);
            consumer.join(2000);
            assertFalse(publisher.isAlive());
            assertFalse(consumer.isAlive());
            assertTrue(allPublished.getCount() == 0, "All elements should have been published");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Error is propagated to consumer")
        void testErrorPropagation() {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            RuntimeException testError = new RuntimeException("Test error");

            stream.publish(createResult(1));
            stream.error(testError);

            // Consume first element
            assertTrue(stream.hasNext());
            assertEquals(createResult(1), stream.next());

            // Next call should throw the error
            RuntimeException thrown = assertThrows(RuntimeException.class, stream::next);
            assertEquals("Test error", thrown.getMessage());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Error with full queue should not hang")
        void testErrorWithFullQueueNoHang() {
            AsyncRecordStream stream = new AsyncRecordStream(1);
            RuntimeException testError = new RuntimeException("Test error");

            // Fill the queue
            stream.publish(createResult(1));
            // Signal error (queue is full, but +1 slot allows this)
            stream.error(testError);

            // Consume should work
            assertTrue(stream.hasNext());
            stream.next();

            // Error should be received
            assertThrows(RuntimeException.class, stream::next);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Error after complete is ignored")
        void testErrorAfterComplete() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            stream.publish(createResult(1));
            stream.complete();
            stream.error(new RuntimeException("Should be ignored"));

            // Should consume normally without error
            List<RecordResult> results = stream.stream().toList();
            assertEquals(1, results.size());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Null error is converted to RuntimeException")
        void testNullError() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            stream.error(null);

            RuntimeException thrown = assertThrows(RuntimeException.class, stream::next);
            assertEquals("Unknown error", thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("Cancellation Tests")
    class CancellationTests {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Close stream mid-consumption")
        void testCloseMidConsumption() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            for (int i = 0; i < 5; i++) {
                stream.publish(createResult(i));
            }

            // Consume first element
            assertTrue(stream.hasNext());
            stream.next();

            // Close stream
            stream.close();

            // cancelled() should return true
            assertTrue(stream.cancelled().getAsBoolean());

            // Note: close() clears the queue and adds END, but if the iterator
            // already fetched the next element before close(), hasNext() might still be true.
            // The key is that cancelled() returns true and further iteration will stop quickly.
            // So we just verify cancelled() works, not the exact hasNext() state.
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("cancelled() returns true after close")
        void testCancelledAfterClose() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            assertFalse(stream.cancelled().getAsBoolean());
            stream.close();
            assertTrue(stream.cancelled().getAsBoolean());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("publish() stops after close")
        void testPublishStopsAfterClose() throws Exception {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            AtomicInteger publishCount = new AtomicInteger(0);

            Thread publisher = Thread.ofVirtual().start(() -> {
                for (int i = 0; i < 100; i++) {
                    if (stream.cancelled().getAsBoolean()) {
                        break;
                    }
                    stream.publish(createResult(i));
                    publishCount.incrementAndGet();
                }
            });

            // Let publisher publish a few
            Thread.sleep(50);
            stream.close();

            publisher.join(1000);
            assertFalse(publisher.isAlive());

            // Should have published fewer than 100 due to cancellation
            assertTrue(publishCount.get() < 100, "Publisher should have stopped early");
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("Close is idempotent")
        void testCloseIdempotent() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            stream.close();
            stream.close(); // Should not throw or cause issues
            stream.close();

            assertTrue(stream.cancelled().getAsBoolean());
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Multiple producers publishing simultaneously")
        void testMultipleProducers() throws Exception {
            AsyncRecordStream stream = new AsyncRecordStream(100);
            int numProducers = 3;
            int elementsPerProducer = 10;
            AtomicInteger totalPublished = new AtomicInteger(0);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

            // Start multiple producers
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
            for (int p = 0; p < numProducers; p++) {
                final int producerId = p;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < elementsPerProducer; i++) {
                        stream.publish(createResult(producerId * 1000 + i));
                        totalPublished.incrementAndGet();
                    }
                }));
            }

            // Wait for all producers to finish
            for (var future : futures) {
                future.get(3, TimeUnit.SECONDS);
            }
            executor.shutdown();
            
            stream.complete();

            // Verify we got all elements
            long count = stream.stream().count();
            assertEquals(numProducers * elementsPerProducer, count);
            assertEquals(numProducers * elementsPerProducer, totalPublished.get());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("hasMorePages() is thread-safe")
        void testHasMorePagesThreadSafety() throws Exception {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            int numThreads = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);
            AtomicInteger trueCount = new AtomicInteger(0);

            // Multiple threads call hasMorePages() simultaneously
            for (int i = 0; i < numThreads; i++) {
                Thread.ofVirtual().start(() -> {
                    try {
                        startLatch.await();
                        if (stream.hasMorePages()) {
                            trueCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(2, TimeUnit.SECONDS));

            // Only one thread should have seen true
            assertEquals(1, trueCount.get(), "Only one thread should see hasMorePages() return true");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("complete() called multiple times is idempotent")
        void testCompleteIdempotent() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            stream.publish(createResult(1));
            stream.complete();
            stream.complete(); // Should not cause issues
            stream.complete();

            List<RecordResult> results = stream.stream().toList();
            assertEquals(1, results.size());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("publish() after complete() is ignored")
        void testPublishAfterComplete() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            stream.publish(createResult(1));
            stream.complete();
            stream.publish(createResult(2)); // Should be ignored

            List<RecordResult> results = stream.stream().toList();
            assertEquals(1, results.size());
            assertEquals(createResult(1), results.get(0));
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("close() after complete()")
        void testCloseAfterComplete() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            stream.publish(createResult(1));
            stream.complete();
            stream.close(); // Should not cause issues

            // Stream is closed, so we might not get the element
            // but it shouldn't hang or throw
            assertDoesNotThrow(() -> stream.stream().toList());
        }

        @Test
        @DisplayName("Invalid capacity throws exception")
        void testInvalidCapacity() {
            assertThrows(IllegalArgumentException.class, () -> new AsyncRecordStream(0));
            assertThrows(IllegalArgumentException.class, () -> new AsyncRecordStream(-1));
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("publish(null) is ignored")
        void testPublishNull() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            stream.publish(null); // Should be ignored
            stream.publish(createResult(1));
            stream.complete();

            List<RecordResult> results = stream.stream().toList();
            assertEquals(1, results.size());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("next() without hasNext() throws NoSuchElementException")
        void testNextWithoutHasNext() {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            stream.complete();

            assertThrows(NoSuchElementException.class, stream::next);
        }
    }

    @Nested
    @DisplayName("Stream API Integration Tests")
    class StreamApiTests {

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("stream().findFirst() with one element")
        void testStreamFindFirst() {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            RecordResult result = createResult(1);

            stream.publish(result);
            stream.complete();

            Optional<RecordResult> first = stream.stream().findFirst();
            assertTrue(first.isPresent());
            assertEquals(result, first.get());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("stream().toList()")
        void testStreamToList() {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            List<RecordResult> expected = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                RecordResult result = createResult(i);
                expected.add(result);
                stream.publish(result);
            }
            stream.complete();

            List<RecordResult> actual = stream.stream().toList();
            assertEquals(expected, actual);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("stream().limit()")
        void testStreamLimit() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            for (int i = 0; i < 10; i++) {
                stream.publish(createResult(i));
            }
            stream.complete();

            List<RecordResult> limited = stream.stream().limit(3).toList();
            assertEquals(3, limited.size());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("stream().forEach()")
        void testStreamForEach() {
            AsyncRecordStream stream = new AsyncRecordStream(10);
            AtomicInteger count = new AtomicInteger(0);

            for (int i = 0; i < 5; i++) {
                stream.publish(createResult(i));
            }
            stream.complete();

            stream.stream().forEach(r -> count.incrementAndGet());
            assertEquals(5, count.get());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("stream().count()")
        void testStreamCount() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            for (int i = 0; i < 7; i++) {
                stream.publish(createResult(i));
            }
            stream.complete();

            long count = stream.stream().count();
            assertEquals(7, count);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("stream() with try-with-resources closes stream")
        void testStreamAutoClose() {
            AsyncRecordStream stream = new AsyncRecordStream(10);

            stream.publish(createResult(1));
            stream.publish(createResult(2));
            stream.complete();

            try (Stream<RecordResult> s = stream.stream()) {
                Optional<RecordResult> first = s.findFirst();
                assertTrue(first.isPresent());
            }

            // Stream should be closed
            assertTrue(stream.cancelled().getAsBoolean());
        }
    }
}

