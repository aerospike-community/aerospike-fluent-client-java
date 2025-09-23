package com.example;

import java.util.List;
import java.util.Optional;

import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.DataSet;
import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.policy.Behavior;

public class Example2_NewStyle {
    public static void seed(IAerospikeClient client) {
        WritePolicy wp = client.copyWritePolicyDefault();
        wp.recordExistsAction = RecordExistsAction.REPLACE;
        client.put(wp, new Key("test", "customer", 1), new Bin("name", "Tim"), new Bin("acctIds", List.of(1,2,3,4,5,6,7,8,9)));
        for (int i = 1; i <= 9; i++) {
            client.put(null, new Key("test", "account", i), new Bin("balance", 500*i));
        }
        System.out.println(client.get(null, new Key("test", "customer", 1)));
    }
    
    public static void main(String[] args) {
        String userName = null;
        String password = null;
        int custId = 1;

        ClusterDefinition definition = new ClusterDefinition("localhost", 3100)
                .withNativeCredentials(userName, password)
                .preferringRacks(1);

        try (Cluster cluster = definition.connect()) {
            
            Session session = cluster.createSession(Behavior.DEFAULT);
            seed(session.getClient());
            
            DataSet customerDataSet = DataSet.of("test", "customer");
            DataSet accountDataSet = DataSet.of("test", "account");
            
            Optional<KeyRecord> customer = session.query(customerDataSet.id(custId))
                    .execute().getFirst();
            
            customer.ifPresent(cust -> {
                RecordStream accts = session.query(accountDataSet.ids(cust.record.getList("acctIds"))).execute();
                int sum = accts.stream().mapToInt(keyRecord -> keyRecord.record.getInt("balance")).sum();
                
                if (sum > 10000) {
                    session.update(customerDataSet.id(custId))
                            .bin("status").setTo("GOLD")
                            .ensureGenerationIs(cust.record.generation)
                            .execute();
                }
            });
            System.out.println(session.query(customerDataSet.id(1)).execute().getFirst());
        }
    }
}
