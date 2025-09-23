package com.example;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.DSLPath;
import com.aerospike.DataSet;
import com.aerospike.OperationBuilder;
import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.metadata.NamespaceInfo;
import com.aerospike.policy.Behavior;

public class Examples_NewStyle {

    private static final int ITERATIONS = 1;

    private Map<String, Object> getMapToStore() {
        return Map.of("topscore", Map.of("clash", 5602, "galaga", 346724, "space invaders", 23421221));
    }
    
    public long run() {
        Cluster connection = new ClusterDefinition("localhost", 3100).connect();

        Behavior newBehavior = Behavior.DEFAULT.deriveWithChanges("newBehavior", builder ->
            builder.onAvailablityModeReads()
                .abandonCallAfter(Duration.ofSeconds(3))
            .done()
        );
        
        Duration.of(50,  ChronoUnit.MILLIS);
        DataSet set = DataSet.of("test", "testSet");
        Key key1 = set.id(1);
        Key key2 = set.id(2);
        Key key3 = set.id(3);
        Key key4 = set.id(4);
        
        DataSet mapTestSet = DataSet.of(set.getNamespace(), "testSet");
        
        // Set up the keys for the delete style operations
        Session session = connection.createSession(newBehavior);
        session.truncate(mapTestSet);
        session.upsert(key3)
            .bin("person").setTo(getMapToStore())
            .bin("name").setTo("Tim")
            .execute();
        
        System.out.println("Get key3: " + session.query(key3).execute());
        System.out.println("Read key3 header: " + session.query(key3).withNoBins().execute());
        
//        NamespaceInfo nodeMetrics = session.getNodes()[0].getNamespaceInfo("test");
//        NamespaceInfo nsInfo = session.getNamespaceInfo("test");
//        System.out.println("Cluster is in stop writes: " + nsInfo.isStopWrites());
//        nsInfo.refreshEvery(5, TimeUnit.SECONDS).onChange("client_read_success", (oldValue, newValue) -> {
//            System.out.println("Successful client reads: " + newValue);
//        });
        
        session.upsert(key1).bin("name").setTo("Tim").execute();
        session.upsert(key2,key3).bin("name").setTo("Jake").execute();
        System.out.println(session.query(key1, key2, key3, key4).execute());
        System.out.println(session.query(key1).execute());

        long now = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            System.out.println(session.query(key3).execute());
//            session.upsert(key3).delete(DSLPath.from("$.person.topscore.clash")).execute();
//            System.out.println(session.query(key3).execute());
//            session.upsert(key3).delete(DSLPath.from("$.person.topscore.'space invaders'")).execute();
//            System.out.println(session.query(key3).execute());
//            session.upsert(key3).delete(DSLPath.from("$.name")).execute();
//            System.out.println(session.query(key3).execute());

            session.upsert(key1, key2, key3).bin("now").setTo(new Date().getTime()).execute();
            session.upsert(key1).bin("name").setTo("Fred").execute();
            session.upsert(key1).bin("name").setTo("Fred")
                    .bin("age").setTo(21)
                    .bin("other").remove()
                    .execute();
            
            session.upsert(key1).bin("name").setTo("Fred").bin("age").setTo(21).execute();
            session.upsert(key2).bins("name", "age").values("Alex", 99).execute();

            OperationBuilder builder = session.upsert(key1);
            builder.bin("a").setTo("b");
            builder.bin("b").setTo("c");
            builder.execute();
            
            RecordStream records = session.query(key1, key2).execute();
            
            System.out.println(records.getFirst());
    
//            connection.replace(key1)
//                    .withBins(new Bin("name", "Tim"), new Bin("age", 312))
//                    .withRetrySettingsOf(retrySettings)
//                    .requiringGenerationOf(records.get(0).generation).go();
            
//            connection.get(key1).where("$.a > 17").go();
        }
        long time = (System.nanoTime() - now) / 1000;
        System.out.printf("New way completed in %,dus\n", time);
        return time;
    }

    public static void main(String[] args) throws InterruptedException {
        Examples_NewStyle example = new Examples_NewStyle();
        example.run();
    }
}
