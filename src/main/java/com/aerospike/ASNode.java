package com.aerospike;

import java.util.HashMap;
import java.util.Map;

import com.aerospike.client.Info;
import com.aerospike.client.cluster.Node;
import com.aerospike.metadata.NamespaceInfo;

public class ASNode {
    private final Node node;
    protected ASNode(Node node) {
        this.node = node;
    }
    
    public String getName() {
        return this.node.getName();
    }
    
    public Node getNode() {
        return this.node;
    }
    
    protected Map<String, String> getNamespaceInfoMap(String namespace, int refreshPeriod) {
        String results = Info.request(this.node, "namespace/" + namespace);
        String[] items = results.split(";");
        Map<String, String> resultMap = new HashMap<>();
        for (String item : items) {
            String[] keyAndValue = item.split("=");
            resultMap.put(keyAndValue[0], keyAndValue[1]);
        }
        return resultMap;
    }
    
    public NamespaceInfo getNamespaceInfo(String namespace) {
        return new NamespaceInfo(namespace, this, null);
    }
}
