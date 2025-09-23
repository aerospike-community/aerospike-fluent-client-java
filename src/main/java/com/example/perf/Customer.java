package com.example.perf;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
}