package com.example;

import com.aerospike.*;
import com.aerospike.client.AerospikeException;

import java.util.*;

/**
 * Examples demonstrating the new relative range CDT operations
 * for maps in the Aerospike Fluent Client.
 * 
 * These examples show how to use:
 * - onMapKeyRelativeIndexRange() - Select map items relative to a key by index
 * - onMapValueRelativeRankRange() - Select map items relative to a value by rank
 */
public class RelativeRangeExamples {
    
    /**
     * Example 1: Get values starting from a specific key, offset by index
     * 
     * Use case: You have a sorted map of scores by student names, and you want
     * to get scores starting from "Mary" plus 2 positions forward.
     */
    public static void example1_KeyRelativeIndexRange(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with student scores
            Map<String, Long> scores = new LinkedHashMap<>();
            scores.put("Alice", 85L);
            scores.put("Bob", 92L);
            scores.put("Charlie", 78L);
            scores.put("David", 95L);
            scores.put("Eve", 88L);
            scores.put("Frank", 91L);
            scores.put("Grace", 87L);
            scores.put("Henry", 93L);
            
            // Insert the map
            session.upsert(dataSet.id("class123"))
                .bin("scores").setTo(scores)
                .execute();
            
            // Get values starting at "Charlie" + 2 positions (should get David, Eve, Frank, etc.)
            RecordStream results = session.upsert(dataSet.id("class123"))
                .bin("scores")
                .onMapKeyRelativeIndexRange("Charlie", 2)
                .getValues()
                .execute();
            
            System.out.println("Values starting from Charlie+2: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 2: Get a limited number of values with key relative index range
     * 
     * Use case: Get only 3 scores starting from "Bob" + 1 position
     */
    public static void example2_KeyRelativeIndexRangeWithCount(Session session, DataSet dataSet) {
        try {
            // Get exactly 3 values starting at "Bob" + 1 position
            RecordStream results = session.upsert(dataSet.id("class123"))
                .bin("scores")
                .onMapKeyRelativeIndexRange("Bob", 1, 3)
                .getValues()
                .execute();
            
            System.out.println("3 values starting from Bob+1: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 3: Get values by relative rank (sorted by value)
     * 
     * Use case: Get all scores that rank above 85 (plus 2 ranks higher)
     */
    public static void example3_ValueRelativeRankRange(Session session, DataSet dataSet) {
        try {
            // Get values ranked above 85 (offset by 2 ranks)
            RecordStream results = session.upsert(dataSet.id("class123"))
                .bin("scores")
                .onMapValueRelativeRankRange(85L, 2)
                .getValues()
                .execute();
            
            System.out.println("Values ranked 2 positions above 85: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 4: Count items in a relative range
     * 
     * Use case: Count how many scores are in a specific relative range
     */
    public static void example4_CountRelativeRange(Session session, DataSet dataSet) {
        try {
            // Count items starting from "David" + 1 position, for 3 items
            RecordStream results = session.upsert(dataSet.id("class123"))
                .bin("scores")
                .onMapKeyRelativeIndexRange("David", 1, 3)
                .count()
                .execute();
            
            System.out.println("Count in range: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 5: Count all items OUTSIDE a relative range (inverted)
     * 
     * Use case: Count how many scores are NOT in the top 5 (ranked by value)
     */
    public static void example5_CountAllOthers(Session session, DataSet dataSet) {
        try {
            // Count all items NOT in the range starting from value 90, rank offset 0, count 5
            RecordStream results = session.upsert(dataSet.id("class123"))
                .bin("scores")
                .onMapValueRelativeRankRange(90L, 0, 5)
                .countAllOthers()
                .execute();
            
            System.out.println("Count outside range: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 6: Remove items in a relative range
     * 
     * Use case: Remove the bottom 3 scores (by value rank)
     */
    public static void example6_RemoveRelativeRange(Session session, DataSet dataSet) {
        try {
            // Remove the 3 lowest scores
            RecordStream results = session.upsert(dataSet.id("class123"))
                .bin("scores")
                .onMapValueRelativeRankRange(0L, 0, 3)  // Start at minimum, take 3
                .remove()
                .execute();
            
            System.out.println("Removed items: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 7: Get keys in a relative range
     * 
     * Use case: Get student names in a specific index range
     */
    public static void example7_GetKeysRelativeRange(Session session, DataSet dataSet) {
        try {
            // Get keys (student names) starting from "Bob" + 1, for 4 items
            RecordStream results = session.upsert(dataSet.id("class123"))
                .bin("scores")
                .onMapKeyRelativeIndexRange("Bob", 1, 4)
                .getKeys()
                .execute();
            
            System.out.println("Keys in range: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 8: Nested map operation with relative range
     * 
     * Use case: Navigate to a nested map and perform a relative range operation
     */
    public static void example8_NestedMapRelativeRange(Session session, DataSet dataSet) {
        try {
            // Create a nested structure: classes -> class1 -> students -> scores
            Map<String, Object> classData = new HashMap<>();
            Map<String, Long> class1Scores = new LinkedHashMap<>();
            class1Scores.put("Alice", 85L);
            class1Scores.put("Bob", 92L);
            class1Scores.put("Charlie", 78L);
            classData.put("class1", class1Scores);
            
            // Insert nested structure
            session.upsert(dataSet.id("school123"))
                .bin("classes").setTo(classData)
                .execute();
            
            // Navigate to nested map and perform relative range operation
            RecordStream results = session.upsert(dataSet.id("school123"))
                .bin("classes")
                .onMapKey("class1")  // Navigate to class1
                .onMapKeyRelativeIndexRange("Alice", 1)  // Get from Alice+1
                .getValues()
                .execute();
            
            System.out.println("Nested map relative range: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 9: Working with byte array keys
     * 
     * Use case: Binary keys with relative range operations
     */
    public static void example9_ByteArrayKeys(Session session, DataSet dataSet) {
        try {
            // Get values using byte array key
            byte[] startKey = "key123".getBytes();
            
            RecordStream results = session.upsert(dataSet.id("binary123"))
                .bin("data")
                .onMapKeyRelativeIndexRange(startKey, 2, 5)
                .getValues()
                .execute();
            
            System.out.println("Values with byte array key: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 10: Performance optimization - remove all low scores in one operation
     * 
     * Use case: Efficiently remove all scores below a threshold
     */
    public static void example10_BulkRemove(Session session, DataSet dataSet) {
        try {
            // Remove all scores in the bottom half (by rank)
            // Assuming we have 8 scores, remove the bottom 4
            RecordStream results = session.upsert(dataSet.id("class123"))
                .bin("scores")
                .onMapValueRelativeRankRange(0L, 0, 4)  // Bottom 4 by rank
                .remove()
                .execute();
            
            System.out.println("Bulk removed: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        // Setup connection (example)
        ClusterDefinition clusterDef = new ClusterDefinition("localhost", 3000);
        
        try (Cluster cluster = clusterDef.connect()) {
            Session session = cluster.session();
            DataSet dataSet = DataSet.of("test", "students");
            
            System.out.println("=== Example 1: Key Relative Index Range ===");
            example1_KeyRelativeIndexRange(session, dataSet);
            
            System.out.println("\n=== Example 2: Key Relative Index Range with Count ===");
            example2_KeyRelativeIndexRangeWithCount(session, dataSet);
            
            System.out.println("\n=== Example 3: Value Relative Rank Range ===");
            example3_ValueRelativeRankRange(session, dataSet);
            
            System.out.println("\n=== Example 4: Count Relative Range ===");
            example4_CountRelativeRange(session, dataSet);
            
            System.out.println("\n=== Example 5: Count All Others (Inverted) ===");
            example5_CountAllOthers(session, dataSet);
            
            System.out.println("\n=== Example 6: Remove Relative Range ===");
            example6_RemoveRelativeRange(session, dataSet);
            
            System.out.println("\n=== Example 7: Get Keys Relative Range ===");
            example7_GetKeysRelativeRange(session, dataSet);
            
            System.out.println("\n=== Example 8: Nested Map Relative Range ===");
            example8_NestedMapRelativeRange(session, dataSet);
            
            System.out.println("\n=== Example 9: Byte Array Keys ===");
            example9_ByteArrayKeys(session, dataSet);
            
            System.out.println("\n=== Example 10: Bulk Remove ===");
            example10_BulkRemove(session, dataSet);
            
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Utility: Print map contents for debugging
     */
    private static void printMap(Session session, DataSet dataSet, String id) {
        try {
            RecordStream results = session.query(dataSet.id(id))
                .execute();
            System.out.println("Current map contents: " + results);
        } catch (AerospikeException e) {
            System.err.println("Error reading map: " + e.getMessage());
        }
    }
}

