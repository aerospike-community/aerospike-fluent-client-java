package com.example;

import java.util.Arrays;
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
 * Examples demonstrating the new Map CDT operations:
 * - onMapIndexRange - select by index range
 * - onMapRankRange - select by rank range
 * - Additional type overloads for onMapValue, onMapKeyRange, onMapValueRange
 */
public class CdtMapOperationsExamples {
    
    /**
     * Example 1: Get values by index range
     */
    public static void example1_MapIndexRange(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with scores
            Map<String, Long> scores = new LinkedHashMap<>();
            scores.put("Alice", 85L);
            scores.put("Bob", 92L);
            scores.put("Charlie", 78L);
            scores.put("David", 95L);
            scores.put("Eve", 88L);
            
            session.upsert(dataSet.id("class1"))
                .bin("scores").setTo(scores)
                .execute();
            
            // Get items at indices 1-3 (Bob, Charlie, David)
            RecordStream results = session.update(dataSet.id("class1"))
                .bin("scores")
                .onMapIndexRange(1, 3)
                .getValues()
                .execute();
            
            System.out.println("Values at indices 1-3: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 2: Get values by rank range (sorted by value)
     */
    public static void example2_MapRankRange(Session session, DataSet dataSet) {
        try {
            // Get the 3 lowest scores (ranks 0-2)
            RecordStream results = session.update(dataSet.id("class1"))
                .bin("scores")
                .onMapRankRange(0, 3)
                .getValues()
                .execute();
            
            System.out.println("3 lowest scores: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 3: Use onMapValue with double type
     */
    public static void example3_MapValueDouble(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with double values
            Map<String, Double> prices = new LinkedHashMap<>();
            prices.put("apple", 1.99);
            prices.put("banana", 0.99);
            prices.put("orange", 2.49);
            
            session.upsert(dataSet.id("prices1"))
                .bin("prices").setTo(prices)
                .execute();
            
            // Find items with specific price
            RecordStream results = session.update(dataSet.id("prices1"))
                .bin("prices")
                .onMapValue(1.99)
                .getKeys()
                .execute();
            
            System.out.println("Items priced at 1.99: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 4: Use onMapValueRange with String type
     */
    public static void example4_MapValueRangeString(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with string values
            Map<Integer, String> names = new LinkedHashMap<>();
            names.put(1, "Alice");
            names.put(2, "Bob");
            names.put(3, "Charlie");
            names.put(4, "David");
            
            session.upsert(dataSet.id("names1"))
                .bin("names").setTo(names)
                .execute();
            
            // Get names in range "B" to "D"
            RecordStream results = session.update(dataSet.id("names1"))
                .bin("names")
                .onMapValueRange("B", "D")
                .getValues()
                .execute();
            
            System.out.println("Names B-D: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 5: Use onMapValue with boolean type
     */
    public static void example5_MapValueBoolean(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with boolean values
            Map<String, Boolean> flags = new LinkedHashMap<>();
            flags.put("feature1", true);
            flags.put("feature2", false);
            flags.put("feature3", true);
            
            session.upsert(dataSet.id("flags1"))
                .bin("flags").setTo(flags)
                .execute();
            
            // Find all enabled features
            RecordStream results = session.update(dataSet.id("flags1"))
                .bin("flags")
                .onMapValue(true)
                .getKeys()
                .execute();
            
            System.out.println("Enabled features: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 6: Count items in index range (inverted)
     */
    public static void example6_CountAllOthersIndexRange(Session session, DataSet dataSet) {
        try {
            // Count all items OUTSIDE index range 1-3
            RecordStream results = session.update(dataSet.id("class1"))
                .bin("scores")
                .onMapIndexRange(1, 3)
                .countAllOthers()
                .execute();
            
            System.out.println("Count outside range: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 7: Remove items by rank range
     */
    public static void example7_RemoveByRankRange(Session session, DataSet dataSet) {
        try {
            // Remove the bottom 2 scores
            RecordStream results = session.update(dataSet.id("class1"))
                .bin("scores")
                .onMapRankRange(0, 2)
                .remove()
                .execute();
            
            System.out.println("Removed bottom 2 scores");
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 8: Nested operation - onMapKey followed by onMapIndexRange
     */
    public static void example8_NestedMapOperations(Session session, DataSet dataSet) {
        try {
            // Setup: Create a nested structure
            Map<String, Object> classData = new LinkedHashMap<>();
            
            Map<String, Long> class1Scores = new LinkedHashMap<>();
            class1Scores.put("Alice", 85L);
            class1Scores.put("Bob", 92L);
            class1Scores.put("Charlie", 78L);
            class1Scores.put("David", 95L);
            
            classData.put("class1", class1Scores);
            
            session.upsert(dataSet.id("school1"))
                .bin("classes").setTo(classData)
                .execute();
            
            // Navigate to nested map and get index range
            RecordStream results = session.update(dataSet.id("school1"))
                .bin("classes")
                .onMapKey("class1")
                .onMapIndexRange(1, 2)
                .getValues()
                .execute();
            
            System.out.println("Nested range values: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 9: Use onMapValue with List type
     */
    public static void example9_MapValueWithList(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with list values
            Map<String, List<Integer>> groupScores = new LinkedHashMap<>();
            groupScores.put("groupA", Arrays.asList(90, 85, 92));
            groupScores.put("groupB", Arrays.asList(78, 82, 88));
            groupScores.put("groupC", Arrays.asList(90, 85, 92)); // Same as groupA
            
            session.upsert(dataSet.id("groups1"))
                .bin("scores").setTo(groupScores)
                .execute();
            
            // Find groups with specific score pattern
            RecordStream results = session.update(dataSet.id("groups1"))
                .bin("scores")
                .onMapValue(Arrays.asList(90, 85, 92))
                .getKeys()
                .execute();
            
            System.out.println("Groups with score [90, 85, 92]: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        ClusterDefinition clusterDef = new ClusterDefinition("localhost", 3000);
        
        try (Cluster cluster = clusterDef.connect()) {
            Session session = cluster.createSession(Behavior.DEFAULT);
            DataSet dataSet = DataSet.of("test", "cdt_examples");
            
            System.out.println("=== Example 1: Map Index Range ===");
            example1_MapIndexRange(session, dataSet);
            
            System.out.println("\n=== Example 2: Map Rank Range ===");
            example2_MapRankRange(session, dataSet);
            
            System.out.println("\n=== Example 3: Map Value (Double) ===");
            example3_MapValueDouble(session, dataSet);
            
            System.out.println("\n=== Example 4: Map Value Range (String) ===");
            example4_MapValueRangeString(session, dataSet);
            
            System.out.println("\n=== Example 5: Map Value (Boolean) ===");
            example5_MapValueBoolean(session, dataSet);
            
            System.out.println("\n=== Example 6: Count All Others ===");
            example6_CountAllOthersIndexRange(session, dataSet);
            
            System.out.println("\n=== Example 7: Remove By Rank Range ===");
            example7_RemoveByRankRange(session, dataSet);
            
            System.out.println("\n=== Example 8: Nested Operations ===");
            example8_NestedMapOperations(session, dataSet);
            
            System.out.println("\n=== Example 9: Map Value with List ===");
            example9_MapValueWithList(session, dataSet);
            
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

