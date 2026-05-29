package com.aerospike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.aerospike.client.Key;

/**
 * Builds a single batch read of multiple typed key segments (e.g. products then customers),
 * then splits results back per segment for independent mapping.
 */
public final class MixedTypedBatchReadBuilder {

    private final Session session;
    private final List<List<Key>> segments = new ArrayList<>();

    MixedTypedBatchReadBuilder(Session session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    /**
     * Appends a segment of keys (same logical type at the Aerospike level; mapping uses {@code clazz} at read time).
     */
    public <U> MixedTypedBatchReadBuilder segment(List<TypedKey<U>> keys) {
        Objects.requireNonNull(keys, "keys");
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("Segment must contain at least one TypedKey");
        }
        List<Key> nativeKeys = Session.nativeKeysFromTyped(keys);
        segments.add(nativeKeys);
        return this;
    }

    @SafeVarargs
    public final <U> MixedTypedBatchReadBuilder segment(TypedKey<U> first, TypedKey<U> second, TypedKey<U>... rest) {
        List<TypedKey<U>> list = new ArrayList<>();
        list.add(first);
        list.add(second);
        Collections.addAll(list, rest);
        return segment(list);
    }

    public MixedTypedBatchReadResult execute() {
        if (segments.isEmpty()) {
            throw new IllegalStateException("Add at least one segment with segment(...) before execute()");
        }
        List<Key> allKeys = new ArrayList<>();
        List<int[]> ranges = new ArrayList<>();
        int offset = 0;
        for (List<Key> seg : segments) {
            allKeys.addAll(seg);
            ranges.add(new int[] { offset, seg.size() });
            offset += seg.size();
        }
        List<RecordResult> flat = new ArrayList<>();
        try (RecordStream rs = session.query(allKeys).execute()) {
            while (rs.hasNext()) {
                flat.add(rs.next());
            }
        }
        List<List<RecordResult>> bySegment = new ArrayList<>();
        int readPos = 0;
        for (int[] r : ranges) {
            int len = r[1];
            List<RecordResult> part = new ArrayList<>(len);
            for (int i = 0; i < len && readPos < flat.size(); i++) {
                part.add(flat.get(readPos++));
            }
            bySegment.add(part);
        }
        return new MixedTypedBatchReadResult(session, bySegment);
    }
}
