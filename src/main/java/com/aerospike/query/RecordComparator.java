package com.aerospike.query;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.aerospike.AerospikeComparator;
import com.aerospike.RecordResult;
import com.aerospike.client.query.KeyRecord;

public class RecordComparator implements Comparator<RecordResult>{
    private final List<SortProperties> sortPropertiesList;
    private static final AerospikeComparator aerospikeComparatorCaseSensitive = new AerospikeComparator(true);
    private static final AerospikeComparator aerospikeComparatorCaseInsensitive = new AerospikeComparator(false);;
    
    public RecordComparator(List<SortProperties> sortProperties) {
        this.sortPropertiesList = sortProperties;
    }
    
    private AerospikeComparator getComparator(boolean caseSensitive) {
        if (caseSensitive) {
            return aerospikeComparatorCaseSensitive;
        }
        else {
            return aerospikeComparatorCaseInsensitive;
        }
    }

    private int compare(int index, Map<String, Object> map1, Map<String, Object> map2) {
        if (index >= sortPropertiesList.size()) {
            // Arbitrary sort
            return -1;
        }
        SortProperties sortProperties = sortPropertiesList.get(index);
        
        Object o1 = map1.get(sortProperties.getName());
        Object o2 = map2.get(sortProperties.getName());

        if (o1 == null && o2 == null) {
            return compare(index+1, map1, map2);
        }
        else if (o1 == null) {
            return sortProperties.getSortDir() == SortDir.SORT_ASC ? -1 : 1;
        }
        else if (o2 == null) {
            return sortProperties.getSortDir() == SortDir.SORT_ASC ? 1 : -1;
        }
        else {
            int result = this.getComparator(!sortProperties.isCaseInsensitive()).compare(o1, o2);
            if (result == 0) {
                // Identical elements, move onto the next one
                return compare(index+1, map1, map2);
            }
            else if (sortProperties.getSortDir() == SortDir.SORT_DESC) {
                return -result;
            }
            else {
                return result;
            }
        }
    }
    
    private Map<String, Object> getBins(RecordResult kr) {
        if (kr == null || kr.recordOrNull() == null) {
            return null;
        }
        return kr.recordOrNull().bins;
    }
    
    @Override
    public int compare(RecordResult o1, RecordResult o2) {
        return compare(0, getBins(o1), getBins(o2));
    }

}
