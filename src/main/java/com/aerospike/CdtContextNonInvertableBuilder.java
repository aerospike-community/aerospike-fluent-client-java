package com.aerospike;

import java.util.List;
import java.util.Map;

import com.aerospike.client.cdt.ListOrder;
import com.aerospike.client.cdt.MapOrder;

/**
 * This interface handles operations at the end of contexts. Note that some of these methods
 * like onMapValueRangemust be at the end of a context and hence must be followed by an action 
 * (CdtAction* return types), others (like onMapIndex) are context items which can be followed 
 * either by other context paths or by an action (CdtContext* return types), and onMapKey which
 * can be followed a context path, an action (get or remove) or can be used to set the value, and
 * hence returns a CdtSetter* method.
 * <p/>
 * Note that some methods are invertable (ie can support the INVERTED flag) and others aren't.
 * For example, onMapIndex returns a single value, hence cannot support the INVERTED flag. 
 * onMapValue returns a list of values and hence can be inverted.
 * <p/>
 * Note that this is a paired interface with {@link CdtContextInvertableBuilder} and they have exactly
 * the same methods, differing only in the interface they extend.
 */
public interface CdtContextNonInvertableBuilder extends CdtActionNonInvertableBuilder {
    // Map index
    public CdtContextNonInvertableBuilder onMapIndex(int index);
    
    // Map index range operations
    public CdtActionInvertableBuilder onMapIndexRange(int index, int count);
    public CdtActionInvertableBuilder onMapIndexRange(int index);

    // Map key operations
    public CdtSetterNonInvertableBuilder onMapKey(long key);
    public CdtSetterNonInvertableBuilder onMapKey(String key);
    public CdtSetterNonInvertableBuilder onMapKey(byte[] key);
    public CdtSetterNonInvertableBuilder onMapKey(long key, MapOrder createType);
    public CdtSetterNonInvertableBuilder onMapKey(String key, MapOrder createType);
    public CdtSetterNonInvertableBuilder onMapKey(byte[] key, MapOrder createType);

    // Map key range operations
    public CdtActionInvertableBuilder onMapKeyRange(String startIncl, String endExcl);
    public CdtActionInvertableBuilder onMapKeyRange(byte[] startIncl, byte[] endExcl);
    public CdtActionInvertableBuilder onMapKeyRange(double startIncl, double endExcl);

    // Map key relative rank range
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(long key, int index);
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(String key, int index);
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(byte[] key, int index);
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(long key, int index, int count);
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(String key, int index, int count);
    public CdtActionInvertableBuilder onMapKeyRelativeIndexRange(byte[] key, int index, int count);

    // Map rank
    public CdtContextNonInvertableBuilder onMapRank(int index);

    // Map rank range operations
    public CdtActionInvertableBuilder onMapRankRange(int rank, int count);
    public CdtActionInvertableBuilder onMapRankRange(int rank);

    // Map value operations
    public CdtContextInvertableBuilder onMapValue(long value);
    public CdtContextInvertableBuilder onMapValue(String value);
    public CdtContextInvertableBuilder onMapValue(byte[] value);
    public CdtContextInvertableBuilder onMapValue(double value);
    public CdtContextInvertableBuilder onMapValue(boolean value);
    public CdtContextInvertableBuilder onMapValue(List<?> value);
    public CdtContextInvertableBuilder onMapValue(Map<?,?> value);
    
    // Map value range
    public CdtActionInvertableBuilder onMapValueRange(long startIncl, long endExcl);
    public CdtActionInvertableBuilder onMapValueRange(String startIncl, String endExcl);
    public CdtActionInvertableBuilder onMapValueRange(byte[] startIncl, byte[] endExcl);
    public CdtActionInvertableBuilder onMapValueRange(double startIncl, double endExcl);
    public CdtActionInvertableBuilder onMapValueRange(boolean startIncl, boolean endExcl);

    // Map value relative rank range
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(long value, int rank);
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(String value, int rank);
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(byte[] value, int rank);
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(long value, int rank, int count);
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(String value, int rank, int count);
    public CdtActionInvertableBuilder onMapValueRelativeRankRange(byte[] value, int rank, int count);



    public CdtContextNonInvertableBuilder onListIndex(int index);
    public CdtContextNonInvertableBuilder onListIndex(int index, ListOrder order, boolean pad);
    public CdtContextNonInvertableBuilder onListRank(int index);
    public CdtContextInvertableBuilder onListValue(long value);
    public CdtContextInvertableBuilder onListValue(String value);
    public CdtContextInvertableBuilder onListValue(byte[] value);

    public OperationBuilder mapClear();
    public OperationBuilder mapSize();
    
    /** Append an item to the end of an unordered list */
    public OperationBuilder listAppend(long value);
    /** Append an item to the end of an unordered list */
    public OperationBuilder listAppend(String value);
    /** Append an item to the end of an unordered list */
    public OperationBuilder listAppend(double value);
    /** Append an item to the end of an unordered list */
    public OperationBuilder listAppend(boolean value);
    /** Append an item to the end of an unordered list */
    public OperationBuilder listAppend(byte[] value);
    /** Append an item to the end of an unordered list */
    public OperationBuilder listAppend(List<?> value);
    /** Append an item to the end of an unordered list */
    public OperationBuilder listAppend(Map<?,?> value);
    /** Append an item to the end of an unordered list with unique items, allowing for failures */
    public OperationBuilder listAppendUnique(long value, boolean allowFailures);
    /** Append an item to the end of an unordered list with unique items, allowing for failures */
    public OperationBuilder listAppendUnique(String value, boolean allowFailures);
    /** Append an item to the end of an unordered list with unique items, allowing for failures */
    public OperationBuilder listAppendUnique(double value, boolean allowFailures);
    /** Append an item to the end of an unordered list with unique items, allowing for failures */
    public OperationBuilder listAppendUnique(boolean value, boolean allowFailures);
    /** Append an item to the end of an unordered list with unique items, allowing for failures */
    public OperationBuilder listAppendUnique(byte[] value, boolean allowFailures);
    /** Append an item to the end of an unordered list with unique items, allowing for failures */
    public OperationBuilder listAppendUnique(List<?> value, boolean allowFailures);
    /** Append an item to the end of an unordered list with unique items, allowing for failures */
    public OperationBuilder listAppendUnique(Map<?,?> value, boolean allowFailures);
    
    /** Add an item to the appropriate spot in an ordered list */
    public OperationBuilder listAdd(long value);
    /** Add an item to the appropriate spot in an ordered list */
    public OperationBuilder listAdd(String value);
    /** Add an item to the appropriate spot in an ordered list */
    public OperationBuilder listAdd(double value);
    /** Add an item to the appropriate spot in an ordered list */
    public OperationBuilder listAdd(boolean value);
    /** Add an item to the appropriate spot in an ordered list */
    public OperationBuilder listAdd(byte[] value);
    /** Add an item to the appropriate spot in an ordered list */
    public OperationBuilder listAdd(List<?> value);
    /** Add an item to the appropriate spot in an ordered list */
    public OperationBuilder listAdd(Map<?,?> value);

    /** Add an item to the appropriate spot in an ordered list. If the item is not unique 
     * either an exception will be thrown or the error will be silently ignored, based on allowFailures */
    public OperationBuilder listAddUnique(long value, boolean allowFailures);
    /** Add an item to the appropriate spot in an ordered list. If the item is not unique 
     * either an exception will be thrown or the error will be silently ignored, based on allowFailures */
    public OperationBuilder listAddUnique(String value, boolean allowFailures);
    /** Add an item to the appropriate spot in an ordered list. If the item is not unique 
     * either an exception will be thrown or the error will be silently ignored, based on allowFailures */
    public OperationBuilder listAddUnique(double value, boolean allowFailures);
    /** Add an item to the appropriate spot in an ordered list. If the item is not unique 
     * either an exception will be thrown or the error will be silently ignored, based on allowFailures */
    public OperationBuilder listAddUnique(boolean value, boolean allowFailures);
    /** Add an item to the appropriate spot in an ordered list. If the item is not unique 
     * either an exception will be thrown or the error will be silently ignored, based on allowFailures */
    public OperationBuilder listAddUnique(byte[] value, boolean allowFailures);
    /** Add an item to the appropriate spot in an ordered list. If the item is not unique 
     * either an exception will be thrown or the error will be silently ignored, based on allowFailures */
    public OperationBuilder listAddUnique(List<?> value, boolean allowFailures);
    /** Add an item to the appropriate spot in an ordered list. If the item is not unique 
     * either an exception will be thrown or the error will be silently ignored, based on allowFailures */
    public OperationBuilder listAddUnique(Map<?,?> value, boolean allowFailures);

}