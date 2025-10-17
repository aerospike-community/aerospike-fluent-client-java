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
     * Example 10: Use INFINITY for unbounded range end
     */
    public static void example10_InfinityBound(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with numeric scores
            Map<String, Long> scores = new LinkedHashMap<>();
            scores.put("Alice", 75L);
            scores.put("Bob", 82L);
            scores.put("Charlie", 90L);
            scores.put("David", 95L);
            scores.put("Eve", 88L);
            
            session.upsert(dataSet.id("students"))
                .bin("scores").setTo(scores)
                .execute();
            
            // Get all scores from 85 to infinity (all scores >= 85)
            // Using SpecialValue.INFINITY as the upper bound
            RecordStream results = session.update(dataSet.id("students"))
                .bin("scores")
                .onMapValueRange(85L, com.aerospike.SpecialValue.INFINITY)
                .getValues()
                .execute();
            
            System.out.println("Scores >= 85: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 11: Use NULL boundary in key range
     */
    public static void example11_NullBound(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with string keys
            Map<String, String> data = new LinkedHashMap<>();
            data.put("alpha", "first");
            data.put("beta", "second");
            data.put("gamma", "third");
            data.put("delta", "fourth");
            
            session.upsert(dataSet.id("data1"))
                .bin("map").setTo(data)
                .execute();
            
            // Range from NULL to "delta" - gets keys up to (but not including) "delta"
            // Using SpecialValue.NULL as the lower bound
            RecordStream results = session.update(dataSet.id("data1"))
                .bin("map")
                .onMapKeyRange(com.aerospike.SpecialValue.NULL, "delta")
                .getKeys()
                .execute();
            
            System.out.println("Keys from NULL to 'delta': " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 12: Use List as value in relative rank range
     */
    public static void example12_ListAsValueInRelativeRank(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map where values are lists
            Map<String, List<Integer>> listsMap = new LinkedHashMap<>();
            listsMap.put("small", Arrays.asList(1, 2, 3));
            listsMap.put("medium", Arrays.asList(10, 20, 30));
            listsMap.put("large", Arrays.asList(100, 200, 300));
            listsMap.put("huge", Arrays.asList(1000, 2000, 3000));
            
            session.upsert(dataSet.id("lists1"))
                .bin("data").setTo(listsMap)
                .execute();
            
            // Find items with rank relative to a specific list value
            List<Integer> searchValue = Arrays.asList(10, 20, 30);
            
            // Get the next 2 items starting from rank 1 relative to searchValue
            RecordStream results = session.update(dataSet.id("lists1"))
                .bin("data")
                .onMapValueRelativeRankRange(searchValue, 1, 2)
                .getKeys()
                .execute();
            
            System.out.println("Keys relative to list value: " + results);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 13: Combining SpecialValue with regular values
     */
    public static void example13_CombinedSpecialValues(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with various numeric values
            Map<String, Double> metrics = new LinkedHashMap<>();
            metrics.put("cpu", 45.5);
            metrics.put("memory", 67.8);
            metrics.put("disk", 89.2);
            metrics.put("network", 23.4);
            
            session.upsert(dataSet.id("metrics1"))
                .bin("values").setTo(metrics)
                .execute();
            
            // Get all values from 50.0 to INFINITY (all values >= 50.0)
            RecordStream highValues = session.update(dataSet.id("metrics1"))
                .bin("values")
                .onMapValueRange(50.0, com.aerospike.SpecialValue.INFINITY)
                .getKeys()
                .execute();
            
            System.out.println("Metrics with values >= 50.0: " + highValues);
            
            // Get all values from NULL to 50.0 (all values < 50.0)
            RecordStream lowValues = session.update(dataSet.id("metrics1"))
                .bin("values")
                .onMapValueRange(com.aerospike.SpecialValue.NULL, 50.0)
                .getKeys()
                .execute();
            
            System.out.println("Metrics with values < 50.0: " + lowValues);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        ClusterDefinition clusterDef = new ClusterDefinition("localhost", 3100);
        
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
            
            System.out.println("\n=== Example 10: INFINITY Bound ===");
            example10_InfinityBound(session, dataSet);
            
            System.out.println("\n=== Example 11: NULL Bound ===");
            example11_NullBound(session, dataSet);
            
            System.out.println("\n=== Example 12: List as Value in Relative Rank ===");
            example12_ListAsValueInRelativeRank(session, dataSet);
            
            System.out.println("\n=== Example 13: Combined SpecialValues ===");
            example13_CombinedSpecialValues(session, dataSet);
            
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

