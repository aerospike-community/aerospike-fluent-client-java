package com.aerospike.metadata;

public class NamespaceMetadata extends InfoMetadata {
    private static final NamespaceMetadata instance = new NamespaceMetadata();
    private NamespaceMetadata() {
        super("namespaceInfo.yml");
    }
    
    public static NamespaceMetadata getInstance() {
        return instance;
    }
}
