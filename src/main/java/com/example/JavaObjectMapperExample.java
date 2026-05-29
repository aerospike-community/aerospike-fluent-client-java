package com.example;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.DataSet;
import com.aerospike.DefaultRecordMappingFactory;
import com.aerospike.RecordMapper;
import com.aerospike.Session;
import com.aerospike.TypedDataSet;
import com.aerospike.client.Key;
import com.aerospike.client.Log.Level;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.policy.Behavior;
import com.example.model.Address;
import com.example.model.Customer;

public class JavaObjectMapperExample {
    
    public static class Mapper<T> implements RecordMapper<T> {
        private final AeroMapper mapper;
        private final Class<T> clazz;
        public Mapper(AeroMapper mapper, Class<T> clazz) {
            this.mapper = mapper;
            this.clazz = clazz;
        }
        
        @Override
        public T fromMap(Map<String, Object> map, Key recordKey, int generation) {
            // TODO: Expiration?
            Record rec = new Record(map, generation, 0);
            return mapper.getMappingConverter().convertToObject(clazz, recordKey, rec);
        }

        @Override
        public Map<String, Value> toMap(T element) {
            Map<String, Object> map =  mapper.getMappingConverter().convertToMap(element);
            Map<String, Value> result = new HashMap<>();
            for (Entry<String, Object> entry : map.entrySet()) {
                result.put(entry.getKey(), Value.get(entry.getValue()));
            }
            return result;
        }

        @Override
        public Object id(T element) {
            return mapper.getKey(element);
        }
    }
    
    public static void main(String[] args) {
        try (Cluster cluster = new ClusterDefinition("localhost", 3100)
                .withLogLevel(Level.DEBUG)
                .connect()) {

            AeroMapper aeroMapper = new AeroMapper.Builder(cluster.getUnderlyingClient()).build();

            cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
                        Customer.class, new Mapper<>(aeroMapper, Customer.class),
                        Address.class, new Mapper<>(aeroMapper, Address.class)
                    )
                ));
            
            Customer cust = new Customer(1, "Tim", 312, new Date());
            cust.setAddress(new Address("123 main St", "Denver", "CO", "USA", "80224"));
            
            TypedDataSet<Customer> dataSet = TypedDataSet.of("test", "cust", Customer.class);
            Session session = cluster.createSession(Behavior.DEFAULT);
            session.insert(dataSet)
                    .object(cust)
                    .execute();
            
            System.out.println(session.query(dataSet.id(cust.getId())).execute().getFirst());
            
        }
    }
}
