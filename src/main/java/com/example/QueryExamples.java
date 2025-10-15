package com.example;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.DataSet;
import com.aerospike.DefaultRecordMappingFactory;
import com.aerospike.RecordResult;
import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.TypeSafeDataSet;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Log.Level;
import com.aerospike.client.ResultCode;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.dsl.Dsl;
import com.aerospike.info.classes.NamespaceDetail;
import com.aerospike.info.classes.Sindex;
import com.aerospike.policy.Behavior;
import com.aerospike.query.SortDir;
import com.aerospike.query.SortProperties;
import com.example.model.Address;
import com.example.model.Customer;
import com.example.model_mappers.AddressMapper;
import com.example.model_mappers.CustomerMapper;

public class QueryExamples {
    
    public static void print(RecordStream recordStream) {
        int count = 0;
        while (recordStream.hasNext()) {
            RecordResult key = recordStream.next();
            System.out.printf("%5d - Key: %s, Value: %s\n", (++count), key.key(), key);
        }
    }
    
    public static void main(String[] args) {
        try (Cluster cluster = new ClusterDefinition("localhost", 3100)
                .usingServicesAlternate()
                .withNativeCredentials("admin", "password123")
                .preferringRacks(1)
                .withLogLevel(Level.DEBUG)
                .connect()) {
            
            CustomerMapper customerMapper = new CustomerMapper();

            cluster.setRecordMappingFactory(new DefaultRecordMappingFactory(Map.of(
                        Customer.class, customerMapper,
                        Address.class, new AddressMapper()
                    )
                ));

            Behavior newBehavior = Behavior.DEFAULT.deriveWithChanges("newBehavior", builder -> 
                builder.forAllOperations(ops -> ops
                    .waitForSocketResponseAfterCallFails(Duration.ofSeconds(3))
                )
                .onAvailabilityModeReads(ops -> ops
                    .waitForCallToComplete(Duration.ofMillis(25))
                    .abandonCallAfter(Duration.ofMillis(100))
                    .maximumNumberOfCallAttempts(3)
                )
                .onBatchReads(ops -> ops
                    .maximumNumberOfCallAttempts(7)
                    .allowInlineMemoryAccess(true)
                )
            );
            Behavior childBehavior = newBehavior.deriveWithChanges("child", builder -> 
                builder.onBatchWrites(ops -> ops
                    .allowInlineSsdAccess(true)
                    .maxConcurrentServers(5)
                )
                .onAvailabilityModeReads(ops -> ops
                    .maximumNumberOfCallAttempts(8)
                )
            );
                
            TypeSafeDataSet<Customer> customerDataSet = TypeSafeDataSet.of("test", "person", Customer.class);
//            DataSet customerDataSet = DataSet.of("test", "person");
            
            Session session = cluster.createSession(newBehavior);

            Set<String> namespaces = session.info().namespaces();
            namespaces.forEach(ns -> {
                Optional<NamespaceDetail> details = session.info().namespaceDetails(ns);
                details.ifPresent(System.out::println);
            });
            
            List<Sindex> sindexes = session.info().secondaryIndexes();
            System.out.println(sindexes);
            sindexes.forEach(sindex -> {
                System.out.printf("Secondary index: %s\n", sindex);
                System.out.println("   " + session.info().secondaryIndexDetails(sindex));
            });
            session.upsert(customerDataSet.ids(1,2,3,4,5)).bin("holdings").add(1).execute();
            session.upsert(customerDataSet.ids(1,2,3))
                    .bins("name", "age")
                    .values("Tim", 312)
                    .values("Bob", 25)
                    .values("Jane", 46)
                    .execute();
            
            session.touch(customerDataSet.ids(1,2,3)).execute();
            
            
            DataSet users = DataSet.of("test", "users");
            Key key = users.id("alice");

            RecordStream result = session.upsert(customerDataSet.id(80))
                    .bin("name").setTo("Tim")
                    .bin("age").setTo(342)
                    .execute();
            System.out.println(result.getFirst());
            
            session.upsert(customerDataSet.ids(81, 82))
                    .bin("name").setTo("Tim")
                    .bin("age").setTo(343)
                    .execute();
            session.upsert(customerDataSet.id(83))
                    .bins("name", "age")
                    .values("Tim", 342)
                    .execute();
            session.upsert(customerDataSet.ids(84, 85))
                    .bins("name", "age")
                    .values("Tim", 342)
                    .values("Fred", 37)
                    .execute();
            
            session.upsert(customerDataSet.id(100))
                    .bin("name").setTo("Tim")
                    .bin("age").setTo(312)
                    .bin("dob").setTo(new Date().getTime())
                    .bin("id2").setTo(100)
                    .expireRecordAt(LocalDateTime.of(2030, 1, 1, 0, 0))
                    .execute();

            session.delete(customerDataSet.ids(900, 901, 902, 903, 904, 905)).execute();
            
            session.insertInto(customerDataSet.id(899))
                    .bins("name", "age", "hair", "dob")
                    .values("Tim", 312, "brown", new Date().getTime());
                    
            RecordStream values = session.insertInto(customerDataSet.ids(900, 901, 902, 903, 904,905))
                    .bins("name", "age", "hair", "dob")
                    .values("Tim", 312, "brown", new Date().getTime())
                    .values("Jane", 28, "blonde", new Date().getTime())
                    .values("Bob", 54, "brown", new Date().getTime())
                    .values("Jordan", 45, "red", new Date().getTime())
                    .values("Alex", 67, "blonde", new Date().getTime())
                    .values("Sam", 24, "brown", new Date().getTime())
                    .expireAllRecordsAfter(Duration.ofDays(30))
                    .execute();
            values.forEach(kr -> System.out.printf("%s -> %s\n", kr.key(), kr.recordOrThrow()));

            for (int i = 0; i < 15; i++) {
                session.upsert(customerDataSet.id(i))
                    .bin("name").setTo("Tim-" + i)
                    .bin("age").setTo(312+i)
                    .bin("hair").setTo("brown")
                    .bin("dob").setTo(new Date().getTime())
                    .execute();
                    
                session.upsert(customerDataSet.id(1000+i))
                    .bins("name", "age", "hair", "dob")
                    .values("Tim-"+i, 312+i, "brown", new Date().getTime())
                    .expireRecordAfter(Duration.ofDays(30))
                    .execute();
//              .values("Jane", 28, "blonde", new Date().getTime())
            }
            
            session.delete(customerDataSet.ids(1,2,3,5,7,11,13,17)).execute();
            
            session.delete(customerDataSet.id(102)).execute();
            
            session.insertInto(customerDataSet.id(102))
                .bin("name").setTo("Sue")
                .bin("age").setTo(27)
                .bin("id").setTo(102)
                .bin("dob").setTo(new Date().getTime())
                .execute();
            
            session.update(customerDataSet.id(102))
                .bin("age").setTo(26)
                .execute();
            
            session.delete(customerDataSet.id(102)).execute();
            
            session.upsert(customerDataSet.id(102)) 
                .bin("name").setTo("Sue")
                .bin("age").setTo(27)
                .bin("id").setTo(102)
                .bin("dob").setTo(new Date().getTime())
                .bin("rooms").setTo(Map.of(
                        "room1", Map.of("occupied", false, "rates", Map.of(1, 100, 2, 150, 3, -1)),
                        "room2", Map.of("occupied", true, "rates", Map.of(1, 90, 2, -1, 3, -1)),
                        "room3", Map.of("occupied", false, "rates", Map.of(1, 67, 2, 200, 3, 99)),
                        "room4", Map.of("occupied", true, "rates", Map.of(1, 98, 2, -1, 3, -1)),
                        "room5", Map.of("occupied", false, "rates", Map.of(1, 98, 2, -1, 3, -1)),
                        "room6", Map.of("occupied", true, "rates", Map.of(1, 98, 2, -1, 3, -1))
                    ))
                .bin("rooms2").setTo(Map.of("test", true))
                .execute();
        
            
            RecordStream results = session.upsert(customerDataSet.id(102)) 
                .bin("name").setTo("Bob")
                .bin("age").setTo(30)
                .bin("id").get()
                .bin("dob").setTo(new Date().getTime())
                .bin("rooms").onMapIndex(2).getValues()
                .bin("rooms").onMapKeyRange("room1", "room2").countAllOthers()
                .bin("rooms").onMapKey("room1").getValues()
                .bin("rooms").onMapKeyRange("room1", "room3").count()
                .bin("rooms").onMapKey("room1").onMapKey("rates").onMapKey(1).setTo(110)
                .bin("rooms").onMapKey("room2").mapClear()
                .bin("rooms").onMapKeyRange("room4", "room9").remove()
                .bin("rooms").onMapKey("room1").onMapKey("rates").onMapKey(1).add(5)
                .bin("rooms2").mapClear()
                .bin("rooms2").onMapKey("child", MapOrder.KEY_ORDERED).onMapKey("subChild").setTo(5)
                // TODO: How to insert an element into a list which doesn't exist?
                // TODO: Should complex operations return one item per call?
//                .bin("rooms2").onMapKey("child", MapOrder.KEY_ORDERED).onListIndex(0, ListOrder.UNORDERED, false).listAdd(5)
                .execute();
            System.out.println(results.getFirst());
            System.out.println(session.query(customerDataSet.id(102)).execute().getFirst());
            session.update(customerDataSet.id(102))
                .bin("name").append("-test")
                .bin("age").add(1)
                .execute();
            System.out.println(session.query(customerDataSet.id(102)).execute().getFirst());
            
    
            session.upsert(customerDataSet.id(102))
                .bin("name").setTo("Sue")
                .bin("age").setTo(26)
                .bin("dob").setTo(new Date().getTime())
                .execute();
    
            List<Customer> customers = List.of(
                    new Customer(20, "Jordan", 36, new Date()),
                    new Customer(21, "Alex", 27, new Date()),
                    new Customer(22, "Betty", 27, new Date()),
                    new Customer(23, "Bob", 33, new Date()),
                    new Customer(24, "Fred", 6, new Date()),
                    new Customer(25, "Alex", 28, new Date()),
                    new Customer(26, "Alex", 26, new Date()),
                    new Customer(27, "Jordan", 19, new Date()),
                    new Customer(28, "Gruper", 28, new Date()),
                    new Customer(29, "Bree", 24, new Date()),
                    new Customer(30, "Perry", 44, new Date()),
                    new Customer(31, "Alex", 27, new Date()),
                    new Customer(32, "Betty", 27, new Date()),
                    new Customer(33, "Wilma", 18, new Date()),
                    new Customer(34, "Joran", 82, new Date()),
                    new Customer(35, "Alex", 27, new Date()),
                    new Customer(36, "Fred", 99, new Date()),
                    new Customer(37, "Sydney", 22, new Date()),
                    new Customer(38, "Ita", 99, new Date()),
                    new Customer(39, "Rupert", 83, new Date()),
                    new Customer(40, "Dominic", 53, new Date()),
                    new Customer(41, "Tim", 27, new Date()),
                    new Customer(42, "Tim", 29, new Date()),
                    new Customer(43, "Tim", 31, new Date()),
                    new Customer(44, "Tim", 30, new Date()),
                    new Customer(45, "Tim", 33, new Date()),
                    new Customer(46, "Tim", 35, new Date())
                );
            
            session.insertInto(customerDataSet)
                .objects(customers)
                .using(customerMapper)
                .execute();

            System.out.println("Updating all customers called Tim");
            print(session.update(customerDataSet)
                .where("$.name == 'Tim'")
                .objects(customers)
                .using(customerMapper)
                .execute());

            // Batch partition filter test
            List<Key> keys = customerDataSet.ids(IntStream.rangeClosed(20, 48).toArray());
            System.out.println("Read 25 records, but only those in partitions 0->2047");
            print(session.query(keys)
                    .onPartitionRange(0, 2048)
                    .execute());
            
            System.out.println("Full batch read:");
            print(session.query(keys).execute());
            
            System.out.println("\nBatchRead where name = 'Tim':");
            print(session.query(keys).where("$.name == 'Tim'").execute());
            
            System.out.println("\nBatchRead where name = 'Tim':");
            print(session.query(keys).respondAllKeys().where("$.name == 'Tim'").execute());
            
            System.out.println("\nBatchRead where name = 'Tim':");
            print(session.query(keys).where("$.name == 'Tim'").respondAllKeys().failOnFilteredOut().execute());

//            System.out.println("Read the set, limit 6, test than respondAllKeys() gives a compile error");
//            print(session.query(customerDataSet).respondAllKeys().execute());
//            print(session.query(customerDataSet).failOnFilteredOut().execute());
            

            System.out.println("Read the set, limit 6");
            print(session.query(customerDataSet).limit(6).execute());
            
            List<Key> keyList2 = customerDataSet.ids(20,21,22,23,24,25,26,27);
            RecordStream thisStream = session.update(keyList2)
                   .bin("age").add(1)
                   .execute();
            
            System.out.println("Showing results before guaranteeing execution has finished.");
            print(session.query(keyList2).execute());
            System.out.println("Showing async results");
            print(thisStream);
            System.out.println("Showing results now execution has finished.");
            print(session.query(keyList2).execute());
            

            System.out.printf("Update people in list whose age is < 35 (%s)\n", keyList2);
            print(session.update(keyList2)
                   .bin("age").add(1)
                   .where("$.age < 35")
                   .execute());
            print(session.query(keyList2).execute());

            session.update(keyList2)
                    .bin("age").add(1)
                    .where("$.age < 35")
                    .failOnFilteredOut()
                    .execute();
             print(session.query(keyList2).execute());

            // Query contract:
            // - If a list of ids is provided and there is not "sort" clause, the records in the stream are returned in the order which the ids are specified 
            // - If we're processing the records with notifiers, we cannot also get them back in a stream
            // Should there be a KeyRecord style class with an error code on it and inDoubt? (Similar to Batch Record, but this violates SOLID by being used both for
            // input and output)
            
            // Need a class to turn a resultcode into an exception similar to SQLExceptionTranslator in Spring Boot. These are customizable in `sql-error-codes.xml`
            // file, eg:
            // <error-codes>
            //   <database-product-name>PostgreSQL</database-product-name>
            //   <duplicate-key-codes>23505</duplicate-key-codes>
            //   <data-integrity-violation-codes>23000,23502,23503</data-integrity-violation-codes>
            // </error-codes>

            
            // Threads are really cheap in JDK 21+ so all calls could notify async via a new thread and an ArrayBlockingQueue for example?
//            session.query(customerDataSet)
//                    .onRecordArrival(keyRecord -> System.out.println(keyRecord))
//                    .onRecordError()
//                    .onDone(() -> System.out.println("Done"))
//                    .execute();
            
            System.out.println("\nRead point records - in the same order as the keys, limit to 3");
            print(session.query(customerDataSet.ids(1,3,5,7)).limit(3).execute());

            System.out.println("\nSingle point record");
            print(session.query(customerDataSet.ids(6)).execute());
            
            System.out.println("Read the set, output as stream, limit of 5");
            session.query(customerDataSet).limit(5).execute()
                    .stream()
                    .map(keyRec -> "Name: " + keyRec.recordOrThrow().getString("name"))
                    .forEach(str -> System.out.println(str));

            System.out.println("Read header, point read");
            print(session.query(customerDataSet.id(6)).withNoBins().execute());
            System.out.println("Read header, batch read");
            print(session.query(customerDataSet.ids(6,7,8)).withNoBins().execute());
            System.out.println("Read header, set read");
            print(session.query(customerDataSet).withNoBins().execute());
            
            System.out.println("Read with select bins, point read");
            print(session.query(customerDataSet.ids(6)).readingOnlyBins("name", "age").execute());
            System.out.println("Read with select bins, batch read");
            print(session.query(customerDataSet.ids(6,7,8)).readingOnlyBins("name", "age").execute());
            System.out.println("Read with select bins, set read");
            print(session.query(customerDataSet).readingOnlyBins("name", "age").execute());

            // Throw an exception
            try {
                print(session.query(customerDataSet.ids(6,7,8)).readingOnlyBins("name", "age").withNoBins().execute());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            
            // TODO: Put transaction control into policies
//            session.doInTransaction(txnSession -> {
//                Optional<KeyRecord> result = txnSession.query(customerDataSet.id(1)).execute().getFirst();
//                if (true) {
//                    txnSession.insertInto(customerDataSet.id(3));
//                }
//                txnSession.delete(customerDataSet.id(3));
//                txnSession.insertInto(customerDataSet.id(3)).notInAnyTransaction().execute();
//            });
            
            customers = session.query(customerDataSet.ids(20, 21)).execute().toObjectList(customerMapper);
            System.out.println(customers); 

            customers = session.query(customerDataSet).pageSize(20).execute().toObjectList(customerMapper);
            System.out.println(customers);

            RecordStream rs = session.query(customerDataSet).pageSize(10).execute();
            int page = 0;
            while (rs.hasMorePages()) {
                System.out.println("Page: " + (++page));
                rs.forEach(rec -> System.out.println(rec));
            }
            
            int total = session.query(customerDataSet)
                .execute()
                .stream()
                .mapToInt(kr -> kr.recordOrThrow().getInt("quantity"))
                .sum();
            System.out.println("\n\nSorting customers by Name with a where clause");
            customers = session.query(customerDataSet)
                    .sortReturnedSubsetBy("name", SortDir.SORT_ASC, true)
                    .where("$.name == 'Tim' and $.age > 30")
                    .limit(1000)
                    .execute()
                    .toObjectList(customerMapper);
            for (Customer customer : customers) {
                System.out.println(customer);
            }
            
            System.out.println("End sorting customers by Name with a where clause\n");

            customers = session.query(customerDataSet)
                    .sortReturnedSubsetBy("name", SortDir.SORT_ASC, true)
                    .where(Dsl.stringBin("name").eq("Tim").and(Dsl.longBin("age").gt(30)))
                    .limit(1000)
                    .execute()
                    .toObjectList(customerMapper);
            for (Customer customer : customers) {
                System.out.println(customer);
            }
            
            System.out.println("---- End sort ---");
            

            System.out.println("\n\nSorting customers by Age (desc) then name (asc), paginating by 5 record");
            try (RecordStream recStream = session.query(customerDataSet)
                    .sortReturnedSubsetBy("age", SortDir.SORT_DESC)
                    .sortReturnedSubsetBy("name", SortDir.SORT_ASC, true)
                    .pageSize(5)
                    .limit(13)
                    .execute()) {
                page = 0;
                while (recStream.hasMorePages()) {
                    System.out.println("---- Page " + (++page) + " -----");
                    customers = recStream.toObjectList(customerMapper);
                    customers.forEach(cust -> System.out.println(cust));
                }
                System.out.println("---- End sort ---");
                
                recStream.asResettablePagination().ifPresent(rp -> {
                    System.out.println("--- Setting page to 2 ---");
                    rp.setPageTo(2);
                    recStream.forEach(rec -> System.out.println(rec));
                    System.out.println("--- done with page 2 ---");
                });
                
                // Now re-sort by name
                recStream.asSortable().ifPresent(sort -> {
                    sort.sortBy(new SortProperties("name", SortDir.SORT_ASC, false));
                    System.out.println("Re-sorting records by name");
                    int pageNum = 0;
                    while (recStream.hasMorePages()) {
                        System.out.println("---- Page " + (++pageNum) + " -----");
                        List<Customer> custList = recStream.toObjectList(customerMapper);
                        custList.forEach(cust -> System.out.println(cust));
                    }
                    System.out.println("---- End sort ---");
                });
            }

            // Insert then read back a customer with an address
            System.out.println("\n--- Object mapping test ----");
            Customer sampleCust = new Customer(999, "sample", 456, new Date(), new Address("123 Main St", "Denver", "CO", "USA", "80112"));
            System.out.println("Reference customer: " + sampleCust);
            
            session.delete(customerDataSet.id(999)).execute();
            session.insertInto(customerDataSet).object(sampleCust).execute();
            Customer readCustomer = session.query(customerDataSet.id(999)).execute().toObjectList(customerMapper).get(0);
            System.out.println("Customer read back: " + readCustomer);
            System.out.println("--- End object mapping test ----");
            
            System.out.println("\n--- Generation check test ----");
            
            RecordStream data = session.query(customerDataSet.id(999)).execute();
            data.getFirst().ifPresent(keyRecord -> {
                int generation = keyRecord.recordOrThrow().generation;
                System.out.println("   Read record with generation of " + generation);
                session.update(customerDataSet.id(999))
                        .bin("gen").setTo(generation)
                        .ensureGenerationIs(generation)
                        .execute();
                System.out.println("   First update was successful");
                
                try {
                    // Second update should fail with a generation exception
                    session.update(customerDataSet)
                        .object(readCustomer)
                        .ensureGenerationIs(generation)
                        .execute();
                    System.out.println("   Second update was successful -- this is an error");
                    
                }
                catch (AerospikeException ae) {
                    System.out.println("   Second update failed as expected");
                    System.out.println(ae.getResultCode() == ResultCode.GENERATION_ERROR);
                }
            });
        }
    }
}
