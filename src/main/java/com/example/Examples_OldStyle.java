package com.example;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Info;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.CTX;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.BatchWritePolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;

public class Examples_OldStyle {

    private static final int ITERATIONS = 1;

    private Map<String, Object> getMapToStore() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Integer> childMap = new HashMap<>();
        childMap.put("clash", 5602);
        childMap.put("galaga", 346724);
        childMap.put("space invaders", 23421221);
        
        map.put("topscore", childMap);
        return map;
    }
    

    public long run() {
        IAerospikeClient client = new AerospikeClient("localhost", 3100);
        Policy readPolicy = new Policy();
        readPolicy.maxRetries = 5;
        readPolicy.sendKey = true;
        
        WritePolicy writePolicy = new WritePolicy();
        writePolicy.sendKey = true;

        Key key1 = new Key("test", "testSet", 1);
        Key key2 = new Key("test", "testSet", 2);
        Key key3 = new Key("test", "testSet", 3);
        Key key4 = new Key("test", "testSet", 3);
        
        // Set up the keys for the delete style operations
        client.truncate(null, "test", "testSet", null);
        WritePolicy wp = new WritePolicy(writePolicy);
        wp.recordExistsAction = RecordExistsAction.UPDATE;
        client.put(null, key3, new Bin("person", getMapToStore()), new Bin("name", "Tim"));
        
//        NamespaceInfo nodeMetrics = session.getNodes()[0].getNamespaceInfo("test");
        boolean isInStopWrites = false;
        for (Node node : client.getNodes()) {
            String nsInfo = Info.request(node, "namespace/test");
            String[] items = nsInfo.split(";");
            for (String thisItem : items) {
                if (thisItem.startsWith("stop_writes=")) {
                    String[] keyAndValue = thisItem.split("=");
                    isInStopWrites = isInStopWrites && Boolean.valueOf(keyAndValue[1]);
                    break;
                }
                
            }
        }
        System.out.println("Cluster is in stop writes: " + isInStopWrites);
        // To much code needed to reproduce this line
//        nsInfo.refreshEvery(5, TimeUnit.SECONDS).onChange("client_read_success", (oldValue, newValue) -> {
//            System.out.println("Successful client reads: " + newValue);
//        });
        
        client.put(wp, key1, new Bin("name", "Tim"));
        client.operate(null, null, new Key[] {key2, key3}, Operation.put(new Bin("name", "Jake")));
        Record[] recs = client.get(null, new Key[] {key1, key2, key3, key4});
        for (Record r : recs) {
            System.out.println(r);
        }
        Policy policy = new Policy(client.getReadPolicyDefault());
        policy.maxRetries = 5;
        System.out.println(client.get(policy, key1));
        
        client.operate(null, new Key("test", "testSet", 10),
                MapOperation.put(MapPolicy.Default, "map", Value.get("Tim"), Value.get(312)),
                MapOperation.put(MapPolicy.Default, "map", Value.get("Bob"), Value.get(45)),
                MapOperation.put(MapPolicy.Default, "map", Value.get("Joe"), Value.get(28)));
        
        System.out.println(client.operate(null, new Key("test", "testSet", 10),
                MapOperation.getByKeyRange("map", Value.get("A"), Value.get("K"), MapReturnType.COUNT)));
                
        System.out.println(client.operate(null, new Key("test", "testSet", 10),
                MapOperation.getByKeyRange("map", Value.get("A"), Value.get("K"), MapReturnType.COUNT|MapReturnType.INVERTED)));
                
        System.out.println(client.operate(null, new Key("test", "testSet", 10),
                MapOperation.getByKey("map", Value.get("Tim"), MapReturnType.COUNT | MapReturnType.INVERTED)));
                
        
        long now = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            System.out.println(client.get(null, key3));
            client.operate(wp, key3, 
                    MapOperation.removeByKey("person", Value.get("clash"), MapReturnType.NONE, CTX.mapKey(Value.get("topscore"))));
            System.out.println(client.get(null, key3));
            client.operate(wp, key3, 
                    MapOperation.removeByKey("person", Value.get("space invaders"), MapReturnType.NONE, CTX.mapKey(Value.get("topscore"))));
            System.out.println(client.get(null, key3));
            client.put(wp, key3, Bin.asNull("name"));
            System.out.println(client.get(null, key3));

            BatchPolicy batchPolicy = new BatchPolicy();
            batchPolicy.sendKey = true;
            BatchWritePolicy bwp = new BatchWritePolicy();
            bwp.recordExistsAction = RecordExistsAction.UPDATE;
            bwp.sendKey = true;
            client.operate(batchPolicy, bwp, new Key[] {key1, key2, key3}, Operation.put(new Bin("now", new Date().getTime())));
            client.put(wp, key1, new Bin("name", "Fred"));
            client.put(wp, key1, new Bin("name", "Fred"), new Bin("age", 21), Bin.asNull("other"));

            client.put(wp, key1, new Bin("name", "Fred"), new Bin("age", 21));
            client.put(wp, key1, new Bin("name", "Alex"), new Bin("age", 99));
            
            BatchPolicy batchReadPolicy = new BatchPolicy(batchPolicy);
            Record[] records = client.get(batchReadPolicy, new Key[] {key1, key2});
            
            System.out.println(records[0]);
            System.out.println(records[1]);
    
//            connection.replace(key1)
//                    .withBins(new Bin("name", "Tim"), new Bin("age", 312))
//                    .withRetrySettingsOf(retrySettings)
//                    .requiringGenerationOf(records.get(0).generation).go();
            
//            connection.get(key1).where("$.a > 17").go();
        }
        long time = (System.nanoTime() - now) / 1000;
        System.out.printf("Old way completed in %,dus\n", time);
        return time;
    }
    
    public static void main(String[] args) throws InterruptedException {
        Examples_OldStyle example = new Examples_OldStyle();
        example.run();
    }
}
