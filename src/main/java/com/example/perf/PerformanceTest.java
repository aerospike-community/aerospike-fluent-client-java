package com.example.perf;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.example.perf.Customer.Status;

public class PerformanceTest {
    public static final String HOST = "localhost";
    public static final int PORT = 3100;
    public static final int NUM_RECORDS = 500_000;
    public final byte[] payload;
    
    private final IPerfTest impl;
    public PerformanceTest(IPerfTest impl) {
        this.impl = impl;
        int size = 2048;
        this.payload = new byte[size];
        for (int i = 0; i < size; i++) {
            this.payload[i] = (byte)('A' + (i % 26));
        }
    }

    private Customer createCustomer(long id) {
        Customer reference = new Customer();
        reference.setFirstName("William");
        reference.setLastName("tell");
        reference.setAddrCity("Denver");
        reference.setAddrCountry("United States of America");
        reference.setAddrLine1("1234 SomwhereWithAReallyLongStreetName");
        reference.setAddrState("Colorado");
        reference.setAddrZip("80000-1000");
        reference.setDob(new Date());
        reference.setId(id);
        reference.setPayload(payload);
        reference.setPhoneNum("555-555-1234");
        reference.setStatus(Status.GOLD);
        return reference;
    }
    
    public void insert() throws InterruptedException {
        impl.truncate();
        Thread.sleep(1000);
        int numThreads = 10;
        ExecutorService service = Executors.newFixedThreadPool(numThreads);
        AtomicLong counter = new AtomicLong();
        long now = System.currentTimeMillis();
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    while (true) {
                        long id = counter.incrementAndGet();
                        if (id > NUM_RECORDS) {
                            break;
                        }
                        Customer cust = createCustomer(id);
                        impl.insert(cust);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.DAYS);
        System.out.printf("Using %s: Inserting %,d customers with %,d threads took %,dms\n", impl.getName(), NUM_RECORDS, numThreads, System.currentTimeMillis() - now);
    }
    public static void main(String[] args) throws Exception {
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 2; i ++) {
                PerformanceTest perfTest = new PerformanceTest(
                        i == 0 ? new PerfTestOld(HOST, PORT) 
                                : new PerfTestNew(HOST, PORT));
                
                perfTest.insert();
            }
        }        
    }
}
