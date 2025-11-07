package com.example;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.DefaultRecordMappingFactory;
import com.aerospike.NavigatableRecordStream;
import com.aerospike.RecordResult;
import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.TypeSafeDataSet;
import com.aerospike.client.Log.Level;
import com.aerospike.policy.Behavior;
import com.aerospike.query.SortDir;
import com.aerospike.query.SortProperties;
import com.example.model.Address;
import com.example.model.Customer;
import com.example.model_mappers.AddressMapper;
import com.example.model_mappers.CustomerMapper;

/**
 * Comprehensive example demonstrating NavigatableRecordStream capabilities.
 * 
 * <p>This example showcases the full power of NavigatableRecordStream for in-memory
 * sorting and pagination of query results. Unlike traditional query-based sorting,
 * NavigatableRecordStream allows you to:</p>
 * 
 * <ul>
 *   <li>Sort data after fetching from the database</li>
 *   <li>Change sort order dynamically without re-querying</li>
 *   <li>Navigate forward and backward through pages</li>
 *   <li>Jump to specific pages</li>
 *   <li>Re-iterate through data with different sorting</li>
 *   <li>Sort by multiple columns with different directions</li>
 * </ul>
 * 
 * <h2>Key Concepts Demonstrated</h2>
 * 
 * <h3>1. Basic Conversion to NavigatableRecordStream</h3>
 * <pre>{@code
 * RecordStream results = session.query(dataSet).execute();
 * NavigatableRecordStream navigatable = results.asNavigatableStream();
 * }</pre>
 * 
 * <h3>2. Multi-Column Sorting</h3>
 * <p>Sort by multiple fields using a List of SortProperties:</p>
 * <pre>{@code
 * navigatable = results.asNavigatableStream()
 *     .sortBy(List.of(
 *         SortProperties.ascending("name"),     // Primary sort
 *         SortProperties.descending("age")      // Secondary sort
 *     ));
 * }</pre>
 * 
 * <h3>3. Dynamic Re-sorting</h3>
 * <p>Change sort order without re-querying the database. Each sortBy() call
 * replaces the previous sort criteria:</p>
 * <pre>{@code
 * navigatable.sortBy("age", SortDir.SORT_DESC);  // Replaces previous sort, re-sorts in-memory
 * }</pre>
 * 
 * <h3>4. Pagination Control</h3>
 * <pre>{@code
 * navigatable.pageSize(10);  // Set page size
 * 
 * // Forward pagination
 * while (navigatable.hasMorePages()) {
 *     // Process current page
 * }
 * 
 * // Jump to specific page
 * navigatable.setPageTo(3);  // Jump to page 3
 * 
 * // Reset to beginning
 * navigatable.reset();
 * }</pre>
 * 
 * <h3>5. Converting to Business Objects</h3>
 * <pre>{@code
 * List<Customer> customers = navigatable.toObjectList(customerMapper);
 * }</pre>
 */
public class NavigatableRecordStreamExample {
    
    /**
     * Demonstrates basic usage of NavigatableRecordStream with forward pagination.
     * 
     * <p>This example shows how to:</p>
     * <ul>
     *   <li>Convert a RecordStream to NavigatableRecordStream</li>
     *   <li>Set page size</li>
     *   <li>Sort by a single field</li>
     *   <li>Paginate forward through results</li>
     *   <li>Convert records to business objects</li>
     * </ul>
     */
    private static void example1_BasicPagination(Session session, TypeSafeDataSet<Customer> customerDataSet, 
                                                  CustomerMapper mapper) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  Example 1: Basic Forward Pagination                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        
        // Query all customers and convert to navigatable stream
        NavigatableRecordStream navigatable = session.query(customerDataSet).execute().asNavigatableStream()
            .pageSize(5)
            .sortBy("name");
        
        System.out.println("Total records: " + navigatable.size());
        System.out.println("Page size: 5");
        System.out.println("Total pages: " + navigatable.maxPages());
        
        // Iterate forward through all pages
        while (navigatable.hasMorePages()) {
            System.out.println("\n--- Page " + navigatable.currentPage() + " of " + navigatable.maxPages() + " ---");
            List<Customer> customers = navigatable.toObjectList(mapper);
            customers.forEach(c -> System.out.printf("  %-15s Age: %2d\n", c.getName(), c.getAge()));
        }
    }
    
    /**
     * Demonstrates jumping to specific pages and backward navigation.
     * 
     * <p>This example shows how to:</p>
     * <ul>
     *   <li>Jump directly to a specific page using setPageTo()</li>
     *   <li>Navigate backward by jumping to earlier pages</li>
     *   <li>Reset iteration to the beginning</li>
     * </ul>
     */
    private static void example2_PageJumping(Session session, TypeSafeDataSet<Customer> customerDataSet, 
                                             CustomerMapper mapper) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  Example 2: Page Jumping and Backward Navigation            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        
        RecordStream results = session.query(customerDataSet).execute();
        NavigatableRecordStream navigatable = results.asNavigatableStream()
            .pageSize(5)
            .sortBy("age", SortDir.SORT_DESC);
        
        System.out.println("Total records: " + navigatable.size());
        System.out.println("Total pages: " + navigatable.maxPages());
        
        // Jump to page 3 (if it exists)
        int targetPage = Math.min(3, navigatable.maxPages());
        System.out.println("\nJumping directly to page " + targetPage + ":");
        navigatable.setPageTo(targetPage);
        System.out.println("--- Page " + navigatable.currentPage() + " ---");
        List<Customer> customers = navigatable.toObjectList(mapper);
        customers.forEach(c -> System.out.printf("  %-15s Age: %2d\n", c.getName(), c.getAge()));
        
        // Jump backward to page 1
        System.out.println("\nJumping backward to page 1:");
        navigatable.setPageTo(1);
        System.out.println("--- Page " + navigatable.currentPage() + " ---");
        customers = navigatable.toObjectList(mapper);
        customers.forEach(c -> System.out.printf("  %-15s Age: %2d\n", c.getName(), c.getAge()));
        
        // Jump forward to last page
        int lastPage = navigatable.maxPages();
        System.out.println("\nJumping forward to last page (" + lastPage + "):");
        navigatable.setPageTo(lastPage);
        System.out.println("--- Page " + navigatable.currentPage() + " ---");
        customers = navigatable.toObjectList(mapper);
        customers.forEach(c -> System.out.printf("  %-15s Age: %2d\n", c.getName(), c.getAge()));
        
        // Reset to beginning
        System.out.println("\nResetting to beginning and reading first 2 pages:");
        navigatable.reset();
        int pageCount = 0;
        while (navigatable.hasMorePages() && pageCount < 2) {
            pageCount++;
            System.out.println("--- Page " + navigatable.currentPage() + " ---");
            customers = navigatable.toObjectList(mapper);
            customers.forEach(c -> System.out.printf("  %-15s Age: %2d\n", c.getName(), c.getAge()));
        }
    }
    
    /**
     * Demonstrates multi-column sorting with different sort directions.
     * 
     * <p>This example shows how to:</p>
     * <ul>
     *   <li>Sort by multiple columns using List&lt;SortProperties&gt;</li>
     *   <li>Use different sort directions for each column</li>
     *   <li>Understand sort order precedence (first in list is primary)</li>
     * </ul>
     */
    private static void example3_MultiColumnSorting(Session session, TypeSafeDataSet<Customer> customerDataSet, 
                                                    CustomerMapper mapper) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  Example 3: Multi-Column Sorting                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        
        RecordStream results = session.query(customerDataSet).execute();
        NavigatableRecordStream navigatable = results.asNavigatableStream().pageSize(8);
        
        // Sort by name (ascending) then by age (descending)
        System.out.println("\nSort by: 1) Name (ascending), 2) Age (descending)");
        navigatable.sortBy(List.of(
            SortProperties.ascending("name"),    // Primary sort
            SortProperties.descending("age")     // Secondary sort
        ));
        
        navigatable.hasMorePages();
        List<Customer> customers = navigatable.toObjectList(mapper);
        customers.forEach(c -> System.out.printf("  %-15s Age: %2d\n", c.getName(), c.getAge()));
        
        // Sort by age (ascending) then by name (descending)
        System.out.println("\nSort by: 1) Age (ascending), 2) Name (descending)");
        navigatable.sortBy(List.of(
            SortProperties.ascending("age"),     // Primary sort
            SortProperties.descending("name")    // Secondary sort
        ));
        
        navigatable.hasMorePages();
        customers = navigatable.toObjectList(mapper);
        customers.forEach(c -> System.out.printf("  Age: %2d  %-15s\n", c.getAge(), c.getName()));
    }
    
    /**
     * Demonstrates dynamic re-sorting without re-querying the database.
     * 
     * <p>This is the key advantage of NavigatableRecordStream: you can change
     * the sort order dynamically without going back to the database. This is
     * particularly useful when:</p>
     * <ul>
     *   <li>Sort criteria is determined by user input after initial query</li>
     *   <li>You want to show data in multiple sort orders</li>
     *   <li>Database queries are expensive and you want to minimize them</li>
     * </ul>
     * 
     * <p>This example demonstrates:</p>
     * <ul>
     *   <li>Initial sort by multiple columns (name, then age)</li>
     *   <li>Dynamic re-sort by age only</li>
     *   <li>Another re-sort by name descending</li>
     *   <li>No database queries during re-sorting</li>
     * </ul>
     */
    private static void example4_DynamicResorting(Session session, TypeSafeDataSet<Customer> customerDataSet, 
                                                   CustomerMapper mapper) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  Example 4: Dynamic Re-sorting (No Database Queries)        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        
        // Initial query - this is the only database call
        System.out.println("STEP 1: Query database once");
        RecordStream results = session.query(customerDataSet).limit(20).execute();
        NavigatableRecordStream navigatable = results.asNavigatableStream()
            .pageSize(7);
        
        // First sort: by name then age (multi-column)
        System.out.println("\nSTEP 2: Sort by name (asc), then age (desc) - IN MEMORY");
        navigatable.sortBy(List.of(
            SortProperties.ascending("name"),
            SortProperties.descending("age")
        ));
        
        System.out.println("Page 1:");
        navigatable.hasMorePages();
        List<Customer> customers = navigatable.toObjectList(mapper);
        customers.forEach(c -> System.out.printf("  %-15s Age: %2d\n", c.getName(), c.getAge()));
        
        // Re-sort by age only - NO DATABASE QUERY!
        System.out.println("\nSTEP 3: Re-sort by age (asc) only - IN MEMORY, NO DATABASE QUERY!");
        navigatable.sortBy("age", SortDir.SORT_ASC);  // Replaces previous sort
        
        System.out.println("Page 1 (after re-sort):");
        navigatable.hasMorePages();
        customers = navigatable.toObjectList(mapper);
        customers.forEach(c -> System.out.printf("  Age: %2d  %-15s\n", c.getAge(), c.getName()));
        
        // Re-sort by name descending - STILL NO DATABASE QUERY!
        System.out.println("\nSTEP 4: Re-sort by name (desc) - STILL IN MEMORY!");
        navigatable.sortBy("name", SortDir.SORT_DESC);  // Replaces previous sort
        
        System.out.println("Page 1 (after second re-sort):");
        navigatable.hasMorePages();
        customers = navigatable.toObjectList(mapper);
        customers.forEach(c -> System.out.printf("  %-15s Age: %2d\n", c.getName(), c.getAge()));
        
        System.out.println("\nNote: Only 1 database query was made, all re-sorting happened in memory!");
    }
    
    /**
     * Demonstrates advanced pagination scenarios including:
     * - Starting from a specific page
     * - Re-sorting and restarting pagination
     * - Iterating through a subset of pages
     * 
     * <p>This example shows realistic scenarios like:</p>
     * <ul>
     *   <li>User navigates to page 3, changes sort order, pagination restarts from page 1</li>
     *   <li>Processing only specific pages in a result set</li>
     *   <li>Combining pagination with filtering and sorting</li>
     * </ul>
     */
    private static void example5_AdvancedPaginationScenarios(Session session, TypeSafeDataSet<Customer> customerDataSet, 
                                                             CustomerMapper mapper) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  Example 5: Advanced Pagination Scenarios                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        
        RecordStream results = session.query(customerDataSet).execute();
        NavigatableRecordStream navigatable = results.asNavigatableStream()
            .pageSize(6)
            .sortBy("name");
        
        System.out.println("Total records: " + navigatable.size());
        System.out.println("Total pages: " + navigatable.maxPages());
        
        // Scenario 1: Start from page 2, view a couple pages
        if (navigatable.maxPages() < 2) {
            System.out.println("\nScenario 1: Skipped (not enough pages)");
        } else {
            System.out.println("\nScenario 1: Start viewing from page 2");
            navigatable.setPageTo(2);
        int pagesViewed = 0;
        while (navigatable.hasNext() && pagesViewed < 2) {
            if (pagesViewed == 0 || navigatable.hasMorePages()) {
                if (pagesViewed > 0) {
                    // hasMorePages advances the page
                }
                System.out.println("--- Page " + navigatable.currentPage() + " ---");
                List<Customer> customers = navigatable.toObjectList(mapper);
                customers.forEach(c -> System.out.printf("  %-15s Age: %2d\n", c.getName(), c.getAge()));
                pagesViewed++;
            }
        }
        }
        
        // Scenario 2: User changes sort order - restart from page 1
        System.out.println("\nScenario 2: User changes sort to age (desc) - restart from page 1");
        navigatable.sortBy("age", SortDir.SORT_DESC);  // Replaces previous sort, resets iteration
        
        navigatable.hasMorePages();  // Start at page 1
        System.out.println("--- Page " + navigatable.currentPage() + " (after sort change) ---");
        List<Customer> customers = navigatable.toObjectList(mapper);
        customers.forEach(c -> System.out.printf("  Age: %2d  %-15s\n", c.getAge(), c.getName()));
        
        // Scenario 3: Jump to middle page, then to last page
        if (navigatable.maxPages() < 2) {
            System.out.println("\nScenario 3: Skipped (not enough pages)");
        } else {
            System.out.println("\nScenario 3: Navigate to middle, then jump to last page");
            int midPage = Math.max(1, navigatable.maxPages() / 2);
            navigatable.setPageTo(midPage);
        System.out.println("--- Page " + navigatable.currentPage() + " (middle) ---");
        customers = navigatable.toObjectList(mapper);
        customers.forEach(c -> System.out.printf("  Age: %2d  %-15s\n", c.getAge(), c.getName()));
        
            navigatable.setPageTo(navigatable.maxPages());
            System.out.println("--- Page " + navigatable.currentPage() + " (last) ---");
            customers = navigatable.toObjectList(mapper);
            customers.forEach(c -> System.out.printf("  Age: %2d  %-15s\n", c.getAge(), c.getName()));
        }
    }
    
    /**
     * Demonstrates using NavigatableRecordStream with a limit to control memory usage.
     * 
     * <p>When dealing with large datasets, it's important to limit how much data
     * is loaded into memory. The asNavigatableStream(limit) method allows you to
     * specify a maximum number of records to load.</p>
     * 
     * <p>This example shows:</p>
     * <ul>
     *   <li>Using a limit when converting to NavigatableRecordStream</li>
     *   <li>How the limit affects pagination</li>
     *   <li>Best practices for memory-efficient navigation</li>
     * </ul>
     */
    private static void example6_LimitedRecordStream(Session session, TypeSafeDataSet<Customer> customerDataSet, 
                                                     CustomerMapper mapper) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  Example 6: Memory-Efficient Navigation with Limits         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        
        // Query database but only load first 15 records into navigatable stream
        System.out.println("Querying database and loading first 15 records into memory");
        RecordStream results = session.query(customerDataSet).execute();
        NavigatableRecordStream navigatable = results.asNavigatableStream(15)  // Limit to 15 records
            .pageSize(5)
            .sortBy("age", SortDir.SORT_DESC);
        
        System.out.println("Total records loaded: " + navigatable.size());
        System.out.println("Page size: 5");
        System.out.println("Total pages: " + navigatable.maxPages());
        
        // Show all pages
        while (navigatable.hasMorePages()) {
            System.out.println("\n--- Page " + navigatable.currentPage() + " ---");
            List<Customer> customers = navigatable.toObjectList(mapper);
            customers.forEach(c -> System.out.printf("  Age: %2d  %-15s\n", c.getAge(), c.getName()));
        }
        
        System.out.println("\nNote: Even though database may have more records, we only loaded 15 into memory.");
    }
    
    /**
     * Main method that sets up test data and runs all examples.
     */
    public static void main(String[] args) {
        try (Cluster cluster = new ClusterDefinition("localhost", 3100)
                .usingServicesAlternate()
                .withNativeCredentials("admin", "password123")
                .preferringRacks(1)
                .withLogLevel(Level.INFO)
                .connect()) {
            
            runExamples(cluster);
            
        } catch (Exception e) {
            System.err.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.err.println("║  ERROR: Failed to run examples                              ║");
            System.err.println("╚══════════════════════════════════════════════════════════════╝");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("\nPlease ensure:");
            System.err.println("  1. Aerospike is running on localhost:3100");
            System.err.println("  2. Credentials are correct (admin/password123)");
            System.err.println("  3. Namespace 'test' exists");
        }
    }
    
    private static void runExamples(Cluster cluster) {
            
            CustomerMapper customerMapper = new CustomerMapper();
            cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
                    Customer.class, customerMapper,
                    Address.class, new AddressMapper()
            )));
            
            TypeSafeDataSet<Customer> customerDataSet = TypeSafeDataSet.of("test", "navigatable_demo", Customer.class);
            Session session = cluster.createSession(Behavior.DEFAULT);
            
            // Clean up any existing data
            System.out.println("Setting up test data...");
            for (int i = 1; i <= 30; i++) {
                session.delete(customerDataSet.id(i)).execute();
            }
            
            // Create diverse test data for better demonstration
            List<Customer> customers = List.of(
                // Multiple Alexs with different ages (good for demonstrating secondary sort)
                new Customer(1, "Alex", 27, new Date()),
                new Customer(2, "Alex", 31, new Date()),
                new Customer(3, "Alex", 25, new Date()),
                new Customer(4, "Alex", 29, new Date()),
                
                // Multiple Jordans
                new Customer(5, "Jordan", 36, new Date()),
                new Customer(6, "Jordan", 19, new Date()),
                new Customer(7, "Jordan", 42, new Date()),
                
                // Multiple Tims
                new Customer(8, "Tim", 33, new Date()),
                new Customer(9, "Tim", 27, new Date()),
                new Customer(10, "Tim", 35, new Date()),
                new Customer(11, "Tim", 30, new Date()),
                
                // Various other names
                new Customer(12, "Betty", 27, new Date()),
                new Customer(13, "Bob", 33, new Date()),
                new Customer(14, "Fred", 45, new Date()),
                new Customer(15, "Sam", 24, new Date()),
                new Customer(16, "Zara", 28, new Date()),
                new Customer(17, "Quinn", 52, new Date()),
                new Customer(18, "Uma", 38, new Date()),
                new Customer(19, "Victor", 41, new Date()),
                new Customer(20, "Wendy", 29, new Date()),
                new Customer(21, "Xena", 34, new Date()),
                new Customer(22, "Yolanda", 26, new Date()),
                new Customer(23, "Zachary", 31, new Date()),
                new Customer(24, "Amy", 22, new Date()),
                new Customer(25, "Brian", 48, new Date()),
                new Customer(26, "Chloe", 23, new Date()),
                new Customer(27, "David", 37, new Date()),
                new Customer(28, "Emma", 32, new Date()),
                new Customer(29, "Frank", 44, new Date()),
                new Customer(30, "Grace", 26, new Date())
            );
            
            // Insert test data (using upsert to handle existing records)
            System.out.println("Inserting " + customers.size() + " customer records...");
            RecordStream insertResults = session.upsert(customerDataSet)
                .objects(customers)
                .using(customerMapper)
                .execute();
            
            // Verify inserts
            int successCount = 0;
            int failCount = 0;
            while (insertResults.hasNext()) {
                RecordResult result = insertResults.next();
                if (result.isOk()) {
                    successCount++;
                } else {
                    failCount++;
                    if (failCount <= 3) {  // Show first 3 errors
                        System.err.println("  Insert failed for key " + result.key() + ": " + result.message());
                    }
                }
            }
            
            if (failCount > 0) {
                System.out.println("Test data setup: " + successCount + " succeeded, " + failCount + " failed.");
            } else {
                System.out.println("Test data setup complete. " + successCount + " of " + customers.size() + " customers inserted.");
            }
            
            // Verify data is queryable
            RecordStream verifyResults = session.query(customerDataSet).execute();
            int verifyCount = 0;
            while (verifyResults.hasNext()) {
                verifyResults.next();
                verifyCount++;
            }
            System.out.println("Verification: " + verifyCount + " records found in set.\n");
            
            if (verifyCount == 0) {
                System.err.println("WARNING: No records found after insertion. Examples may fail.");
                System.err.println("Please check that Aerospike is running and accessible.");
                return;
            }
            
            // Run all examples
            example1_BasicPagination(session, customerDataSet, customerMapper);
            example2_PageJumping(session, customerDataSet, customerMapper);
            example3_MultiColumnSorting(session, customerDataSet, customerMapper);
            example4_DynamicResorting(session, customerDataSet, customerMapper);
            example5_AdvancedPaginationScenarios(session, customerDataSet, customerMapper);
            example6_LimitedRecordStream(session, customerDataSet, customerMapper);
            
            System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║  All Examples Completed Successfully!                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            
            // Clean up test data
            System.out.println("\nCleaning up test data...");
            for (int i = 1; i <= 30; i++) {
                session.delete(customerDataSet.id(i)).execute();
            }
            System.out.println("Cleanup complete.");
    }
}

