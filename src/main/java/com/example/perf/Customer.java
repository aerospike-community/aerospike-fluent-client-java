package com.example.perf;

import java.util.Date;

public class Customer {
    public enum Status { NONE, BRONZE, SILVER, GOLD, PLATINUM };
    private String firstName;
    private String lastName;
    private Date dob;
    private long id;
    private Customer.Status status;
    private String phoneNum;
    private String addrLine1;
    private String addrCity;
    private String addrState;
    private String addrCountry;
    private String addrZip;
    private byte[] payload;
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public Date getDob() {
        return dob;
    }
    public void setDob(Date dob) {
        this.dob = dob;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public Customer.Status getStatus() {
        return status;
    }
    public void setStatus(Customer.Status status) {
        this.status = status;
    }
    public String getPhoneNum() {
        return phoneNum;
    }
    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }
    public String getAddrLine1() {
        return addrLine1;
    }
    public void setAddrLine1(String addrLine1) {
        this.addrLine1 = addrLine1;
    }
    public String getAddrCity() {
        return addrCity;
    }
    public void setAddrCity(String addrCity) {
        this.addrCity = addrCity;
    }
    public String getAddrState() {
        return addrState;
    }
    public void setAddrState(String addrState) {
        this.addrState = addrState;
    }
    public String getAddrCountry() {
        return addrCountry;
    }
    public void setAddrCountry(String addrCountry) {
        this.addrCountry = addrCountry;
    }
    public String getAddrZip() {
        return addrZip;
    }
    public void setAddrZip(String addrZip) {
        this.addrZip = addrZip;
    }
    public byte[] getPayload() {
        return payload;
    }
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }
}