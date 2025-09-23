package com.aerospike.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.query.KeyRecord;

public class AsyncRecordFetcher {
    private final IAerospikeClient client;
    
    public AsyncRecordFetcher(IAerospikeClient client) {
        super();
        this.client = client;
    }

    public void batchGet(
            List<Key> keys,
            Consumer<KeyRecord> onRecordFetched,
            Runnable onComplete,
            Consumer<Throwable> onError) {
                
        List<CompletableFuture<Void>> futures = keys.stream()
                .map(key -> fetchFromDBAsync(key)
                    .thenAccept(onRecordFetched != null ? onRecordFetched : record -> {})
                    .exceptionally(ex -> {
                        if (onError != null) {
                            onError.accept(ex);
                        }
                        return null;
                }))
                .collect(Collectors.toList());
        
        // When all the futures are done, run onComplete
        if (onComplete != null) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(onComplete);
        }
    }
    
    private CompletableFuture<KeyRecord> fetchFromDBAsync(Key key) {
        return CompletableFuture.supplyAsync(() -> new KeyRecord(key, client.get(null, key)));
    }
    
    public static class DataSet {
        final String namespace;
        final String setName;
        public DataSet(String namespace, String setName) {
            super();
            this.namespace = namespace;
            this.setName = setName;
        }
        
        public Key id(int id) {
            return new Key(this.namespace, this.setName, id);
        }
        public List<Key> ids(int ...ids) {
            List<Key> results = new ArrayList<>();
            for (int id : ids) {
                results.add(new Key(this.namespace, this.setName, id));
            }
            return results;
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        try (IAerospikeClient client = new AerospikeClient("localhost", 3100)) {
            DataSet customerDS = new DataSet("test", "cust");
            for (int i = 0; i < 10; i++) {
                client.put(null, customerDS.id(i), new Bin("name", "name-" + i), new Bin("age", i));
            }
            
            AsyncRecordFetcher fetcher = new AsyncRecordFetcher(client);
            fetcher.batchGet(
                    customerDS.ids(1,2,3,4), 
                    record -> System.out.printf("got record %s\n", record), 
                    () -> System.out.printf("done!\n"), 
                    null);
            Thread.sleep(2000);
        }
    }
}
