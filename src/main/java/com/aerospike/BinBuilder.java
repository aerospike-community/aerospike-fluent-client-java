package com.aerospike;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.aerospike.CdtGetOrRemoveBuilder.CdtOperation;
import com.aerospike.client.Bin;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.ListOrder;
import com.aerospike.client.cdt.MapOrder;

public class BinBuilder extends AbstractCdtBuilder{
    public BinBuilder(OperationBuilder opBuilder, String binName) {
        super(opBuilder, binName, null);
    }
    
    /**
     * Create set database operation.
     */
    public OperationBuilder setTo(String value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public OperationBuilder setTo(int value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public OperationBuilder setTo(long value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public OperationBuilder setTo(float value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public OperationBuilder setTo(double value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public OperationBuilder setTo(boolean value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public OperationBuilder setTo(byte[] value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public OperationBuilder setTo(List<?> value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public OperationBuilder setTo(Map<?, ?> value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create set database operation.
     */
    public OperationBuilder setTo(SortedMap<?, ?> value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    /**
     * Create bin-remove operation.
     */
    public OperationBuilder remove() {
        return opBuilder.setTo(Bin.asNull(binName));
    }
    /**
     * Create set database operation.
     */
    public OperationBuilder get() {
        return opBuilder.get(binName);
    }
    /**
     * Create string append database operation.
     */
    public OperationBuilder append(String value) {
        return opBuilder.append(new Bin(binName, value));
    }
    /**
     * Create string prepend database operation.
     */
    public OperationBuilder prepend(String value) {
        return opBuilder.prepend(new Bin(binName, value));
    }
    /**
     * Create integer add database operation. If the record or bin does not exist, the
     * record/bin will be created by default with the value to be added.
     */
    public OperationBuilder add(int amount) {
        return opBuilder.add(new Bin(binName, amount));
    }
    /**
     * Create long add database operation. If the record or bin does not exist, the
     * record/bin will be created by default with the value to be added.
     */
    public OperationBuilder add(long amount) {
        return opBuilder.add(new Bin(binName, amount));
    }
    /**
     * Create float add database operation. If the record or bin does not exist, the
     * record/bin will be created by default with the value to be added.
     */
    public OperationBuilder add(float amount) {
        return opBuilder.add(new Bin(binName, amount));
    }
    /**
     * Create double add database operation. If the record or bin does not exist, the
     * record/bin will be created by default with the value to be added.
     */
    public OperationBuilder add(double amount) {
        return opBuilder.add(new Bin(binName, amount));
    }
    
    // ==================================================================
    // CDT Operations. Note: make sure to mirror these operations to 
    // CdtContextNonInvertableBuilder and CdtContextInvertableBuilder
    // ==================================================================
    public CdtContextNonInvertableBuilder onMapIndex(int index) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_INDEX, index));
    }
    public CdtSetterNonInvertableBuilder onMapKey(long key) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key)));
    }
    public CdtSetterNonInvertableBuilder onMapKey(long key, MapOrder createType) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key), createType));
    }
    public CdtSetterNonInvertableBuilder onMapKey(String key) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key)));
    }
    public CdtSetterNonInvertableBuilder onMapKey(String key, MapOrder createType) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key), createType));
    }
    public CdtSetterNonInvertableBuilder onMapKey(byte[] key) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key)));
    }
    public CdtSetterNonInvertableBuilder onMapKey(byte[] key, MapOrder createType) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY, Value.get(key), createType));
    }
    public CdtContextNonInvertableBuilder onMapRank(int index) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_RANK, index));
    }
    public CdtContextInvertableBuilder onMapValue(long value) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder onMapValue(String value) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder onMapValue(byte[] value) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtActionInvertableBuilder onMapKeyRange(String startIncl, String endExcl) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, Value.get(startIncl), Value.get(endExcl))); 
    }
    public CdtActionInvertableBuilder onMapValueRange(long startIncl, long endExcl) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    
    /**
     * Navigate to map items by key relative to index range.
     * Server selects map items nearest to key and greater by index.
     * 
     * @param key the reference key
     * @param index the relative index offset
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(long key, int index) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index));
    }
    
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(String key, int index) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index));
    }
    
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(byte[] key, int index) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index));
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
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(long key, int index, int count) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index, count));
    }
    
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(String key, int index, int count) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index, count));
    }
    
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(byte[] key, int index, int count) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_REL_INDEX_RANGE, Value.get(key), index, count));
    }
    
    /**
     * Navigate to map items by value relative to rank range.
     * Server selects map items nearest to value and greater by relative rank.
     * 
     * @param value the reference value
     * @param rank the relative rank offset
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(long value, int rank) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank));
    }
    
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(String value, int rank) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank));
    }
    
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(byte[] value, int rank) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank));
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
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(long value, int rank, int count) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank, count));
    }
    
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(String value, int rank, int count) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank, count));
    }
    
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(byte[] value, int rank, int count) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_REL_RANK_RANGE, Value.get(value), rank, count));
    }

    /**
     * Navigate to map items by index range.
     * Server selects "count" map items starting at specified index.
     * 
     * @param index the starting index
     * @param count the number of items to select
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder onMapIndexRange(int index, int count) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_INDEX_RANGE, index, count));
    }
    
    /**
     * Navigate to map items by index range to end.
     * Server selects map items starting at specified index to the end of map.
     * 
     * @param index the starting index
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder onMapIndexRange(int index) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_INDEX_RANGE, index));
    }
    
    /**
     * Navigate to map items by rank range.
     * Server selects "count" map items starting at specified rank.
     * 
     * @param rank the starting rank
     * @param count the number of items to select
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder onMapRankRange(int rank, int count) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_RANK_RANGE, rank, count));
    }
    
    /**
     * Navigate to map items by rank range to end.
     * Server selects map items starting at specified rank to the end of map.
     * 
     * @param rank the starting rank
     * @return builder for continued chaining (invertable for range operations)
     */
    public CdtActionInvertableBuilder onMapRankRange(int rank) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_RANK_RANGE, rank));
    }
    
    // Additional value type overloads
    public CdtContextInvertableBuilder onMapValue(double value) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder onMapValue(boolean value) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder onMapValue(List<?> value) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder onMapValue(Map<?,?> value) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE, Value.get(value)));
    }
    
    // Additional range type overloads
    public CdtActionInvertableBuilder onMapKeyRange(byte[] startIncl, byte[] endExcl) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, Value.get(startIncl), Value.get(endExcl))); 
    }
    public CdtActionInvertableBuilder onMapKeyRange(double startIncl, double endExcl) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_KEY_RANGE, Value.get(startIncl), Value.get(endExcl))); 
    }
    public CdtActionInvertableBuilder onMapValueRange(String startIncl, String endExcl) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder onMapValueRange(byte[] startIncl, byte[] endExcl) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder onMapValueRange(double startIncl, double endExcl) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }
    public CdtActionInvertableBuilder onMapValueRange(boolean startIncl, boolean endExcl) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.MAP_BY_VALUE_RANGE, Value.get(startIncl), Value.get(endExcl)));
    }

    public CdtContextNonInvertableBuilder onListIndex(int index) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_INDEX, index));
    }
    public CdtContextNonInvertableBuilder onListIndex(int index, ListOrder order, boolean pad) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_INDEX, index, order, pad));
    }
    public CdtContextNonInvertableBuilder onListRank(int index) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_RANK, index));
    }
    public CdtContextInvertableBuilder onListValue(long value) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder onListValue(String value) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_VALUE, Value.get(value)));
    }
    public CdtContextInvertableBuilder onListValue(byte[] value) {
        return new CdtGetOrRemoveBuilder(binName, opBuilder, new CdtOperationParams(CdtOperation.LIST_BY_VALUE, Value.get(value)));
    }

}