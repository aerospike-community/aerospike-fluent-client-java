package com.example.perf;

public interface IPerfTest {
    String getName();
    void truncate();
    void insert(Customer customer);
    void close();
}