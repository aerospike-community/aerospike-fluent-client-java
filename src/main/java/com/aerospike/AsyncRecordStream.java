package com.aerospike;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

//A push-driven stream that supports backpressure and cancellation.
public final class AsyncRecordStream implements AutoCloseable, Iterable<RecordResult> {
    private static final Object END = new Object();
    private static final class Err { final Throwable t; Err(Throwable t){ this.t = t; } }

    private final BlockingQueue<Object> queue;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    // Optional: give producers a way to see if they should stop.
    private final BooleanSupplier cancelled = () -> closed.get() || completed.get();

    public AsyncRecordStream(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    /** For producers: push a result if we are still open. Blocks when backpressure applies. */
    public void publish(RecordResult result) {
        if (result == null) return;
        if (cancelled.getAsBoolean()) return; // best effort
        // Block with backpressure, but wake up promptly if closed/completed
        for (;;) {
            if (cancelled.getAsBoolean()) return;
            try {
                if (queue.offer(result, 50, TimeUnit.MILLISECONDS)) return;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** For producers: signal a terminal error (the consumer will see it as a runtime exception). */
    public void error(Throwable t) {
        if (t == null) t = new RuntimeException("Unknown error");
        if (completed.get()) return;
        // Try to enqueue the error; if we can't, close the stream immediately
        if (!queue.offer(new Err(t))) {
            close();
        }
    }

    /** For producers: signal normal completion. Safe to call multiple times. */
    public void complete() {
        if (completed.compareAndSet(false, true)) {
            // Ensure consumer unblocks even if queue is full
            queue.offer(END);
        }
    }

    /** For consumers: a standard Java Stream view. Closing the stream cancels producers. */
    public Stream<RecordResult> stream() {
        return StreamSupport.stream(spliterator(), false).onClose(this::close);
    }

    /** A lightweight cancellation token for producers. */
    public BooleanSupplier cancelled() { return cancelled; }

    /** Cancel consumption & production early. Idempotent. */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            // Drain quickly to keep memory bounded, then unblock consumer.
            queue.clear();
            queue.offer(END);
        }
    }

    // --- Iterable / Spliterator plumbing so you can use for-each or stream() ---

    @Override
    public Iterator<RecordResult> iterator() {
        return new Iterator<>() {
            Object next = fetch();

            @Override public boolean hasNext() {
                return !(next == END || next instanceof Err);
            }

            @Override public RecordResult next() {
                if (next == END) throw new NoSuchElementException();
                if (next instanceof Err e) {
                    // Propagate as unchecked
                    RuntimeException re = (e.t instanceof RuntimeException r) ? r : new RuntimeException(e.t);
                    // Advance to END so further calls behave
                    next = END;
                    throw re;
                }
                @SuppressWarnings("unchecked")
                RecordResult rr = (RecordResult) next;
                next = fetch();
                return rr;
            }

            private Object fetch() {
                for (;;) {
                    try {
                        Object o = queue.take();
                        if (o == END) {
                            // Ensure terminal state is visible to publishers
                            completed.set(true);
                            return END;
                        }
                        return o;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return END;
                    }
                }
            }
        };
    }

    @Override
    public Spliterator<RecordResult> spliterator() {
        // Unknown size, ordered, non-null, concurrent-ish
        return Spliterators.spliteratorUnknownSize(iterator(),
                Spliterator.ORDERED | Spliterator.NONNULL);
    }
}
