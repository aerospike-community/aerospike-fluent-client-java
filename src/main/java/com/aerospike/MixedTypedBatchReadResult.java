package com.aerospike;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.aerospike.client.Record;

/**
 * Result of {@link MixedTypedBatchReadBuilder#execute()}: one {@link List} of {@link RecordResult} per segment.
 */
public final class MixedTypedBatchReadResult {

    private final Session session;
    private final List<List<RecordResult>> segments;

    MixedTypedBatchReadResult(Session session, List<List<RecordResult>> segments) {
        this.session = Objects.requireNonNull(session, "session");
        this.segments = List.copyOf(segments);
    }

    public int segmentCount() {
        return segments.size();
    }

    public List<RecordResult> records(int segmentIndex) {
        return List.copyOf(segments.get(segmentIndex));
    }

    /**
     * Maps every record in the segment to {@code T} using the {@link RecordMappingFactory}.
     */
    public <T> List<T> toObjects(int segmentIndex, Class<T> clazz) {
        RecordReadContext<T> ctx = new RecordReadContext<>(session, clazz);
        RecordMapper<T> mapper = MappingSupport.requireMapper(ctx.getRecordMappingFactory(), clazz);
        List<T> out = new ArrayList<>();
        for (RecordResult rr : segments.get(segmentIndex)) {
            Record rec = rr.recordOrThrow();
            out.add(mapper.fromMap(rec.bins, rr.key(), rec.generation, ctx));
        }
        return out;
    }
}
