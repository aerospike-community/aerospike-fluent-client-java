package com.example.model_mappers;

import java.util.Map;

import com.aerospike.MapUtil;
import com.aerospike.RecordMapper;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.example.model.Address;

public class AddressMapper implements RecordMapper<Address> {

    @Override
    public Address fromMap(Map<String, Object> map, Key recordKey, int generation) {
        Address result = new Address(
                MapUtil.asString(map, "line1"),
                MapUtil.asString(map, "city"),
                MapUtil.asString(map, "state"),
                MapUtil.asString(map, "country"),
                MapUtil.asString(map, "zip"));
        return result;
    }

    @Override
    public Map<String, Value> toMap(Address addr) {
        return MapUtil.buildMap()
                .add("line1", addr.getLine1())
                .add("city", addr.getCity())
                .add("state", addr.getState())
                .add("country", addr.getCountry())
                .add("zip", addr.getZipCode())
                .done();
    }

    @Override
    public Object id(Address element) {
        return null;
    }

}
