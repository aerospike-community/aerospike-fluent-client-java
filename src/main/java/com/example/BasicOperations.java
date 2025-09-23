package com.example;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;

public class BasicOperations {

    public static void main(String[] args) {
        IAerospikeClient client = new AerospikeClient(null, "172.17.0.2", 3000);
        
        Key key = new Key("test", "testSet", 1);
        
        // Upsert a record
        client.put(null, key, new Bin("name", "Tim"), new Bin("age", 312), new Bin("height", 2.01));
        
        // Read the record
        Record record = client.get(null, key);
        System.out.printf("Name: %s, age: %d\n", record.getString("name"), record.getInt("age"));
        
        // Read some bins in the record
        record = client.get(null, key, "height");
        System.out.printf("Name: %s, height: %f\n", record.getString("name"), record.getFloat("height"));
        
        // Update the age
        client.put(null, key, new Bin("age", 313));
        
        // Replace the contents of the record with a new record
        WritePolicy wp = new WritePolicy();
        wp.recordExistsAction = RecordExistsAction.REPLACE;
        client.put(wp, key, new Bin("name", "Bob"), new Bin("age", 37));
        System.out.println(client.get(null, key));
        
        // Delete the record
        client.delete(null, key);
        
        // Multiple operations on a single key
        client.operate(null, key, Operation.append(new Bin("name", " Faulkes")),
                Operation.put(new Bin("title", "Developer Advocate")),
                Operation.add(new Bin("age", 1)));
        
        // Reading multiple records
        Key[] keys = new Key[] {new Key("test", "testSet", 1), new Key("test", "testSet", 2), new Key("test", "testSet", 3)};
        Record[] records = client.get(null, keys);
        
        client.close();
    }
}
