package com.aerospike;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Consumer;

import com.aerospike.CdtGetOrRemoveBuilder.CdtOperation;
import com.aerospike.client.Bin;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.ListOrder;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.ExpOperation;
import com.aerospike.client.exp.ExpReadFlags;
import com.aerospike.client.exp.ExpWriteFlags;
import com.aerospike.dslobjects.DslExpression;
import com.aerospike.query.PreparedDsl;

public class BinBuilder<T extends AbstractOperationBuilder<T>> extends AbstractCdtBuilder<T> {
    public BinBuilder(T opBuilder, String binName) {
        super(opBuilder, binName, null);
    }
    
    /**
     * Create set database operation.
     */
    public T setTo(String value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public T setTo(int value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public T setTo(long value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public T setTo(float value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public T setTo(double value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public T setTo(boolean value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public T setTo(byte[] value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public T setTo(List<?> value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public T setTo(Map<?, ?> value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public T setTo(SortedMap<?, ?> value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create bin-remove operation.
     */
    public T remove() {
        return opBuilder.setTo(Bin.asNull(binName));
    }
    /**
     * Create set database operation.
     */
    public T get() {
        return opBuilder.get(binName);
    }
    /**
     * Create string append database operation.
     */
    public T append(String value) {
        return opBuilder.append(new Bin(binName, value));
    }
    /**
     * Create string prepend database operation.
     */
    public T prepend(String value) {
        return opBuilder.prepend(new Bin(binName, value));
    }
    /**
     * Create integer add database operation. If the record or bin does not exist, the
     * record/bin will be created by default with the value to be added.
     */
    public T add(int amount) {
        return opBuilder.add(new Bin(binName, amount));
    }
    /**
     * Create long add database operation. If the record or bin does not exist, the
     * record/bin will be created by default with the value to be added.
     */
    public T add(long amount) {
        return opBuilder.add(new Bin(binName, amount));
    }
    /**
     * Create float add database operation. If the record or bin does not exist, the
     * record/bin will be created by default with the value to be added.
     */
    public T add(float amount) {
        return opBuilder.add(new Bin(binName, amount));
    }
    /**
     * Create double add database operation. If the record or bin does not exist, the
     * record/bin will be created by default with the value to be added.
     */
    public T add(double amount) {
        return opBuilder.add(new Bin(binName, amount));
    }
    
    // ==================================================================
    // Expression Operations
    // ==================================================================
    
    /**
     * Read the result of evaluating a server-side expression into this bin.
     * 
     * <p>The expression is specified using the DSL string syntax. The result of
     * evaluating the expression will be returned in this bin's name.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * session.update(dataSet.id(1))
     *     .bin("result").readExp("$.a + $.b")
     *     .execute();
     * }</pre>
     * 
     * @param dsl the DSL expression string
     * @param params optional parameters for DSL string formatting
     * @return the parent operation builder for continued chaining
     */
    public T readExp(String dsl, Object... params) {
        Exp exp = opBuilder.parseExpression(dsl, params);
        return opBuilder.addOp(ExpOperation.read(binName, Exp.build(exp), ExpReadFlags.DEFAULT));
    }
    
    /**
     * Read the result of evaluating a server-side expression into this bin, with options.
     * 
     * <p>The expression is specified using the DSL string syntax. The options lambda
     * allows configuring behavior for error handling.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * session.update(dataSet.id(1))
     *     .bin("result").readExp("$.a + $.b", opts -> opts
     *         .returnNilForMissingBins()
     *         .ignoreExpressionErrors()
     *     )
     *     .execute();
     * }</pre>
     * 
     * @param dsl the DSL expression string
     * @param options lambda to configure read options
     * @return the parent operation builder for continued chaining
     */
    public T readExp(String dsl, Consumer<ExpReadOptions> options) {
        Exp exp = opBuilder.parseExpression(dsl);
        ExpReadOptions opts = new ExpReadOptions();
        options.accept(opts);
        return opBuilder.addOp(ExpOperation.read(binName, Exp.build(exp), opts.getFlags()));
    }
    
    /**
     * Read the result of evaluating a server-side expression into this bin.
     * 
     * <p>The expression is specified using a DslExpression object built programmatically.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * session.update(dataSet.id(1))
     *     .bin("isAdult").readExp(Bins.intBin("age").gt(18))
     *     .execute();
     * }</pre>
     * 
     * @param exp the DSL expression object
     * @return the parent operation builder for continued chaining
     */
    public T readExp(DslExpression exp) {
        return opBuilder.addOp(ExpOperation.read(binName, Exp.build(exp.toAerospikeExp()), ExpReadFlags.DEFAULT));
    }
    
    /**
     * Read the result of evaluating a server-side expression into this bin, with options.
     * 
     * <p>The expression is specified using a DslExpression object built programmatically.</p>
     * 
     * @param exp the DSL expression object
     * @param options lambda to configure read options
     * @return the parent operation builder for continued chaining
     */
    public T readExp(DslExpression exp, Consumer<ExpReadOptions> options) {
        ExpReadOptions opts = new ExpReadOptions();
        options.accept(opts);
        return opBuilder.addOp(ExpOperation.read(binName, Exp.build(exp.toAerospikeExp()), opts.getFlags()));
    }
    
    /**
     * Read the result of evaluating a server-side expression into this bin.
     * 
     * <p>The expression is specified using a PreparedDsl with parameters.</p>
     * 
     * @param dsl the prepared DSL
     * @param params the parameters for the prepared DSL
     * @return the parent operation builder for continued chaining
     */
    public T readExp(PreparedDsl dsl, Object... params) {
        Exp exp = opBuilder.parseExpression(dsl, params);
        return opBuilder.addOp(ExpOperation.read(binName, Exp.build(exp), ExpReadFlags.DEFAULT));
    }
    
    /**
     * Read the result of evaluating a server-side expression into this bin, with options.
     * 
     * <p>The expression is specified using a PreparedDsl with parameters.</p>
     * 
     * @param dsl the prepared DSL
     * @param options lambda to configure read options
     * @param params the parameters for the prepared DSL
     * @return the parent operation builder for continued chaining
     */
    public T readExp(PreparedDsl dsl, Consumer<ExpReadOptions> options, Object... params) {
        Exp exp = opBuilder.parseExpression(dsl, params);
        ExpReadOptions opts = new ExpReadOptions();
        options.accept(opts);
        return opBuilder.addOp(ExpOperation.read(binName, Exp.build(exp), opts.getFlags()));
    }
    
    /**
     * Read the result of evaluating a server-side expression into this bin.
     * 
     * <p>The expression is specified using a native Aerospike Exp object.</p>
     * 
     * @param exp the Aerospike expression
     * @return the parent operation builder for continued chaining
     */
    public T readExp(Exp exp) {
        return opBuilder.addOp(ExpOperation.read(binName, Exp.build(exp), ExpReadFlags.DEFAULT));
    }
    
    /**
     * Read the result of evaluating a server-side expression into this bin, with options.
     * 
     * <p>The expression is specified using a native Aerospike Exp object.</p>
     * 
     * @param exp the Aerospike expression
     * @param options lambda to configure read options
     * @return the parent operation builder for continued chaining
     */
    public T readExp(Exp exp, Consumer<ExpReadOptions> options) {
        ExpReadOptions opts = new ExpReadOptions();
        options.accept(opts);
        return opBuilder.addOp(ExpOperation.read(binName, Exp.build(exp), opts.getFlags()));
    }
    
    /**
     * Write the result of evaluating a server-side expression to this bin.
     * 
     * <p>The expression is specified using the DSL string syntax. The result of
     * evaluating the expression will be stored in this bin.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * session.update(dataSet.id(1))
     *     .bin("total").writeExp("$.price * $.quantity")
     *     .execute();
     * }</pre>
     * 
     * @param dsl the DSL expression string
     * @param params optional parameters for DSL string formatting
     * @return the parent operation builder for continued chaining
     */
    public T writeExp(String dsl, Object... params) {
        Exp exp = opBuilder.parseExpression(dsl, params);
        return opBuilder.addOp(ExpOperation.write(binName, Exp.build(exp), ExpWriteFlags.DEFAULT));
    }
    
    /**
     * Write the result of evaluating a server-side expression to this bin, with options.
     * 
     * <p>The expression is specified using the DSL string syntax. The options lambda
     * allows configuring behavior for error handling and bin existence policies.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * session.update(dataSet.id(1))
     *     .bin("computed").writeExp("$.a * 2", opts -> opts
     *         .onlyWhen(BinExistsPolicy.CREATE_ONLY)
     *         .ignoreExpressionErrors()
     *     )
     *     .execute();
     * }</pre>
     * 
     * @param dsl the DSL expression string
     * @param options lambda to configure write options
     * @return the parent operation builder for continued chaining
     */
    public T writeExp(String dsl, Consumer<ExpWriteOptions> options) {
        Exp exp = opBuilder.parseExpression(dsl);
        ExpWriteOptions opts = new ExpWriteOptions();
        options.accept(opts);
        return opBuilder.addOp(ExpOperation.write(binName, Exp.build(exp), opts.getFlags()));
    }
    
    /**
     * Write the result of evaluating a server-side expression to this bin.
     * 
     * <p>The expression is specified using a DslExpression object built programmatically.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * session.update(dataSet.id(1))
     *     .bin("status").writeExp(
     *         Bins.intBin("balance").lt(0).ifTrue("overdrawn").otherwise("ok")
     *     )
     *     .execute();
     * }</pre>
     * 
     * @param exp the DSL expression object
     * @return the parent operation builder for continued chaining
     */
    public T writeExp(DslExpression exp) {
        return opBuilder.addOp(ExpOperation.write(binName, Exp.build(exp.toAerospikeExp()), ExpWriteFlags.DEFAULT));
    }
    
    /**
     * Write the result of evaluating a server-side expression to this bin, with options.
     * 
     * <p>The expression is specified using a DslExpression object built programmatically.</p>
     * 
     * @param exp the DSL expression object
     * @param options lambda to configure write options
     * @return the parent operation builder for continued chaining
     */
    public T writeExp(DslExpression exp, Consumer<ExpWriteOptions> options) {
        ExpWriteOptions opts = new ExpWriteOptions();
        options.accept(opts);
        return opBuilder.addOp(ExpOperation.write(binName, Exp.build(exp.toAerospikeExp()), opts.getFlags()));
    }
    
    /**
     * Write the result of evaluating a server-side expression to this bin.
     * 
     * <p>The expression is specified using a PreparedDsl with parameters.</p>
     * 
     * @param dsl the prepared DSL
     * @param params the parameters for the prepared DSL
     * @return the parent operation builder for continued chaining
     */
    public T writeExp(PreparedDsl dsl, Object... params) {
        Exp exp = opBuilder.parseExpression(dsl, params);
        return opBuilder.addOp(ExpOperation.write(binName, Exp.build(exp), ExpWriteFlags.DEFAULT));
    }
    
    /**
     * Write the result of evaluating a server-side expression to this bin, with options.
     * 
     * <p>The expression is specified using a PreparedDsl with parameters.</p>
     * 
     * @param dsl the prepared DSL
     * @param options lambda to configure write options
     * @param params the parameters for the prepared DSL
     * @return the parent operation builder for continued chaining
     */
    public T writeExp(PreparedDsl dsl, Consumer<ExpWriteOptions> options, Object... params) {
        Exp exp = opBuilder.parseExpression(dsl, params);
        ExpWriteOptions opts = new ExpWriteOptions();
        options.accept(opts);
        return opBuilder.addOp(ExpOperation.write(binName, Exp.build(exp), opts.getFlags()));
    }
    
    /**
     * Write the result of evaluating a server-side expression to this bin.
     * 
     * <p>The expression is specified using a native Aerospike Exp object.</p>
     * 
     * @param exp the Aerospike expression
     * @return the parent operation builder for continued chaining
     */
    public T writeExp(Exp exp) {
        return opBuilder.addOp(ExpOperation.write(binName, Exp.build(exp), ExpWriteFlags.DEFAULT));
    }
    
    /**
     * Write the result of evaluating a server-side expression to this bin, with options.
     * 
     * <p>The expression is specified using a native Aerospike Exp object.</p>
     * 
     * @param exp the Aerospike expression
     * @param options lambda to configure write options
     * @return the parent operation builder for continued chaining
     */
    public T writeExp(Exp exp, Consumer<ExpWriteOptions> options) {
        ExpWriteOptions opts = new ExpWriteOptions();
        options.accept(opts);
        return opBuilder.addOp(ExpOperation.write(binName, Exp.build(exp), opts.getFlags()));
    }
    
    // ==================================================================
    // CDT Operations. Note: make sure to mirror these operations to 
    // CdtContextNonInvertableBuilder and CdtContextInvertableBuilder
    // ==================================================================
    public CdtContextNonInvertableBuilder<T> onMapIndex(int index) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_INDEX, index));
    }
    public CdtSetterNonInvertableBuilder<T> onMapKey(long key) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key)));
    }
    public CdtSetterNonInvertableBuilder<T> onMapKey(long key, MapOrder createType) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key), createType));
    }
    public CdtSetterNonInvertableBuilder<T> onMapKey(String key) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key)));
    }
    public CdtSetterNonInvertableBuilder<T> onMapKey(String key, MapOrder createType) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key), createType));
    }
    public CdtSetterNonInvertableBuilder<T> onMapKey(byte[] key) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key)));
    }
    public CdtSetterNonInvertableBuilder<T> onMapKey(byte[] key, MapOrder createType) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key), createType));
    }
    public CdtContextNonInvertableBuilder<T> onMapRank(int index) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_RANK, index));
    }
    public CdtContextInvertableBuilder<T> onMapValue(long value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder<T> onMapValue(String value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder<T> onMapValue(byte[] value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtActionInvertableBuilder<T> onMapKeyRange(String startIncl, String endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, Value.get(startIncl), Value.get(endExcl))); 
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(long startIncl, long endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    
    /**
     * Navigate to map items by key relative to index range.
     * Server selects map items nearest to key and greater by index.
     * 
     * @param key the reference key
     * @param index the relative index offset
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder<T> onMapKeyRelativeIndexRange(long key, int index) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index));
    }
    
    public CdtActionInvertableBuilder<T> onMapKeyRelativeIndexRange(String key, int index) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index));
    }
    
    public CdtActionInvertableBuilder<T> onMapKeyRelativeIndexRange(byte[] key, int index) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index));
    }
    
    /**
     * Navigate to map items by key relative to index range with count limit.
     * Server selects map items nearest to key and greater by index with a count limit.
     * 
     * @param key the reference key
     * @param index the relative index offset
     * @param count the maximum number of items to select
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder<T> onMapKeyRelativeIndexRange(long key, int index, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index, count));
    }
    
    public CdtActionInvertableBuilder<T> onMapKeyRelativeIndexRange(String key, int index, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index, count));
    }
    
    public CdtActionInvertableBuilder<T> onMapKeyRelativeIndexRange(byte[] key, int index, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index, count));
    }
    
    /**
     * Navigate to map items by value relative to rank range.
     * Server selects map items nearest to value and greater by relative rank.
     * 
     * @param value the reference value
     * @param rank the relative rank offset
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(long value, int rank) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(String value, int rank) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(byte[] value, int rank) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(double value, int rank) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(boolean value, int rank) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(List<?> value, int rank) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(Map<?,?> value, int rank) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(SpecialValue value, int rank) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, value.toAerospikeValue(), rank));
    }
    
    /**
     * Navigate to map items by value relative to rank range with count limit.
     * Server selects map items nearest to value and greater by relative rank with a count limit.
     * 
     * @param value the reference value
     * @param rank the relative rank offset
     * @param count the maximum number of items to select
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(long value, int rank, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank, count));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(String value, int rank, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank, count));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(byte[] value, int rank, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank, count));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(double value, int rank, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank, count));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(boolean value, int rank, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank, count));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(List<?> value, int rank, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank, count));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(Map<?,?> value, int rank, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank, count));
    }
    
    public CdtActionInvertableBuilder<T> onMapValueRelativeRankRange(SpecialValue value, int rank, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, value.toAerospikeValue(), rank, count));
    }

    /**
     * Navigate to map items by index range.
     * Server selects "count" map items starting at specified index.
     * 
     * @param index the starting index
     * @param count the number of items to select
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder<T> onMapIndexRange(int index, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_INDEX_RANGE, index, count));
    }
    
    /**
     * Navigate to map items by index range to end.
     * Server selects map items starting at specified index to the end of map.
     * 
     * @param index the starting index
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder<T> onMapIndexRange(int index) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_INDEX_RANGE, index));
    }
    
    /**
     * Navigate to map items by rank range.
     * Server selects "count" map items starting at specified rank.
     * 
     * @param rank the starting rank
     * @param count the number of items to select
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder<T> onMapRankRange(int rank, int count) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_RANK_RANGE, rank, count));
    }
    
    /**
     * Navigate to map items by rank range to end.
     * Server selects map items starting at specified rank to the end of map.
     * 
     * @param rank the starting rank
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder<T> onMapRankRange(int rank) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_RANK_RANGE, rank));
    }
    
    // Additional value type overloads
    public CdtContextInvertableBuilder<T> onMapValue(double value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder<T> onMapValue(boolean value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder<T> onMapValue(List<?> value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder<T> onMapValue(Map<?,?> value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder<T> onMapValue(SpecialValue value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, value.toAerospikeValue()));
    }
    
    // Additional range type overloads
    public CdtActionInvertableBuilder<T> onMapKeyRange(byte[] startIncl, byte[] endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, Value.get(startIncl), Value.get(endExcl))); 
    }
    public CdtActionInvertableBuilder<T> onMapKeyRange(double startIncl, double endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, Value.get(startIncl), Value.get(endExcl))); 
    }
    // SpecialValue combinations for onMapKeyRange
    public CdtActionInvertableBuilder<T> onMapKeyRange(SpecialValue startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, startIncl.toAerospikeValue(), endExcl.toAerospikeValue())); 
    }
    public CdtActionInvertableBuilder<T> onMapKeyRange(SpecialValue startIncl, long endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl))); 
    }
    public CdtActionInvertableBuilder<T> onMapKeyRange(SpecialValue startIncl, String endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl))); 
    }
    public CdtActionInvertableBuilder<T> onMapKeyRange(SpecialValue startIncl, byte[] endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl))); 
    }
    public CdtActionInvertableBuilder<T> onMapKeyRange(SpecialValue startIncl, double endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl))); 
    }
    public CdtActionInvertableBuilder<T> onMapKeyRange(long startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, Value.get(startIncl), endExcl.toAerospikeValue())); 
    }
    public CdtActionInvertableBuilder<T> onMapKeyRange(String startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, Value.get(startIncl), endExcl.toAerospikeValue())); 
    }
    public CdtActionInvertableBuilder<T> onMapKeyRange(byte[] startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, Value.get(startIncl), endExcl.toAerospikeValue())); 
    }
    public CdtActionInvertableBuilder<T> onMapKeyRange(double startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, Value.get(startIncl), endExcl.toAerospikeValue())); 
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(String startIncl, String endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(byte[] startIncl, byte[] endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(double startIncl, double endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(boolean startIncl, boolean endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(List<?> startIncl, List<?> endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(Map<?,?> startIncl, Map<?,?> endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    // SpecialValue combinations for onMapValueRange
    public CdtActionInvertableBuilder<T> onMapValueRange(SpecialValue startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, startIncl.toAerospikeValue(), endExcl.toAerospikeValue()));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(SpecialValue startIncl, long endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(SpecialValue startIncl, String endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(SpecialValue startIncl, byte[] endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(SpecialValue startIncl, double endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(SpecialValue startIncl, boolean endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(SpecialValue startIncl, List<?> endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(SpecialValue startIncl, Map<?,?> endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, startIncl.toAerospikeValue(), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(long startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), endExcl.toAerospikeValue()));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(String startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), endExcl.toAerospikeValue()));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(byte[] startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), endExcl.toAerospikeValue()));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(double startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), endExcl.toAerospikeValue()));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(boolean startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), endExcl.toAerospikeValue()));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(List<?> startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), endExcl.toAerospikeValue()));
    }
    public CdtActionInvertableBuilder<T> onMapValueRange(Map<?,?> startIncl, SpecialValue endExcl) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), endExcl.toAerospikeValue()));
    }

    public CdtContextNonInvertableBuilder<T> onListIndex(int index) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_INDEX, index));
    }
    public CdtContextNonInvertableBuilder<T> onListIndex(int index, ListOrder order, boolean pad) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_INDEX, index, order, pad));
    }
    public CdtContextNonInvertableBuilder<T> onListRank(int index) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_RANK, index));
    }
    public CdtContextInvertableBuilder<T> onListValue(long value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder<T> onListValue(String value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder<T> onListValue(byte[] value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder<T> onListValue(SpecialValue value) {
        return new CdtGetOrRemoveBuilder<>(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_VALUE, value.toAerospikeValue()));
    }
    
    /**
     * Navigate to map items by a list of keys.
     * 
     * @param keys the list of keys to match
     * @return builder for continued chaining
     */
    public CdtContextInvertableBuilder<T> onMapKeyList(List<?> keys) {
        List<Value> valueList = new ArrayList<>();
        for (Object key : keys) {
            valueList.add(Value.get(key));
        }
        CdtOperationParams params = new CdtOperationParams(CdtOperation.MAP_BY_KEY_LIST, valueList);
        return new CdtGetOrRemoveBuilder<>(this.binName, this.opBuilder, params);
    }
    
    /**
     * Navigate to map items by a list of values.
     * 
     * @param values the list of values to match
     * @return builder for continued chaining
     */
    public CdtContextInvertableBuilder<T> onMapValueList(List<?> values) {
        List<Value> valueList = new ArrayList<>();
        for (Object value : values) {
            valueList.add(Value.get(value));
        }
        CdtOperationParams params = new CdtOperationParams(CdtOperation.MAP_BY_VALUE_LIST, valueList);
        return new CdtGetOrRemoveBuilder<>(this.binName, this.opBuilder, params);
    }

}