package com.example.model;

import java.util.Date;

import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;

import lombok.Data;

@Data
@AerospikeRecord(namespace = "test", set = "cust")
public class Customer {
    @AerospikeKey
    private long id;
    private String name;
    private int age;
    private Date dob;
    @AerospikeEmbed
    private Address address;
    
    public Customer() {
        super();
    }
    public Customer(long id, String name, int age, Date dob) {
        this(id, name, age, dob, null);
    }
    public Customer(long id, String name, int age, Date dob, Address address) {
        super();
        this.id = id;
        this.name = name;
        this.age = age;
        this.dob = dob;
        this.address = address;
    }
    
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
    public Date getDob() {
        return dob;
    }
    public void setDob(Date dob) {
        this.dob = dob;
    }
    
    public Address getAddress() {
        return address;
    }
    public void setAddress(Address address) {
        this.address = address;
    }
    
    @Override
    public String toString() {
        return "Customer [id=" + id + ", name=" + name + ", age=" + age + ", dob=" + dob + ", address=" + address + "]";
    }
}