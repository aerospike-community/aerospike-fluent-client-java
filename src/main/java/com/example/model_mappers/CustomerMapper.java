package com.example.model_mappers;

import java.util.Map;

import com.aerospike.MapUtil;
import com.aerospike.RecordMapper;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.mapper.tools.AeroMapper;
import com.example.model.Customer;

public class CustomerMapper implements RecordMapper<Customer> {
    private AeroMapper mapper; 
    
    @Override
    public Customer fromMap(Map<String, Object> map, Key recordKey, int generation) {
//        Customer result = new Customer();
//        result.setId(recordKey.userKey.toLong());
//        result.setAge(MapUtil.asInt(map, "age"));
//        result.setDob(MapUtil.asDateFromLong(map, "dob"));
//        result.setName(MapUtil.asString(map, "name"));
//        result.setAddress(MapUtil.asObjectFromMap(map, "address", new AddressMapper()));
//        return result;
        return mapper.getMappingConverter().convertToObject(Customer.class, map);
    }

    @Override
    public Map<String, Value> toMap(Customer customer) {
        return MapUtil.buildMap()
                .add("id", customer.getId())
                .add("age", customer.getAge())
                .addAsLong("dob", customer.getDob())
                .add("name", customer.getName())
                .add("address", customer.getAddress(), new AddressMapper())
                .done();
    }

    @Override
    public Object id(Customer customer) {
        return customer.getId();
    }
}