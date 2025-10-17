package com.aerospike;

/**
 * This interface defines the operations available at the end of a CDT path, other than
 * the set(). There are two fundamental operations, get() and remove() but there are 
 * different varieties of these based on the return type (eg count() and getValue() are
 * both get() operations.
 * <p>
 * These methods correspond to MapReturnType values:
 * <ul>
 *   <li>VALUE → getValues()</li>
 *   <li>KEY → getKeys()</li>
 *   <li>COUNT → count()</li>
 *   <li>INDEX → getIndex()</li>
 *   <li>REVERSE_INDEX → getReverseIndex()</li>
 *   <li>RANK → getRank()</li>
 *   <li>REVERSE_RANK → getReverseRank()</li>
 *   <li>KEY_VALUE → getKeyAndValue()</li>
 *   <li>NONE → remove()</li>
 * </ul>
 */
public interface CdtActionNonInvertableBuilder {
    public OperationBuilder getValues();
    public OperationBuilder getKeys();
    public OperationBuilder count();
    public OperationBuilder getIndex();
    public OperationBuilder getReverseIndex();
    public OperationBuilder getRank();
    public OperationBuilder getReverseRank();
    public OperationBuilder getKeyAndValue();
    public OperationBuilder remove();
}
