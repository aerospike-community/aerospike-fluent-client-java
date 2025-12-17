package com.example;

import com.aerospike.Cluster;
import com.aerospike.ClusterDefinition;
import com.aerospike.client.Log.Level;

public class TlsTest {
    public static void main(String[] args) {
        ClusterDefinition definition = new ClusterDefinition("localhost", 3101)
                .withTlsConfigOf()
                    .caFile("/Users/tfaulkes/Programming/Aerospike/tls/CA/cacert.pem")
                    .clientCertFile("/Users/tfaulkes/Programming/Aerospike/tls/CA/cert.pem")
                    .clientKeyFile("/Users/tfaulkes/Programming/Aerospike/tls/CA/key.pem")
                    .tlsName("tls1")
                .done()
                .withLogLevel(Level.DEBUG)
                .withNativeCredentials("admin", "admin");
        
        Cluster cluster = definition.connect();
        System.out.println("connected!");
        cluster.close();
    }
}
