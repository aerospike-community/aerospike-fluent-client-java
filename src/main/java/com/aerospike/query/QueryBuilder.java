package com.aerospike.query;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.DataSet;
import com.aerospike.RecordMapper;
import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.Key;
import com.aerospike.client.Txn;
import com.aerospike.client.cluster.Partition;
import com.aerospike.dsl.BooleanExpression;

/**
 * Builder class for constructing and executing queries against Aerospike.
 * 
 * <p>This class provides a fluent API for building complex queries with support for
 * filtering, sorting, pagination, and partition targeting. The QueryBuilder can be
 * used to query entire datasets, specific keys, or ranges of keys.</p>
 * 
 * <p>The QueryBuilder automatically selects the appropriate query implementation
 * based on the input:</p>
 * <ul>
 *   <li><strong>Dataset queries</strong>: Uses secondary indexes when available, falls back to scan</li>
 *   <li><strong>Single key queries</strong>: Direct key lookup</li>
 *   <li><strong>Multiple key queries</strong>: Batch key operations</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Query entire dataset with filtering
 * RecordStream results = session.query(customerDataSet)
 *     .where("$.name == 'Tim' and $.age > 30")
 *     .sortReturnedSubsetBy("age", SortDir.SORT_DESC)
 *     .limit(100)
 *     .pageSize(20)
 *     .execute();
 * 
 * // Query specific keys
 * RecordStream results = session.query(customerDataSet.ids(1, 2, 3))
 *     .readingOnlyBins("name", "age")
 *     .execute();
 * 
 * // Query with partition targeting
 * RecordStream results = session.query(customerDataSet)
 *     .onPartitionRange(0, 2048)
 *     .limit(1000)
 *     .execute();
 * }</pre>
 * 
 * @see Session#query(DataSet)
 * @see Session#query(Key)
 * @see Session#query(List)
 * @see RecordStream
 * @see SortDir
 * @see SortProperties
 */
public class QueryBuilder {
    private final QueryImpl implementation;
    private String[] binNames = null;
    private boolean withNoBins = false;
    private long limit = 0;
    private int pageSize = 0;
    private int startPartition = 0;
    private int endPartition = 4096;
    String dslString = null;
    private BooleanExpression dsl = null;
    private List<SortProperties> sortInfo = null;
    private Txn txnToUse;
    
    /**
     * Creates a QueryBuilder for querying an entire dataset.
     * 
     * <p>This constructor creates a query that will scan the entire dataset or use
     * secondary indexes if available. The query can be filtered, sorted, and paginated.</p>
     * 
     * @param session the session to use for the query
     * @param dataSet the dataset to query
     */
    public QueryBuilder(Session session, DataSet dataSet) {
        this.implementation = new IndexQueryBuilderImpl(this, session, dataSet);
        this.txnToUse = session.getCurrentTransaction();
    }
    
    /**
     * Creates a QueryBuilder for querying a single key.
     * 
     * <p>This constructor creates a query that will perform a direct key lookup.
     * The query will return at most one record.</p>
     * 
     * @param session the session to use for the query
     * @param key the key to query
     */
    public QueryBuilder(Session session, Key key) {
        this.implementation = new SingleKeyQueryBuilderImpl(this, session, key);
        this.txnToUse = session.getCurrentTransaction();
    }
    
    /**
     * Creates a QueryBuilder for querying multiple keys.
     * 
     * <p>This constructor creates a query that will perform batch key lookups.
     * If only one key is provided, it will use single key optimization.</p>
     * 
     * @param session the session to use for the query
     * @param keys the list of keys to query
     */
    public QueryBuilder(Session session, List<Key> keys) {
        this.txnToUse = session.getCurrentTransaction();
        if (keys.size() == 1) {
            this.implementation = new SingleKeyQueryBuilderImpl(this, session, keys.get(0));
        }
        else {
            this.implementation = new BatchKeyQueryBuilderImpl(this, session, keys);
        }
    }
    
    /**
     * Checks if a key falls within the current partition range.
     * 
     * <p>This method is used internally to filter keys based on the partition
     * range specified in the query. If no partition range is set (default 0-4096),
     * all keys are considered valid.</p>
     * 
     * @param key the key to check
     * @return true if the key is in the partition range, false otherwise
     */
    protected boolean isKeyInPartitionRange(Key key) {
        if (startPartition <= 0 && endPartition >= 4096) {
            return true;
        }
        int partId = Partition.getPartitionId(key.digest);
        return partId >= startPartition && partId < endPartition;
    }
    
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
    public QueryBuilder readingOnlyBins(String ... binNames) {
        this.binNames = binNames;
        if (this.withNoBins) {
            throw new IllegalArgumentException("Cannot specify both 'withNoBins' and provide a list of bin names");
        }
        return this;
    }
    
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
    public QueryBuilder withNoBins() {
        this.withNoBins = true;
        if (this.binNames != null) {
            throw new IllegalArgumentException("Cannot specify both 'withNoBins' and provide a list of bin names");
        }
        return this;
    }
    
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
    public QueryBuilder limit(long limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be > 0, not " + limit);
        }
        this.limit = limit;
        return this;
    }
    
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
    public QueryBuilder pageSize(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be > 0, not " + pageSize);
        }
        this.pageSize = pageSize;
        return this;
    }
    
    /**
     * Validates partition range parameters.
     * 
     * <p>This method performs sanity checks on partition range parameters to ensure
     * they are valid and consistent.</p>
     * 
     * @param startIncl the start partition (inclusive)
     * @param endExcl the end partition (exclusive)
     * @throws IllegalArgumentException if the partition range is invalid
     */
    private void sanityCheckPartitionRange(int startIncl, int endExcl) {
        if ((this.startPartition != 0 || this.endPartition != 4096) && 
                (this.startPartition != startIncl || this.endPartition != endExcl)) {
            
            throw new IllegalArgumentException(String.format(
                    "Partition range can only be defined once for a query. The partition range is currently "
                    + "[%,d->%,d) and is being set to [%,d->%,d)", 
                    startPartition, endPartition, startIncl, endExcl));
        }
        if (startIncl < 0 || startIncl >= 4096) {
            throw new IllegalArgumentException("Start partition must in the range of 0 to 4095, not " + startIncl);
        }
        if (endExcl < 1 || startIncl > 4096) {
            throw new IllegalArgumentException("End partition (exclusive) must in the range of 1 to 4096, not " + startIncl);
        }
        if (startIncl >= endExcl) {
            throw new IllegalArgumentException(String.format(
                    "Start partition must be less than the end partition. Specified start partition is %,d and end partition is %,d",
                    startIncl, endExcl));
        }
    }
    
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
    public QueryBuilder onPartition(int partId) {
        return onPartitionRange(partId, partId+1);
    }
    
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
    public QueryBuilder onPartitionRange(int startIncl, int endExcl) {
        sanityCheckPartitionRange(startIncl, endExcl);
        this.startPartition = startIncl;
        this.endPartition = endExcl;
        return this;
    }
    
    /**
     * Adds a sort field in ascending order with case sensitivity.
     * 
     * <p>This method adds a field to the sort criteria. Multiple sort fields can
     * be added, and they will be applied in the order they are added.</p>
     * 
     * @param field the field name to sort by
     * @return this QueryBuilder for method chaining
     */
    public QueryBuilder sortReturnedSubsetBy(String field) {
        return sortReturnedSubsetBy(field, SortDir.SORT_ASC, true);
    }
    
    /**
     * Adds a sort field in ascending order with specified case sensitivity.
     * 
     * @param field the field name to sort by
     * @param caseInsensitive true for case-insensitive sorting, false for case-sensitive
     * @return this QueryBuilder for method chaining
     */
    public QueryBuilder sortReturnedSubsetBy(String field, boolean caseInsensitive) {
        return sortReturnedSubsetBy(field, SortDir.SORT_ASC, caseInsensitive);
    }
    
    /**
     * Adds a sort field with specified direction and case sensitivity.
     * 
     * @param field the field name to sort by
     * @param sortDir the sort direction (ascending or descending)
     * @return this QueryBuilder for method chaining
     */
    public QueryBuilder sortReturnedSubsetBy(String field, SortDir sortDir) {
        return sortReturnedSubsetBy(field, sortDir, true);
    }

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
    public QueryBuilder sortReturnedSubsetBy(String field, SortDir sortDir, boolean caseSensitive) {
        if (sortDir == null) {
            sortDir = SortDir.SORT_ASC;
        }
        if (this.sortInfo == null) {
            this.sortInfo = new ArrayList<>();
        }
        this.sortInfo.add(new SortProperties(field, sortDir, caseSensitive));
        return this;
    }
    
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
    public QueryBuilder where(String dsl, Object ... params) {
        if (this.dslString != null && !this.dslString.equals(dsl)) {
            throw new IllegalArgumentException(String.format("different DSL strings have been provided in 'where' clauses. The first is \"%s\", the second is \"%s\"",
                    this.dslString, dsl));
        }
        if (this.dsl != null) {
            throw new IllegalArgumentException("A Dsl as a string and a DSL as an expression cannot both be specified.");
        }
        if (dsl == null || dsl.isEmpty()) {
            this.dslString = null;
        }
        else if (params.length == 0) {
            this.dslString = dsl;
        }
        else {
            this.dslString = String.format(dsl, params);
        }
        return this;
    }
    
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
    public QueryBuilder where(BooleanExpression dsl) {
        if (this.dslString != null || this.dsl != null) {
            throw new IllegalArgumentException("Multiple .where(...) conditions cannot be specified.");
        }
        this.dsl = dsl;
        return this;
    }
    
    /**
     * Gets the bin names to read.
     * 
     * @return the array of bin names, or null if not specified
     */
    public String[] getBinNames() {
        return this.binNames;
    }
    
    /**
     * Checks if the query should read no bins (header-only).
     * 
     * @return true if no bins should be read, false otherwise
     */
    public boolean getWithNoBins() {
        return this.withNoBins;
    }
    
    /**
     * Gets the query limit.
     * 
     * @return the maximum number of records to return, or 0 if not set
     */
    public long getLimit() {
        return limit;
    }
    
    /**
     * Gets the sort information.
     * 
     * @return the list of sort properties, or null if not specified
     */
    public List<SortProperties> getSortInfo() {
        return sortInfo;
    }
    
    /**
     * Gets the page size.
     * 
     * @return the page size, or 0 if not set
     */
    public int getPageSize() {
        return pageSize;
    }
    
    /**
     * Gets the start partition.
     * 
     * @return the start partition (inclusive)
     */
    public int getStartPartition() {
        return startPartition;
    }
    
    /**
     * Gets the end partition.
     * 
     * @return the end partition (exclusive)
     */
    public int getEndPartition() {
        return endPartition;
    }
    
    /**
     * Specifies that these operations are not to be included in any transaction, even if a
     * transaction exists on the underlying session.
     * 
     * <p>This method explicitly excludes the query from any active transaction,
     * ensuring it runs as a standalone operation.</p>
     * 
     * @return this QueryBuilder for method chaining
     */
    public QueryBuilder notInAnyTransaction() {
        this.txnToUse = null;
        return this;
    }
    
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
    public QueryBuilder inTransaction(Txn txn) {
        this.txnToUse = txn;
        return this;
    }
    
    /**
     * Gets the transaction to use for this query.
     * 
     * @return the transaction, or null if no transaction should be used
     */
    protected Txn getTxnToUse() {
        return this.txnToUse;
    }

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
    public RecordStream execute() {
        return this.implementation.execute();
    }
}
