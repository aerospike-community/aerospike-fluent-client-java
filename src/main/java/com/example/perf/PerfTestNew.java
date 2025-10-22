package com.example.perf;

import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.Session;
import com.aerospike.TypeSafeDataSet;
import com.aerospike.policy.Behavior;

public class PerfTestNew implements IPerfTest {
    private final Cluster cluster;
    private final Session session;
    private final TypeSafeDataSet<Customer> customerDataSet;
    private final CustomerMapper customerMapper = new CustomerMapper();
    
    public PerfTestNew(String host, int port) {
        this.cluster = new ClusterDefinition(host, port).connect();
        this.session = cluster.createSession(Behavior.DEFAULT);
        this.customerDataSet = new TypeSafeDataSet<>("test", "cust", Customer.class);
    }
    
    @Override
    public String getName() {
        return "New Client";
    }

    @Override
    public void truncate() {
        session.truncate(customerDataSet);
    }
    
    @Override
    public void insert(Customer customer) {
        session.insert(customerDataSet)
            .object(customer)
            .using(customerMapper)
            .execute();
    }
    
    @Override
    public void close() {
        this.cluster.close();
    }
}