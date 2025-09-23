package com.aerospike.query;

import java.util.List;

public interface Sortable {
    void sortBy(List<SortProperties> sortPropertyList);

    void sortBy(SortProperties sortProperty);
}
