package com.example.perf;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;

public class PerfTestOld implements IPerfTest {
    private final IAerospikeClient client; 
    public PerfTestOld(String host, int port) {
        this.client = new AerospikeClient(host, port);
    }
    @Override
    public String getName() {
        return "Old Client";
    }
    
    @Override
    public void truncate() {
        client.truncate(null, "test", "cust", null);
    }
    
    @Override
    public void insert(Customer customer) {
        WritePolicy wp = client.copyWritePolicyDefault();
        wp.recordExistsAction = RecordExistsAction.CREATE_ONLY;
        client.put(null, new Key("test", "cust", customer.getId()), 
                new Bin("firstName", customer.getFirstName()),
                new Bin("lastName", customer.getLastName()),
                new Bin("dob", customer.getDob() == null ? 0 : customer.getDob().getTime()),
                new Bin("id", customer.getId()),
                new Bin("status", customer.getStatus() == null ? "" : customer.getStatus().toString()),
                new Bin("phoneNum", customer.getPhoneNum()),
                new Bin("addrLine1", customer.getAddrLine1()),
                new Bin("addrCity", customer.getAddrCity()),
                new Bin("addrState", customer.getAddrState()),
                new Bin("addrCountry", customer.getAddrCountry()),
                new Bin("addrZip", customer.getAddrZip()),
                new Bin("payload", customer.getPayload()));
    }
    
    @Override
    public void close() {
        this.client.close();
    }
}
