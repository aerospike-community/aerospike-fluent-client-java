package com.aerospike;

import com.aerospike.client.exp.ExpWriteFlags;

/**
 * Options builder for expression write operations.
 * 
 * <p>This class provides a fluent API for configuring flags that control how
 * expression write operations behave. Expression writes evaluate a server-side
 * expression and store the result in a named bin.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * session.update(dataSet.id(1))
 *     .bin("computed").writeExp("$.a * 2", opts -> opts
 *         .onlyWhen(BinExistsPolicy.CREATE_ONLY)
 *         .ignoreExpressionErrors()
 *     )
 *     .execute();
 * }</pre>
 * 
 * <p>All flags are combined using bitwise OR and passed to the underlying
 * {@link com.aerospike.client.exp.ExpOperation#write} method.</p>
 * 
 * @see com.aerospike.client.exp.ExpOperation
 * @see com.aerospike.client.exp.ExpWriteFlags
 */
public class ExpWriteOptions {
    
    /**
     * Enum for mutually exclusive bin existence policies.
     * 
     * <p>These policies control whether an expression write operation succeeds
     * based on whether the target bin already exists. Only one policy can be
     * active at a time.</p>
     */
    public enum BinExistsPolicy {
        /**
         * Write regardless of whether the bin exists (default behavior).
         * The write will always proceed, creating the bin if it doesn't exist
         * or updating it if it does.
         */
        ALWAYS(0),
        
        /**
         * Only write if the bin does NOT exist; fail if the bin already exists.
         * Use this to ensure you're creating a new bin rather than overwriting
         * an existing one.
         */
        CREATE_ONLY(ExpWriteFlags.CREATE_ONLY),
        
        /**
         * Only write if the bin already exists; fail if the bin doesn't exist.
         * Use this to ensure you're updating an existing bin rather than
         * accidentally creating a new one.
         */
        UPDATE_ONLY(ExpWriteFlags.UPDATE_ONLY);
        
        private final int flag;
        
        BinExistsPolicy(int flag) {
            this.flag = flag;
        }
        
        /**
         * Returns the flag value for this policy.
         * @return the ExpWriteFlags bit value
         */
        public int getFlag() {
            return flag;
        }
    }
    
    private int flags = ExpWriteFlags.DEFAULT;
    private BinExistsPolicy binExistsPolicy = BinExistsPolicy.ALWAYS;
    
    /**
     * Creates a new ExpWriteOptions with default flags.
     */
    public ExpWriteOptions() {
    }
    
    /**
     * Controls whether the write succeeds based on bin existence.
     * 
     * <p>This method sets the bin existence policy for the write operation.
     * Only one policy can be active - calling this method multiple times
     * will replace the previous policy.</p>
     * 
     * <p><b>Policies:</b></p>
     * <ul>
     *   <li>{@link BinExistsPolicy#ALWAYS} - Write regardless of bin existence (default)</li>
     *   <li>{@link BinExistsPolicy#CREATE_ONLY} - Only write if bin does NOT exist</li>
     *   <li>{@link BinExistsPolicy#UPDATE_ONLY} - Only write if bin already exists</li>
     * </ul>
     * 
     * <p>Maps to {@link ExpWriteFlags#CREATE_ONLY} or {@link ExpWriteFlags#UPDATE_ONLY}</p>
     * 
     * @param policy the bin existence policy to apply
     * @return this options builder for method chaining
     */
    public ExpWriteOptions onlyWhen(BinExistsPolicy policy) {
        this.binExistsPolicy = policy;
        return this;
    }
    
    /**
     * If expression evaluates to nil, delete the bin instead of failing.
     * 
     * <p>By default, if an expression evaluates to nil (null), the operation
     * will fail. With this flag, a nil result will instead delete the bin
     * from the record, which can be useful for conditional cleanup operations.</p>
     * 
     * <p>Maps to {@link ExpWriteFlags#ALLOW_DELETE}</p>
     * 
     * @return this options builder for method chaining
     */
    public ExpWriteOptions allowBinDeletion() {
        this.flags |= ExpWriteFlags.ALLOW_DELETE;
        return this;
    }
    
    /**
     * If the bin existence policy (CREATE_ONLY/UPDATE_ONLY) would fail,
     * silently succeed instead.
     * 
     * <p>This flag is useful when you want to attempt a conditional write
     * but don't want the entire operation to fail if the condition isn't met.
     * The write simply won't happen, but the operation succeeds.</p>
     * 
     * <p>Maps to {@link ExpWriteFlags#POLICY_NO_FAIL}</p>
     * 
     * @return this options builder for method chaining
     */
    public ExpWriteOptions ignorePolicyErrors() {
        this.flags |= ExpWriteFlags.POLICY_NO_FAIL;
        return this;
    }
    
    /**
     * If expression evaluation fails, silently succeed instead of raising error.
     * 
     * <p>This is useful when the expression might fail due to type mismatches,
     * division by zero, missing bins, or other runtime errors, and you want to
     * handle these cases gracefully by having the write simply not occur rather
     * than failing the entire operation.</p>
     * 
     * <p>Maps to {@link ExpWriteFlags#EVAL_NO_FAIL}</p>
     * 
     * @return this options builder for method chaining
     */
    public ExpWriteOptions ignoreExpressionErrors() {
        this.flags |= ExpWriteFlags.EVAL_NO_FAIL;
        return this;
    }
    
    /**
     * Returns the combined flags value for use with ExpOperation.write().
     * 
     * <p>This method combines the bin existence policy with any other flags
     * that have been set.</p>
     * 
     * @return the bitwise OR of all configured flags
     */
    public int getFlags() {
        return flags | binExistsPolicy.getFlag();
    }
}

