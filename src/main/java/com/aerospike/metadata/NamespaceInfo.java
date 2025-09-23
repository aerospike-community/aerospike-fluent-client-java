package com.aerospike.metadata;

import java.util.HashMap;
import java.util.Map;

import com.aerospike.ASNode;
import com.aerospike.Session;
import com.aerospike.client.Info;

public class NamespaceInfo extends InfoData {
    private final String namespace;
    public NamespaceInfo(String namespace, ASNode node, Session session) {
        this(namespace, node, session, 0);
    }
    public NamespaceInfo(String namespace, ASNode node, Session session, int refreshIntervalSecs) {
        super(NamespaceMetadata.getInstance(), node, session, refreshIntervalSecs);
        this.namespace = namespace;
        super.completeInitialization();
    }
    
    protected Map<String, String> getInfoForNode(ASNode node) {
        String results = Info.request(node.getNode(), "namespace/" + this.namespace);
        String[] items = results.split(";");
        Map<String, String> resultMap = new HashMap<>();
        for (String item : items) {
            String[] keyAndValue = item.split("=");
            resultMap.put(keyAndValue[0], keyAndValue[1]);
        }
        return resultMap;
    }

    public boolean isStopWrites() {
        return super.getBoolean("stop_writes");
    }
}
