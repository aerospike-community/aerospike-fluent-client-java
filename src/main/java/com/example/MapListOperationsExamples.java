package com.example;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.DataSet;
import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.AerospikeException;
import com.aerospike.policy.Behavior;

/**
 * Examples demonstrating the onMapKeyList() and onMapValueList() methods.
 * These methods allow operations on multiple map keys or values at once.
 */
public class MapListOperationsExamples {

    /**
     * Example 1: Get values for multiple keys using onMapKeyList
     */
    public static void example1_GetValuesByKeyList(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with scores
            Map<String, Integer> scores = new LinkedHashMap<>();
            scores.put("math", 95);
            scores.put("science", 88);
            scores.put("english", 92);
            scores.put("history", 85);
            scores.put("art", 90);
            
            session.upsert(dataSet.id("student1"))
                .bin("scores").setTo(scores)
                .execute();
            
            // Get values for specific keys
            RecordStream results = session.update(dataSet.id("student1"))
                .bin("scores")
                .onMapKeyList(List.of("math", "science", "english"))
                .getValues()
                .execute();
            
            System.out.println("Scores for math, science, english: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 2: Remove items matching a list of values using onMapValueList
     */
    public static void example2_RemoveByValueList(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with statuses
            Map<String, String> userStatuses = new LinkedHashMap<>();
            userStatuses.put("user1", "active");
            userStatuses.put("user2", "inactive");
            userStatuses.put("user3", "deleted");
            userStatuses.put("user4", "active");
            userStatuses.put("user5", "inactive");
            
            session.upsert(dataSet.id("statuses1"))
                .bin("users").setTo(userStatuses)
                .execute();
            
            // Remove items with inactive or deleted status
            session.update(dataSet.id("statuses1"))
                .bin("users")
                .onMapValueList(List.of("inactive", "deleted"))
                .remove()
                .execute();
            
            System.out.println("Removed inactive and deleted users");
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 3: Count items NOT matching the key list (inverted)
     */
    public static void example3_CountInvertedKeys(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with various data
            Map<String, Integer> data = new LinkedHashMap<>();
            data.put("key1", 10);
            data.put("key2", 20);
            data.put("key3", 30);
            data.put("key4", 40);
            data.put("key5", 50);
            
            session.upsert(dataSet.id("data1"))
                .bin("data").setTo(data)
                .execute();
            
            // Count items NOT matching key1 and key2
            RecordStream results = session.update(dataSet.id("data1"))
                .bin("data")
                .onMapKeyList(List.of("key1", "key2"))
                .countAllOthers()
                .execute();
            
            System.out.println("Count of items NOT key1 or key2: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 4: Get keys matching a list of values
     */
    public static void example4_GetKeysByValueList(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with inventory quantities
            Map<String, Integer> inventory = new LinkedHashMap<>();
            inventory.put("apples", 0);
            inventory.put("bananas", 50);
            inventory.put("oranges", 0);
            inventory.put("grapes", 100);
            inventory.put("peaches", 0);
            
            session.upsert(dataSet.id("inventory1"))
                .bin("stock").setTo(inventory)
                .execute();
            
            // Find which items are out of stock (value = 0)
            RecordStream results = session.update(dataSet.id("inventory1"))
                .bin("stock")
                .onMapValueList(List.of(0))
                .getKeys()
                .execute();
            
            System.out.println("Out of stock items: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 5: Count matching values
     */
    public static void example5_CountByValueList(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with grades
            Map<String, String> grades = new LinkedHashMap<>();
            grades.put("student1", "A");
            grades.put("student2", "B");
            grades.put("student3", "A");
            grades.put("student4", "C");
            grades.put("student5", "A");
            grades.put("student6", "B");
            
            session.upsert(dataSet.id("grades1"))
                .bin("grades").setTo(grades)
                .execute();
            
            // Count how many A's and B's
            RecordStream results = session.update(dataSet.id("grades1"))
                .bin("grades")
                .onMapValueList(List.of("A", "B"))
                .count()
                .execute();
            
            System.out.println("Count of A and B grades: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ClusterDefinition clusterDef = new ClusterDefinition("localhost", 3000);
        
        try (Cluster cluster = clusterDef.connect()) {
            Session session = cluster.createSession(Behavior.DEFAULT);
            DataSet dataSet = DataSet.of("test", "mapListExamples");
            
            System.out.println("=== Example 1: Get Values by Key List ===");
            example1_GetValuesByKeyList(session, dataSet);
            
            System.out.println("\n=== Example 2: Remove by Value List ===");
            example2_RemoveByValueList(session, dataSet);
            
            System.out.println("\n=== Example 3: Count Inverted Keys ===");
            example3_CountInvertedKeys(session, dataSet);
            
            System.out.println("\n=== Example 4: Get Keys by Value List ===");
            example4_GetKeysByValueList(session, dataSet);
            
            System.out.println("\n=== Example 5: Count by Value List ===");
            example5_CountByValueList(session, dataSet);
            
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

