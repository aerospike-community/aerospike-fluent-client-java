package com.aerospike.query;

import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.Txn;
import com.aerospike.dsl.BooleanExpression;

/**
 * Base interface for all query builders with common methods.
 * Uses self-referencing generics to maintain fluent API.
 */
public interface BaseQueryBuilder<T extends BaseQueryBuilder<T>> {
    
    /**
     * Specifies which bins to read from the records.
     * 
     * <p>This method allows you to optimize query performance by only reading
     * the bins you need. If not specified, all bins will be read.</p>
     * 
     * <p>This method cannot be used together with {@link #withNoBins()}.</p>
     * 
     * @param binNames the names of the bins to read
     * @return this QueryBuilder for method chaining
     * @throws IllegalArgumentException if used together with withNoBins()
     */
    T readingOnlyBins(String ... binNames);
    
    /**
     * Specifies that no bins should be read (header-only query).
     * 
     * <p>This method is useful when you only need to check for record existence
     * or get metadata like generation numbers, without reading the actual data.</p>
     * 
     * <p>This method cannot be used together with {@link #readingOnlyBins(String...)}.</p>
     * 
     * @return this QueryBuilder for method chaining
     * @throws IllegalArgumentException if used together with readingOnlyBins()
     */
    T withNoBins();
    
    /**
     * Sets the maximum number of records to return.
     * 
     * <p>This method limits the total number of records returned by the query.
     * Once the limit is reached, the query will stop processing.</p>
     * 
     * @param limit the maximum number of records to return (must be > 0)
     * @return this QueryBuilder for method chaining
     * @throws IllegalArgumentException if limit is <= 0
     */
    T limit(long limit);
    
    /**
     * Sets the page size for paginated results.
     * 
     * <p>This method controls how many records are returned per page when using
     * pagination. The page size affects memory usage and network round trips.</p>
     * 
     * @param pageSize the number of records per page (must be > 0)
     * @return this QueryBuilder for method chaining
     * @throws IllegalArgumentException if pageSize is <= 0
     */
    T pageSize(int pageSize);
    
    /**
     * Targets a specific partition for the query.
     * 
     * <p>This method restricts the query to a single partition. This can be useful
     * for load balancing or when you know the data distribution across partitions.</p>
     * 
     * @param partId the partition ID to target (0-4095)
     * @return this QueryBuilder for method chaining
     * @throws IllegalArgumentException if partId is out of range
     */
    T onPartition(int partId);
    
    /**
     * Targets a range of partitions for the query.
     * 
     * <p>This method restricts the query to a specific range of partitions. This
     * can be useful for load balancing, parallel processing, or when you know
     * the data distribution across partitions.</p>
     * 
     * <p>The partition range can only be set once per query. Subsequent calls
     * with different ranges will throw an exception.</p>
     * 
     * @param startIncl the start partition (inclusive, 0-4095)
     * @param endExcl the end partition (exclusive, 1-4096)
     * @return this QueryBuilder for method chaining
     * @throws IllegalArgumentException if the partition range is invalid or already set
     */
    T onPartitionRange(int startIncl, int endExcl);
    
    /**
     * Adds a sort field in ascending order with case sensitivity.
     * 
     * <p>This method adds a field to the sort criteria. Multiple sort fields can
     * be added, and they will be applied in the order they are added.</p>
     * 
     * @param field the field name to sort by
     * @return this QueryBuilder for method chaining
     */
    T sortReturnedSubsetBy(String field);
    
    /**
     * Adds a sort field in ascending order with specified case sensitivity.
     * 
     * @param field the field name to sort by
     * @param caseInsensitive true for case-insensitive sorting, false for case-sensitive
     * @return this QueryBuilder for method chaining
     */
    T sortReturnedSubsetBy(String field, boolean caseInsensitive);
    
    /**
     * Adds a sort field with specified direction and case sensitivity.
     * 
     * @param field the field name to sort by
     * @param sortDir the sort direction (ascending or descending)
     * @return this QueryBuilder for method chaining
     */
    T sortReturnedSubsetBy(String field, SortDir sortDir);
    
    /**
     * Adds a sort field with specified direction and case sensitivity.
     * 
     * <p>This method allows you to specify the complete sort criteria for a field.
     * Multiple sort fields can be added, and they will be applied in the order
     * they are added.</p>
     * 
     * <p>Note: Sorting requires that a limit be set on the query to prevent
     * excessive memory usage.</p>
     * 
     * @param field the field name to sort by
     * @param sortDir the sort direction (ascending or descending)
     * @param caseSensitive true for case-sensitive sorting, false for case-insensitive
     * @return this QueryBuilder for method chaining
     */
    T sortReturnedSubsetBy(String field, SortDir sortDir, boolean caseSensitive);
    
    /**
     * Adds a filter condition using a DSL string.
     * 
     * <p>This method allows you to specify a filter condition using Aerospike's
     * Domain Specific Language (DSL). The DSL provides a SQL-like syntax for
     * expressing complex filter conditions.</p>
     * 
     * <p>Example DSL expressions:</p>
     * <ul>
     *   <li><code>"$.name == 'Tim'"</code> - exact string match</li>
     *   <li><code>"$.age > 30"</code> - numeric comparison</li>
     *   <li><code>"$.name == 'Tim' and $.age > 30"</code> - logical AND</li>
     *   <li><code>"$.name == 'Tim' or $.name == 'Jane'"</code> - logical OR</li>
     * </ul>
     * 
     * <p>Only one filter condition can be specified per query. Multiple calls
     * to this method or {@link #where(BooleanExpression)} will throw an exception.</p>
     * 
     * @param dsl the DSL filter expression
     * @param params The params used to replace arguments in the DSL string (used by {@code String.format(dsl, params)}
     * @return this QueryBuilder for method chaining
     * @throws IllegalArgumentException if multiple filter conditions are specified
     */
    T where(String dsl, Object ... params);
    
    /**
     * Adds a filter condition using a BooleanExpression.
     * 
     * <p>This method allows you to specify a filter condition using the programmatic
     * BooleanExpression API. This provides type safety and compile-time checking
     * compared to DSL strings.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanExpression filter = Dsl.stringBin("name").eq("Tim")
     *     .and(Dsl.longBin("age").gt(30));
     * 
     * RecordStream results = session.query(customerDataSet)
     *     .where(filter)
     *     .execute();
     * }</pre>
     * 
     * <p>Only one filter condition can be specified per query. Multiple calls
     * to this method or {@link #where(String)} will throw an exception.</p>
     * 
     * @param dsl the BooleanExpression filter
     * @return this QueryBuilder for method chaining
     * @throws IllegalArgumentException if multiple filter conditions are specified
     */
    T where(BooleanExpression dsl);
    
    /**
     * Specifies that these operations are not to be included in any transaction, even if a
     * transaction exists on the underlying session.
     * 
     * <p>This method explicitly excludes the query from any active transaction,
     * ensuring it runs as a standalone operation.</p>
     * 
     * @return this QueryBuilder for method chaining
     */
    T notInAnyTransaction();
    
    /**
     * Specify the transaction to use for this call. Note that this should not be commonly used.
     * A better pattern is to use the {@code doInTransaction} method on {@link Session}:
     * <pre>
     * session.doInTransaction(txnSession -> {
     *     Optional<KeyRecord> result = txnSession.query(customerDataSet.id(1)).execute().getFirst();
     *     // Do stuff...
     *     txnSession.insertInto(customerDataSet.id(3));
     *     txnSession.delete(customerDataSet.id(3));
     * });
     * </pre> 
     * 
     * This method should only be used in situations where different parts of a transaction are not all
     * within the same context, for example forming a transaction on callbacks from a file system. 
     * @param txn - the transaction to use
     */
    T inTransaction(Txn txn);
    
    /**
     * Executes the query and returns a RecordStream.
     * 
     * <p>This method executes the query with all the configured parameters and
     * returns a RecordStream that can be used to iterate through the results.</p>
     * 
     * <p>The RecordStream provides methods for:</p>
     * <ul>
     *   <li>Iterating through results: {@link RecordStream#hasNext()}, {@link RecordStream#next()}</li>
     *   <li>Pagination: {@link RecordStream#hasMorePages()}</li>
     *   <li>Sorting: {@link RecordStream#asSortable()}</li>
     *   <li>Object conversion: {@link RecordStream#toObjectList(RecordMapper)}</li>
     * </ul>
     * 
     * @return a RecordStream containing the query results
     * @see RecordStream
     */
    RecordStream execute();
}
