package com.example;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.BatchDelete;
import com.aerospike.client.BatchRecord;
import com.aerospike.client.BatchWrite;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.policy.AuthMode;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.BatchWritePolicy;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.ReadModeSC;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.Replica;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Statement;
import com.aerospike.client.task.ExecuteTask;

public class Example2_OldStyle {
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

        ClientPolicy clientPolicy = new ClientPolicy();
        clientPolicy.setAuthMode(AuthMode.INTERNAL);
        clientPolicy.setUser(userName);
        clientPolicy.setPassword(password);
        clientPolicy.setRackAware(true);
        clientPolicy.setRackId(1);
        
        Policy readPolicyDefault = new Policy();
        readPolicyDefault.readModeSC = ReadModeSC.ALLOW_REPLICA;
        readPolicyDefault.replica = Replica.PREFER_RACK;
        clientPolicy.setReadPolicyDefault(readPolicyDefault);
        
        try (IAerospikeClient client = new AerospikeClient(clientPolicy, "localhost", 3100)){
            seed(client);
            Key customerKey = new Key("test", "customer", custId);
            
            // read the customer
            Record customer = client.get(null, customerKey);
            if (customer != null) {
                // Perform a batch get on all the accounts
                List<Long> accountIds = (List<Long>) customer.getList("acctIds");
                Key[] accountKeys = new Key[accountIds.size()];
                
                for (int i = 0; i < accountKeys.length; i++) {
                    accountKeys[i] = new Key("test", "account", accountIds.get(i));
                }
                BatchPolicy batchPolicy = client.copyBatchPolicyDefault();
                batchPolicy.filterExp = Exp.build(
                        Exp.and(
                            Exp.gt(
                                Exp.intBin("halance"),
                                Exp.val(500)
                            ),
                            Exp.eq(
                                Exp.stringBin("status"),
                                Exp.val("ACTIVE")
                            )
                        )
                    );
                Record[] records = client.get(batchPolicy, accountKeys);
                
                // Sum the balance
                int sum = 0; 
                for (int i = 0; i < records.length; i++) {
                    if (records[i] != null) {
                        sum += records[i].getInt("balance");
                    }
                }
                if (sum > 10000) {
                    // Update the status if the record hasn't changed.
                    WritePolicy writePolicy = client.copyWritePolicyDefault();
                    writePolicy.generation = customer.generation;
                    writePolicy.generationPolicy = GenerationPolicy.EXPECT_GEN_EQUAL;
                    client.put(writePolicy, customerKey, new Bin("status", "GOLD"));
                }
            }
            System.out.println(client.get(null, new Key("test", "customer", 1)));
            
            Statement stmt = new Statement();
            stmt.setNamespace("test");
            stmt.setSetName("customer");
            ExecuteTask task = client.execute(null, stmt, Operation.put(new Bin("touched", true)));
            task.waitTillComplete();
            
            List<BatchRecord> recs = new ArrayList<>();
            for (int i: new int[] {1,3,4}) {
                BatchWritePolicy bwp = new BatchWritePolicy();
                bwp.recordExistsAction = RecordExistsAction.UPDATE_ONLY;
                recs.add(new BatchWrite(bwp, new Key("test", "customer", i), Operation.array(
                        Operation.add(new Bin("balance", 100)),
                        Operation.put(new Bin("status", "PAID")))));
            }
            for (int i: new int[] {7,8,9}) {
                recs.add(new BatchDelete(new Key("test", "customer", i)));
            }
            BatchWritePolicy bwp = new BatchWritePolicy();
            bwp.recordExistsAction = RecordExistsAction.UPDATE;
            recs.add(new BatchWrite(new Key("test", "account", 100), Operation.array(
                        Operation.add(new Bin("ledger", 3 * 100))))
            );
            client.operate(null, recs);
        }
    }
}
