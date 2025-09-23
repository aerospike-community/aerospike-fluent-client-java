package com.example.perf;

import java.util.Map;

import com.aerospike.MapUtil;
import com.aerospike.RecordMapper;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.example.perf.Customer.Status;

public class CustomerMapper implements RecordMapper<Customer> {

    @Override
    public Customer fromMap(Map<String, Object> map, Key recordKey, int generation) {
        Customer customer = new Customer();
        customer.setFirstName(MapUtil.asString(map, "firstName"));
        customer.setLastName(MapUtil.asString(map, "lastName"));
        customer.setDob(MapUtil.asDateFromLong(map, "dob"));
        customer.setId(MapUtil.asLong(map, "id"));
        customer.setStatus(MapUtil.asEnum(map, "status", Status.class));
        customer.setPhoneNum(MapUtil.asString(map, "phoneNum"));
        customer.setAddrLine1(MapUtil.asString(map, "addrLine1"));
        customer.setAddrCity(MapUtil.asString(map, "addrCity"));
        customer.setAddrState(MapUtil.asString(map, "addrState"));
        customer.setAddrCountry(MapUtil.asString(map, "addrCountry"));
        customer.setAddrZip(MapUtil.asString(map, "addrZip"));
        customer.setPayload(MapUtil.asBlob(map, "payload"));
        return customer;
    }

    @Override
    public Map<String, Value> toMap(Customer element) {
        if (element == null) {
            return null;
        }
        return MapUtil.buildMap()
                .add("firstName", element.getFirstName())
                .add("lastName", element.getLastName())
                .addAsLong("dob", element.getDob())
                .add("id", element.getId())
                .add("status", element.getStatus())
                .add("phoneNum", element.getPhoneNum())
                .add("addrLine1", element.getAddrLine1())
                .add("addrCity", element.getAddrCity())
                .add("addrState", element.getAddrState())
                .add("addrCountry", element.getAddrCountry())
                .add("addrZip", element.getAddrZip())
                .add("payload", element.getPayload())
                .done();
    }

    @Override
    public Object id(Customer element) {
        return element.getId();
    }
    
}