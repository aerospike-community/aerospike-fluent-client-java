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
import com.aerospike.SpecialValue;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.policy.Behavior;

/**
 * Comprehensive examples demonstrating ALL Map CDT operations in the Aerospike Fluent API.
 * 
 * This includes:
 * - Basic map navigation (onMapKey, onMapValue, onMapIndex, onMapRank)
 * - Range operations (onMapKeyRange, onMapValueRange, onMapIndexRange, onMapRankRange)
 * - Relative range operations (onMapKeyRelativeIndexRange, onMapValueRelativeRankRange)
 * - Context paths (nested maps)
 * - Actions (getValues, getKeys, count, remove, etc.)
 * - Type variations:
 *   * Map KEYS: long, String, byte[], double (and SpecialValue for boundaries)
 *   * Map VALUES: long, String, byte[], double, boolean, List, Map (and SpecialValue for boundaries)
 * - Special values (NULL, INFINITY, WILDCARD)
 */
public class ComprehensiveMapExamples {
    
    // ============================================================
    // SECTION 1: Basic Map Navigation - onMapKey
    // ============================================================
    
    /**
     * Example 1: Navigate to a specific map key (long) and get its value
     */
    public static void example1_OnMapKeyLong(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with numeric keys
            Map<Long, String> userIds = new LinkedHashMap<>();
            userIds.put(100L, "Alice");
            userIds.put(200L, "Bob");
            userIds.put(300L, "Charlie");
            
            session.upsert(dataSet.id("users"))
                .bin("userMap").setTo(userIds)
                .execute();
            
            // Get value for key 200
            RecordStream result = session.update(dataSet.id("users"))
                .bin("userMap")
                .onMapKey(200L)
                .getValues()
                .execute();
            
            System.out.println("Value for key 200: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 2: Navigate to a specific map key (String) and get its value
     */
    public static void example2_OnMapKeyString(Session session, DataSet dataSet) {
        try {
            // Setup: Create a map with string keys
            Map<String, Integer> scores = new LinkedHashMap<>();
            scores.put("Alice", 95);
            scores.put("Bob", 87);
            scores.put("Charlie", 92);
            
            session.upsert(dataSet.id("scores"))
                .bin("studentScores").setTo(scores)
                .execute();
            
            // Get score for "Bob"
            RecordStream result = session.update(dataSet.id("scores"))
                .bin("studentScores")
                .onMapKey("Bob")
                .getValues()
                .execute();
            
            System.out.println("Bob's score: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 3: Update a value at a specific map key with MapOrder
     */
    public static void example3_OnMapKeyWithOrder(Session session, DataSet dataSet) {
        try {
            // Setup: Create an empty record
            session.upsert(dataSet.id("config"))
                .bin("settings").setTo(new LinkedHashMap<>())
                .execute();
            
            // Set a value with ordered map creation
            session.update(dataSet.id("config"))
                .bin("settings")
                .onMapKey("maxConnections", MapOrder.KEY_ORDERED)
                .setTo(100)
                .execute();
            
            System.out.println("Set maxConnections to 100 with ordered map");
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 2: Map Value Navigation - onMapValue
    // ============================================================
    
    /**
     * Example 4: Find all keys with a specific value (long)
     */
    public static void example4_OnMapValueLong(Session session, DataSet dataSet) {
        try {
            // Setup: Map where multiple keys might have the same value
            Map<String, Long> departments = new LinkedHashMap<>();
            departments.put("Alice", 100L);  // Department 100
            departments.put("Bob", 200L);    // Department 200
            departments.put("Charlie", 100L); // Department 100
            departments.put("David", 100L);   // Department 100
            
            session.upsert(dataSet.id("employees"))
                .bin("deptMap").setTo(departments)
                .execute();
            
            // Find all employees in department 100
            RecordStream result = session.update(dataSet.id("employees"))
                .bin("deptMap")
                .onMapValue(100L)
                .getKeys()
                .execute();
            
            System.out.println("Employees in dept 100: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 5: Find keys with a specific value (String)
     */
    public static void example5_OnMapValueString(Session session, DataSet dataSet) {
        try {
            // Setup: Map with string values
            Map<Integer, String> statuses = new LinkedHashMap<>();
            statuses.put(1, "active");
            statuses.put(2, "inactive");
            statuses.put(3, "active");
            statuses.put(4, "pending");
            
            session.upsert(dataSet.id("accounts"))
                .bin("statusMap").setTo(statuses)
                .execute();
            
            // Find all accounts with "active" status
            RecordStream result = session.update(dataSet.id("accounts"))
                .bin("statusMap")
                .onMapValue("active")
                .getKeys()
                .execute();
            
            System.out.println("Active accounts: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 6: Find keys with a specific value (double)
     */
    public static void example6_OnMapValueDouble(Session session, DataSet dataSet) {
        try {
            // Setup: Map with double values
            Map<String, Double> prices = new LinkedHashMap<>();
            prices.put("item1", 19.99);
            prices.put("item2", 29.99);
            prices.put("item3", 19.99);
            prices.put("item4", 39.99);
            
            session.upsert(dataSet.id("products"))
                .bin("priceMap").setTo(prices)
                .execute();
            
            // Find all items priced at 19.99
            RecordStream result = session.update(dataSet.id("products"))
                .bin("priceMap")
                .onMapValue(19.99)
                .getKeys()
                .execute();
            
            System.out.println("Items at $19.99: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 7: Find keys with a specific value (boolean)
     */
    public static void example7_OnMapValueBoolean(Session session, DataSet dataSet) {
        try {
            // Setup: Map with boolean values
            Map<String, Boolean> features = new LinkedHashMap<>();
            features.put("ssl", true);
            features.put("encryption", false);
            features.put("compression", true);
            features.put("logging", false);
            
            session.upsert(dataSet.id("config"))
                .bin("featureFlags").setTo(features)
                .execute();
            
            // Find all enabled features
            RecordStream result = session.update(dataSet.id("config"))
                .bin("featureFlags")
                .onMapValue(true)
                .getKeys()
                .execute();
            
            System.out.println("Enabled features: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 8: Find keys with a specific value (List)
     */
    public static void example8_OnMapValueList(Session session, DataSet dataSet) {
        try {
            // Setup: Map where values are lists
            Map<String, List<Integer>> permissions = new LinkedHashMap<>();
            permissions.put("admin", Arrays.asList(1, 2, 3, 4, 5));
            permissions.put("user", Arrays.asList(1, 2));
            permissions.put("guest", Arrays.asList(1));
            permissions.put("moderator", Arrays.asList(1, 2));
            
            session.upsert(dataSet.id("roles"))
                .bin("permMap").setTo(permissions)
                .execute();
            
            // Find roles with specific permission set
            RecordStream result = session.update(dataSet.id("roles"))
                .bin("permMap")
                .onMapValue(Arrays.asList(1, 2))
                .getKeys()
                .execute();
            
            System.out.println("Roles with permissions [1,2]: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 9: Find keys with a specific value (Map)
     */
    public static void example9_OnMapValueMap(Session session, DataSet dataSet) {
        try {
            // Setup: Map where values are themselves maps
            Map<String, Map<String, Object>> users = new LinkedHashMap<>();
            
            Map<String, Object> profile1 = new LinkedHashMap<>();
            profile1.put("age", 25);
            profile1.put("city", "NYC");
            users.put("user1", profile1);
            
            Map<String, Object> profile2 = new LinkedHashMap<>();
            profile2.put("age", 30);
            profile2.put("city", "LA");
            users.put("user2", profile2);
            
            Map<String, Object> profile3 = new LinkedHashMap<>();
            profile3.put("age", 25);
            profile3.put("city", "NYC");
            users.put("user3", profile3);
            
            session.upsert(dataSet.id("userProfiles"))
                .bin("data").setTo(users)
                .execute();
            
            // Find users with matching profile
            RecordStream result = session.update(dataSet.id("userProfiles"))
                .bin("data")
                .onMapValue(profile1)
                .getKeys()
                .execute();
            
            System.out.println("Users with profile1: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 10: Use SpecialValue with onMapValue
     */
    public static void example10_OnMapValueSpecial(Session session, DataSet dataSet) {
        try {
            // This would be used for advanced filtering scenarios
            // where you want to match special boundary values
            RecordStream result = session.update(dataSet.id("data"))
                .bin("map")
                .onMapValue(SpecialValue.NULL)
                .getKeys()
                .execute();
            
            System.out.println("Keys with NULL values: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 3: Map Index Navigation - onMapIndex
    // ============================================================
    
    /**
     * Example 11: Get value at a specific index in an ordered map
     */
    public static void example11_OnMapIndex(Session session, DataSet dataSet) {
        try {
            // Setup: Ordered map
            Map<String, Integer> rankings = new LinkedHashMap<>();
            rankings.put("first", 100);
            rankings.put("second", 90);
            rankings.put("third", 80);
            rankings.put("fourth", 70);
            
            session.upsert(dataSet.id("rankings"))
                .bin("topScores").setTo(rankings)
                .execute();
            
            // Get the value at index 2 (third place)
            RecordStream result = session.update(dataSet.id("rankings"))
                .bin("topScores")
                .onMapIndex(2)
                .getValues()
                .execute();
            
            System.out.println("Value at index 2: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 4: Map Rank Navigation - onMapRank
    // ============================================================
    
    /**
     * Example 12: Get key at a specific rank (sorted by value)
     */
    public static void example12_OnMapRank(Session session, DataSet dataSet) {
        try {
            // Setup: Map with numeric values
            Map<String, Integer> scores = new LinkedHashMap<>();
            scores.put("Alice", 75);
            scores.put("Bob", 92);
            scores.put("Charlie", 88);
            scores.put("David", 95);
            
            session.upsert(dataSet.id("gameScores"))
                .bin("scores").setTo(scores)
                .execute();
            
            // Get the player at rank 2 (third highest score)
            RecordStream result = session.update(dataSet.id("gameScores"))
                .bin("scores")
                .onMapRank(2)
                .getKeys()
                .execute();
            
            System.out.println("Player at rank 2: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 5: Map Key Range Operations - onMapKeyRange
    // ============================================================
    
    /**
     * Example 13: Get values in a key range (long keys)
     */
    public static void example13_OnMapKeyRangeLong(Session session, DataSet dataSet) {
        try {
            // Setup: Map with numeric keys
            Map<Long, String> events = new LinkedHashMap<>();
            events.put(1000L, "Event A");
            events.put(2000L, "Event B");
            events.put(3000L, "Event C");
            events.put(4000L, "Event D");
            events.put(5000L, "Event E");
            
            session.upsert(dataSet.id("timeline"))
                .bin("events").setTo(events)
                .execute();
            
            // Get events with keys between 2000 (inclusive) and 4500 (exclusive)
            RecordStream result = session.update(dataSet.id("timeline"))
                .bin("events")
                .onMapKeyRange(2000L, 4500L)
                .getValues()
                .execute();
            
            System.out.println("Events in range [2000, 4500): " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 14: Get values in a key range (String keys)
     */
    public static void example14_OnMapKeyRangeString(Session session, DataSet dataSet) {
        try {
            // Setup: Map with string keys
            Map<String, Integer> inventory = new LinkedHashMap<>();
            inventory.put("apple", 50);
            inventory.put("banana", 30);
            inventory.put("cherry", 25);
            inventory.put("date", 15);
            inventory.put("elderberry", 40);
            
            session.upsert(dataSet.id("warehouse"))
                .bin("stock").setTo(inventory)
                .execute();
            
            // Get items from "banana" (inclusive) to "date" (exclusive)
            RecordStream result = session.update(dataSet.id("warehouse"))
                .bin("stock")
                .onMapKeyRange("banana", "date")
                .getValues()
                .execute();
            
            System.out.println("Items [banana, date): " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 15: Get values in a key range with NULL boundary
     */
    public static void example15_OnMapKeyRangeWithNull(Session session, DataSet dataSet) {
        try {
            // Setup: Map with string keys
            Map<String, String> data = new LinkedHashMap<>();
            data.put("alpha", "first");
            data.put("beta", "second");
            data.put("gamma", "third");
            data.put("delta", "fourth");
            
            session.upsert(dataSet.id("greek"))
                .bin("letters").setTo(data)
                .execute();
            
            // Get all items from start to "gamma" (exclusive)
            RecordStream result = session.update(dataSet.id("greek"))
                .bin("letters")
                .onMapKeyRange(SpecialValue.NULL, "gamma")
                .getKeys()
                .execute();
            
            System.out.println("Keys from NULL to 'gamma': " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 16: Get values in a key range with INFINITY boundary
     */
    public static void example16_OnMapKeyRangeWithInfinity(Session session, DataSet dataSet) {
        try {
            // Setup: Map with numeric keys
            Map<Long, String> logs = new LinkedHashMap<>();
            logs.put(1000L, "Log A");
            logs.put(2000L, "Log B");
            logs.put(3000L, "Log C");
            logs.put(4000L, "Log D");
            
            session.upsert(dataSet.id("logfile"))
                .bin("entries").setTo(logs)
                .execute();
            
            // Get all logs from 2500 to end
            RecordStream result = session.update(dataSet.id("logfile"))
                .bin("entries")
                .onMapKeyRange(2500L, SpecialValue.INFINITY)
                .getValues()
                .execute();
            
            System.out.println("Logs from 2500 to end: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 6: Map Value Range Operations - onMapValueRange
    // ============================================================
    
    /**
     * Example 17: Get keys with values in a range (long values)
     */
    public static void example17_OnMapValueRangeLong(Session session, DataSet dataSet) {
        try {
            // Setup: Map with numeric values
            Map<String, Long> salaries = new LinkedHashMap<>();
            salaries.put("Alice", 50000L);
            salaries.put("Bob", 75000L);
            salaries.put("Charlie", 60000L);
            salaries.put("David", 90000L);
            salaries.put("Eve", 55000L);
            
            session.upsert(dataSet.id("payroll"))
                .bin("salaries").setTo(salaries)
                .execute();
            
            // Find employees with salaries between 55k and 80k
            RecordStream result = session.update(dataSet.id("payroll"))
                .bin("salaries")
                .onMapValueRange(55000L, 80000L)
                .getKeys()
                .execute();
            
            System.out.println("Employees earning [55k, 80k): " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 18: Get keys with values in a range (double values)
     */
    public static void example18_OnMapValueRangeDouble(Session session, DataSet dataSet) {
        try {
            // Setup: Map with double values
            Map<String, Double> temperatures = new LinkedHashMap<>();
            temperatures.put("NYC", 72.5);
            temperatures.put("LA", 85.3);
            temperatures.put("Chicago", 68.9);
            temperatures.put("Miami", 88.1);
            temperatures.put("Seattle", 65.2);
            
            session.upsert(dataSet.id("weather"))
                .bin("temps").setTo(temperatures)
                .execute();
            
            // Find cities with temps between 70 and 80
            RecordStream result = session.update(dataSet.id("weather"))
                .bin("temps")
                .onMapValueRange(70.0, 80.0)
                .getKeys()
                .execute();
            
            System.out.println("Cities with temps [70, 80): " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 19: Get keys with values >= threshold using INFINITY
     */
    public static void example19_OnMapValueRangeWithInfinity(Session session, DataSet dataSet) {
        try {
            // Setup: Map with numeric values
            Map<String, Integer> points = new LinkedHashMap<>();
            points.put("player1", 150);
            points.put("player2", 230);
            points.put("player3", 190);
            points.put("player4", 420);
            points.put("player5", 85);
            
            session.upsert(dataSet.id("leaderboard"))
                .bin("points").setTo(points)
                .execute();
            
            // Find players with 200+ points
            RecordStream result = session.update(dataSet.id("leaderboard"))
                .bin("points")
                .onMapValueRange(200, SpecialValue.INFINITY)
                .getKeys()
                .execute();
            
            System.out.println("Players with 200+ points: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 7: Map Index Range Operations - onMapIndexRange
    // ============================================================
    
    /**
     * Example 20: Get values by index range
     */
    public static void example20_OnMapIndexRange(Session session, DataSet dataSet) {
        try {
            // Setup: Ordered map
            Map<String, Integer> topScores = new LinkedHashMap<>();
            topScores.put("Alice", 100);
            topScores.put("Bob", 95);
            topScores.put("Charlie", 90);
            topScores.put("David", 85);
            topScores.put("Eve", 80);
            
            session.upsert(dataSet.id("highScores"))
                .bin("top5").setTo(topScores)
                .execute();
            
            // Get entries at indices 1, 2, 3 (Bob, Charlie, David)
            RecordStream result = session.update(dataSet.id("highScores"))
                .bin("top5")
                .onMapIndexRange(1, 3)
                .getValues()
                .execute();
            
            System.out.println("Scores at indices [1, 4): " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 21: Get all values from index to end
     */
    public static void example21_OnMapIndexRangeToEnd(Session session, DataSet dataSet) {
        try {
            // Setup: Ordered map
            Map<String, String> sequence = new LinkedHashMap<>();
            sequence.put("a", "first");
            sequence.put("b", "second");
            sequence.put("c", "third");
            sequence.put("d", "fourth");
            sequence.put("e", "fifth");
            
            session.upsert(dataSet.id("sequence"))
                .bin("items").setTo(sequence)
                .execute();
            
            // Get all items from index 2 to end
            RecordStream result = session.update(dataSet.id("sequence"))
                .bin("items")
                .onMapIndexRange(2)
                .getValues()
                .execute();
            
            System.out.println("Items from index 2 to end: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 8: Map Rank Range Operations - onMapRankRange
    // ============================================================
    
    /**
     * Example 22: Get keys by rank range (sorted by value)
     */
    public static void example22_OnMapRankRange(Session session, DataSet dataSet) {
        try {
            // Setup: Map with various values
            Map<String, Integer> ratings = new LinkedHashMap<>();
            ratings.put("Product A", 4);
            ratings.put("Product B", 5);
            ratings.put("Product C", 3);
            ratings.put("Product D", 5);
            ratings.put("Product E", 2);
            
            session.upsert(dataSet.id("products"))
                .bin("ratings").setTo(ratings)
                .execute();
            
            // Get top 3 products by rank (ranks 2, 3, 4 from bottom)
            RecordStream result = session.update(dataSet.id("products"))
                .bin("ratings")
                .onMapRankRange(2, 3)
                .getKeys()
                .execute();
            
            System.out.println("Products at ranks [2, 5): " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 23: Get top-ranked items (from rank to end)
     */
    public static void example23_OnMapRankRangeToEnd(Session session, DataSet dataSet) {
        try {
            // Setup: Map with numeric values
            Map<String, Long> downloads = new LinkedHashMap<>();
            downloads.put("App A", 1000L);
            downloads.put("App B", 5000L);
            downloads.put("App C", 2500L);
            downloads.put("App D", 8000L);
            downloads.put("App E", 500L);
            
            session.upsert(dataSet.id("appStore"))
                .bin("downloads").setTo(downloads)
                .execute();
            
            // Get top 2 apps (ranks 3 and 4 from bottom = ranks 1 and 0 from top)
            RecordStream result = session.update(dataSet.id("appStore"))
                .bin("downloads")
                .onMapRankRange(3)
                .getKeys()
                .execute();
            
            System.out.println("Top apps from rank 3: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 9: Relative Index Range - onMapKeyRelativeIndexRange
    // ============================================================
    
    /**
     * Example 24: Get items relative to a key by index
     */
    public static void example24_OnMapKeyRelativeIndexRange(Session session, DataSet dataSet) {
        try {
            // Setup: Ordered map
            Map<Long, String> timeline = new LinkedHashMap<>();
            timeline.put(1000L, "Event 1");
            timeline.put(2000L, "Event 2");
            timeline.put(3000L, "Event 3");
            timeline.put(4000L, "Event 4");
            timeline.put(5000L, "Event 5");
            
            session.upsert(dataSet.id("timeline"))
                .bin("events").setTo(timeline)
                .execute();
            
            // Get 2 items starting from 1 index position after key 2000
            RecordStream result = session.update(dataSet.id("timeline"))
                .bin("events")
                .onMapKeyRelativeIndexRange(2000L, 1, 2)
                .getValues()
                .execute();
            
            System.out.println("2 events starting 1 position after 2000: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 25: Get all items after a key
     */
    public static void example25_OnMapKeyRelativeIndexRangeToEnd(Session session, DataSet dataSet) {
        try {
            // Setup: Map with string keys
            Map<String, Integer> chapters = new LinkedHashMap<>();
            chapters.put("chapter1", 20);
            chapters.put("chapter2", 25);
            chapters.put("chapter3", 30);
            chapters.put("chapter4", 22);
            chapters.put("chapter5", 28);
            
            session.upsert(dataSet.id("book"))
                .bin("pages").setTo(chapters)
                .execute();
            
            // Get all chapters starting from 1 position after "chapter2"
            RecordStream result = session.update(dataSet.id("book"))
                .bin("pages")
                .onMapKeyRelativeIndexRange("chapter2", 1)
                .getKeys()
                .execute();
            
            System.out.println("Chapters after chapter2: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 10: Relative Rank Range - onMapValueRelativeRankRange
    // ============================================================
    
    /**
     * Example 27: Get items relative to a value by rank
     */
    public static void example27_OnMapValueRelativeRankRange(Session session, DataSet dataSet) {
        try {
            // Setup: Map with numeric values
            Map<String, Long> scores = new LinkedHashMap<>();
            scores.put("Alice", 500L);
            scores.put("Bob", 750L);
            scores.put("Charlie", 600L);
            scores.put("David", 900L);
            scores.put("Eve", 550L);
            
            session.upsert(dataSet.id("players"))
                .bin("scores").setTo(scores)
                .execute();
            
            // Get 2 players with scores higher than and near 600
            RecordStream result = session.update(dataSet.id("players"))
                .bin("scores")
                .onMapValueRelativeRankRange(600L, 1, 2)
                .getKeys()
                .execute();
            
            System.out.println("2 players with scores just above 600: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 28: Get all items with rank >= value rank
     */
    public static void example28_OnMapValueRelativeRankRangeToEnd(Session session, DataSet dataSet) {
        try {
            // Setup: Map with double values
            Map<String, Double> metrics = new LinkedHashMap<>();
            metrics.put("cpu", 45.2);
            metrics.put("memory", 78.5);
            metrics.put("disk", 92.1);
            metrics.put("network", 34.8);
            metrics.put("load", 56.7);
            
            session.upsert(dataSet.id("system"))
                .bin("usage").setTo(metrics)
                .execute();
            
            // Get all metrics with usage >= 60.0
            RecordStream result = session.update(dataSet.id("system"))
                .bin("usage")
                .onMapValueRelativeRankRange(60.0, 0)
                .getKeys()
                .execute();
            
            System.out.println("Metrics with usage >= 60: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 29: Relative rank range with List as value
     */
    public static void example29_OnMapValueRelativeRankRangeListValue(Session session, DataSet dataSet) {
        try {
            // Setup: Map where values are lists
            Map<String, List<Integer>> data = new LinkedHashMap<>();
            data.put("small", Arrays.asList(1, 2));
            data.put("medium", Arrays.asList(1, 2, 3));
            data.put("large", Arrays.asList(1, 2, 3, 4));
            data.put("tiny", Arrays.asList(1));
            
            session.upsert(dataSet.id("collections"))
                .bin("sizes").setTo(data)
                .execute();
            
            // Get items with lists larger than [1, 2]
            RecordStream result = session.update(dataSet.id("collections"))
                .bin("sizes")
                .onMapValueRelativeRankRange(Arrays.asList(1, 2), 1)
                .getKeys()
                .execute();
            
            System.out.println("Collections larger than [1,2]: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 11: Context Paths - Nested Map Operations
    // ============================================================
    
    /**
     * Example 30: Navigate nested maps with context path
     */
    public static void example30_NestedMapNavigation(Session session, DataSet dataSet) {
        try {
            // Setup: Nested map structure
            Map<String, Object> user = new LinkedHashMap<>();
            
            Map<String, Object> profile = new LinkedHashMap<>();
            Map<String, String> address = new LinkedHashMap<>();
            address.put("street", "123 Main St");
            address.put("city", "New York");
            address.put("zip", "10001");
            profile.put("address", address);
            profile.put("age", 30);
            
            user.put("profile", profile);
            user.put("id", 12345);
            
            session.upsert(dataSet.id("user1"))
                .bin("data").setTo(user)
                .execute();
            
            // Navigate: data -> profile -> address -> city
            RecordStream result = session.update(dataSet.id("user1"))
                .bin("data")
                .onMapKey("profile")
                .onMapKey("address")
                .onMapKey("city")
                .getValues()
                .execute();
            
            System.out.println("User's city: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 31: Update value in nested map
     */
    public static void example31_NestedMapUpdate(Session session, DataSet dataSet) {
        try {
            // Setup: Nested structure
            Map<String, Object> config = new LinkedHashMap<>();
            Map<String, Integer> settings = new LinkedHashMap<>();
            settings.put("maxConnections", 100);
            settings.put("timeout", 30);
            config.put("server", settings);
            
            session.upsert(dataSet.id("config1"))
                .bin("data").setTo(config)
                .execute();
            
            // Update nested value: data -> server -> maxConnections
            session.update(dataSet.id("config1"))
                .bin("data")
                .onMapKey("server")
                .onMapKey("maxConnections")
                .setTo(200)
                .execute();
            
            System.out.println("Updated maxConnections to 200");
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 32: Range operation on nested map
     */
    public static void example32_NestedMapRange(Session session, DataSet dataSet) {
        try {
            // Setup: Nested map with numeric values
            Map<String, Object> store = new LinkedHashMap<>();
            Map<String, Double> prices = new LinkedHashMap<>();
            prices.put("apple", 1.50);
            prices.put("banana", 0.75);
            prices.put("cherry", 2.25);
            prices.put("date", 3.00);
            store.put("fruits", prices);
            
            session.upsert(dataSet.id("store1"))
                .bin("inventory").setTo(store)
                .execute();
            
            // Get fruits priced between 1.0 and 2.5
            RecordStream result = session.update(dataSet.id("store1"))
                .bin("inventory")
                .onMapKey("fruits")
                .onMapValueRange(1.0, 2.5)
                .getKeys()
                .execute();
            
            System.out.println("Fruits priced [1.0, 2.5): " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 33: Multiple levels with index navigation
     */
    public static void example33_MultiLevelIndexNavigation(Session session, DataSet dataSet) {
        try {
            // Setup: Multi-level nested structure
            Map<String, Object> company = new LinkedHashMap<>();
            
            Map<String, Object> departments = new LinkedHashMap<>();
            Map<String, Integer> engineering = new LinkedHashMap<>();
            engineering.put("Alice", 100000);
            engineering.put("Bob", 110000);
            engineering.put("Charlie", 95000);
            departments.put("Engineering", engineering);
            
            company.put("departments", departments);
            
            session.upsert(dataSet.id("company1"))
                .bin("org").setTo(company)
                .execute();
            
            // Navigate to middle salary in engineering
            RecordStream result = session.update(dataSet.id("company1"))
                .bin("org")
                .onMapKey("departments")
                .onMapKey("Engineering")
                .onMapRank(1)  // Middle ranked salary
                .getKeys()
                .execute();
            
            System.out.println("Middle-ranked employee: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // SECTION 12: Action Operations
    // ============================================================
    
    /**
     * Example 34: Count operation
     */
    public static void example34_CountOperation(Session session, DataSet dataSet) {
        try {
            // Setup: Map with various entries
            Map<String, Integer> items = new LinkedHashMap<>();
            items.put("item1", 10);
            items.put("item2", 20);
            items.put("item3", 15);
            items.put("item4", 25);
            items.put("item5", 12);
            
            session.upsert(dataSet.id("inventory"))
                .bin("stock").setTo(items)
                .execute();
            
            // Count items with quantity >= 15
            RecordStream result = session.update(dataSet.id("inventory"))
                .bin("stock")
                .onMapValueRange(15, SpecialValue.INFINITY)
                .count()
                .execute();
            
            System.out.println("Items with qty >= 15: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 35: Remove operation
     */
    public static void example35_RemoveOperation(Session session, DataSet dataSet) {
        try {
            // Setup: Map
            Map<String, String> cache = new LinkedHashMap<>();
            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.put("key3", "value3");
            cache.put("key4", "value4");
            
            session.upsert(dataSet.id("cache1"))
                .bin("data").setTo(cache)
                .execute();
            
            // Remove keys in range
            RecordStream result = session.update(dataSet.id("cache1"))
                .bin("data")
                .onMapKeyRange("key2", "key4")
                .remove()
                .execute();
            
            System.out.println("Removed keys: " + result);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 36: GetKeys vs GetValues vs GetEntries
     */
    public static void example36_DifferentGetOperations(Session session, DataSet dataSet) {
        try {
            // Setup: Map
            Map<String, Integer> data = new LinkedHashMap<>();
            data.put("A", 10);
            data.put("B", 20);
            data.put("C", 30);
            
            session.upsert(dataSet.id("sample"))
                .bin("map").setTo(data)
                .execute();
            
            // Get only keys
            RecordStream keys = session.update(dataSet.id("sample"))
                .bin("map")
                .onMapValueRange(15, SpecialValue.INFINITY)
                .getKeys()
                .execute();
            System.out.println("Keys: " + keys);
            
            // Get only values
            RecordStream values = session.update(dataSet.id("sample"))
                .bin("map")
                .onMapValueRange(15, SpecialValue.INFINITY)
                .getValues()
                .execute();
            System.out.println("Values: " + values);
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 37: Clear entire map
     */
    public static void example37_MapClear(Session session, DataSet dataSet) {
        try {
            // Setup: Map
            Map<String, String> temp = new LinkedHashMap<>();
            temp.put("a", "1");
            temp.put("b", "2");
            
            session.upsert(dataSet.id("temp"))
                .bin("data").setTo(temp)
                .execute();
            
            // Clear the map
            session.update(dataSet.id("temp"))
                .bin("data")
                .mapClear()
                .execute();
            
            System.out.println("Map cleared");
            
        } catch (AerospikeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example 38: Get map size
     */
    public static void example38_MapSize(Session session, DataSet dataSet) {
        try {
            // Setup: Map
            Map<String, Integer> items = new LinkedHashMap<>();
            items.put("x", 1);
            items.put("y", 2);
            items.put("z", 3);
            
            session.upsert(dataSet.id("items"))
                .bin("data").setTo(items)
                .execute();
            
            // Get size
            RecordStream result = session.update(dataSet.id("items"))
                .bin("data")
                .mapSize()
                .execute();
            
            System.out.println("Map size: " + result);
            
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
            DataSet dataSet = DataSet.of("test", "comprehensive_map_examples");
            
            System.out.println("=== SECTION 1: Basic Map Navigation - onMapKey ===");
            example1_OnMapKeyLong(session, dataSet);
            example2_OnMapKeyString(session, dataSet);
            example3_OnMapKeyWithOrder(session, dataSet);
            
            System.out.println("\n=== SECTION 2: Map Value Navigation - onMapValue ===");
            example4_OnMapValueLong(session, dataSet);
            example5_OnMapValueString(session, dataSet);
            example6_OnMapValueDouble(session, dataSet);
            example7_OnMapValueBoolean(session, dataSet);
            example8_OnMapValueList(session, dataSet);
            example9_OnMapValueMap(session, dataSet);
            example10_OnMapValueSpecial(session, dataSet);
            
            System.out.println("\n=== SECTION 3: Map Index Navigation - onMapIndex ===");
            example11_OnMapIndex(session, dataSet);
            
            System.out.println("\n=== SECTION 4: Map Rank Navigation - onMapRank ===");
            example12_OnMapRank(session, dataSet);
            
            System.out.println("\n=== SECTION 5: Map Key Range - onMapKeyRange ===");
            example13_OnMapKeyRangeLong(session, dataSet);
            example14_OnMapKeyRangeString(session, dataSet);
            example15_OnMapKeyRangeWithNull(session, dataSet);
            example16_OnMapKeyRangeWithInfinity(session, dataSet);
            
            System.out.println("\n=== SECTION 6: Map Value Range - onMapValueRange ===");
            example17_OnMapValueRangeLong(session, dataSet);
            example18_OnMapValueRangeDouble(session, dataSet);
            example19_OnMapValueRangeWithInfinity(session, dataSet);
            
            System.out.println("\n=== SECTION 7: Map Index Range - onMapIndexRange ===");
            example20_OnMapIndexRange(session, dataSet);
            example21_OnMapIndexRangeToEnd(session, dataSet);
            
            System.out.println("\n=== SECTION 8: Map Rank Range - onMapRankRange ===");
            example22_OnMapRankRange(session, dataSet);
            example23_OnMapRankRangeToEnd(session, dataSet);
            
            System.out.println("\n=== SECTION 9: Relative Index Range - onMapKeyRelativeIndexRange ===");
            example24_OnMapKeyRelativeIndexRange(session, dataSet);
            example25_OnMapKeyRelativeIndexRangeToEnd(session, dataSet);
            
            System.out.println("\n=== SECTION 10: Relative Rank Range - onMapValueRelativeRankRange ===");
            example27_OnMapValueRelativeRankRange(session, dataSet);
            example28_OnMapValueRelativeRankRangeToEnd(session, dataSet);
            example29_OnMapValueRelativeRankRangeListValue(session, dataSet);
            
            System.out.println("\n=== SECTION 11: Context Paths - Nested Maps ===");
            example30_NestedMapNavigation(session, dataSet);
            example31_NestedMapUpdate(session, dataSet);
            example32_NestedMapRange(session, dataSet);
            example33_MultiLevelIndexNavigation(session, dataSet);
            
            System.out.println("\n=== SECTION 12: Action Operations ===");
            example34_CountOperation(session, dataSet);
            example35_RemoveOperation(session, dataSet);
            example36_DifferentGetOperations(session, dataSet);
            example37_MapClear(session, dataSet);
            example38_MapSize(session, dataSet);
            
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

