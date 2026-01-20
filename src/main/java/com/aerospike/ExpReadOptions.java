package com.aerospike;

import com.aerospike.client.exp.ExpReadFlags;

/**
 * Options builder for expression read operations.
 * 
 * <p>This class provides a fluent API for configuring flags that control how
 * expression read operations behave. Expression reads evaluate a server-side
 * expression and return the result in a named bin.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * session.update(dataSet.id(1))
 *     .bin("result").readExp("$.a + $.b", opts -> opts
 *         .returnNilForMissingBins()
 *         .ignoreExpressionErrors()
 *     )
 *     .execute();
 * }</pre>
 * 
 * <p>All flags are combined using bitwise OR and passed to the underlying
 * {@link com.aerospike.client.exp.ExpOperation#read} method.</p>
 * 
 * @see com.aerospike.client.exp.ExpOperation
 * @see com.aerospike.client.exp.ExpReadFlags
 */
public class ExpReadOptions {
    private int flags = ExpReadFlags.DEFAULT;
    
    /**
     * Creates a new ExpReadOptions with default flags.
     */
    public ExpReadOptions() {
    }
    
    /**
     * If expression evaluation fails, return nil instead of raising an error.
     * 
     * <p>This is useful when the expression might fail due to type mismatches,
     * division by zero, or other runtime errors, and you want to handle these
     * cases gracefully by receiving nil rather than an exception.</p>
     * 
     * <p>Maps to {@link ExpReadFlags#EVAL_NO_FAIL}</p>
     * 
     * @return this options builder for method chaining
     */
    public ExpReadOptions ignoreExpressionErrors() {
        this.flags |= ExpReadFlags.EVAL_NO_FAIL;
        return this;
    }
    
    /**
     * If a bin referenced in the expression doesn't exist, return nil instead of error.
     * 
     * <p>By default, if an expression references a bin that doesn't exist on the record,
     * the operation will fail. This flag allows the operation to succeed and return nil
     * instead, which is useful when working with records that may have optional fields.</p>
     * 
     * <p>Maps to a combination of flags that handle unknown bin scenarios.</p>
     * 
     * @return this options builder for method chaining
     */
    public ExpReadOptions returnNilForMissingBins() {
        // EVAL_NO_FAIL handles unknown bin cases during expression evaluation
        this.flags |= ExpReadFlags.EVAL_NO_FAIL;
        return this;
    }
    
    /**
     * Returns the combined flags value for use with ExpOperation.read().
     * 
     * @return the bitwise OR of all configured flags
     */
    public int getFlags() {
        return flags;
    }
}

