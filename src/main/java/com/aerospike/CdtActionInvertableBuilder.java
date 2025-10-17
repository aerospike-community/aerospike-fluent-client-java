package com.aerospike;

/**
 * This interface allows actions at the end of a CDT path but includes only the actions
 * which are valid to pass INVERTED to. This excludes items which return one result -
 * MapOperation.getByKey, mapOperation.getByIndex, mapOperation.getByRank. These operations 
 * will throw a ParameterError if you try to invoke them with the INVERTED flag. This
 * interface is only returned after a context call which selects multiple elements.
 * <p>
 * These methods use the INVERTED flag to return all elements EXCEPT those selected:
 * <ul>
 *   <li>VALUE | INVERTED → getAllOtherValues()</li>
 *   <li>KEY | INVERTED → getAllOtherKeys()</li>
 *   <li>COUNT | INVERTED → countAllOthers()</li>
 *   <li>INDEX | INVERTED → getAllOtherIndexes()</li>
 *   <li>REVERSE_INDEX | INVERTED → getAllOtherReverseIndexes()</li>
 *   <li>RANK | INVERTED → getAllOtherRanks()</li>
 *   <li>REVERSE_RANK | INVERTED → getAllOtherReverseRanks()</li>
 *   <li>KEY_VALUE | INVERTED → getAllOtherKeysAndValues()</li>
 *   <li>INVERTED → removeAllOthers()</li>
 * </ul>
 */
public interface CdtActionInvertableBuilder extends CdtActionNonInvertableBuilder {
    public OperationBuilder getAllOtherValues();
    public OperationBuilder getAllOtherKeys();
    public OperationBuilder countAllOthers();
    public OperationBuilder getAllOtherIndexes();
    public OperationBuilder getAllOtherReverseIndexes();
    public OperationBuilder getAllOtherRanks();
    public OperationBuilder getAllOtherReverseRanks();
    public OperationBuilder getAllOtherKeysAndValues();
    public OperationBuilder removeAllOthers();

}