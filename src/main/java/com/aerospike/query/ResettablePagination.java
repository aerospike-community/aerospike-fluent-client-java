package com.aerospike.query;

public interface ResettablePagination {
    int currentPage();
    int maxPages();
    void setPageTo(int newPage);
}
