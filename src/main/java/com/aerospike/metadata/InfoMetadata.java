package com.aerospike.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.aerospike.client.Log;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class InfoMetadata {
    protected static enum ConfigMerge {
        allow_different,
        sum;
    }
    protected static enum MetricsMerge {
        sum,
        average,
        or,
        and,
        unknown,
        unanimous
    }
    private static class Metadata {
        @JsonProperty("config")
        private Map<String, ConfigMerge> config;
        @JsonProperty("metrics")
        private Map<String, MetricsMerge> metrics;
        @Override
        public String toString() {
            return String.format("{%d configs, %d metrics}", config == null ? 0 : config.size(), metrics == null ? 0 : metrics.size());
        }
    }
    private final Metadata configMetadata;
    protected InfoMetadata(String fileName) {
        Metadata metadata;
        try {
            InputStream stream = InfoData.class.getClassLoader().getResourceAsStream(fileName);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            metadata = objectMapper.readValue(stream, Metadata.class);
            Log.debug("Config loaded: " + metadata);
        }
        catch (IOException ioe) {
            Log.error("Error reading " + fileName);
            ioe.printStackTrace();
            metadata = null;
        }
        this.configMetadata = metadata;
    }
    
    protected boolean successfullyLoaded() {
        return configMetadata == null;
    }
    
    protected boolean merge(String name, boolean currentValue, boolean newValue) {
        MetricsMerge metricsMerge = configMetadata.metrics.get(name);
        if (metricsMerge != null) {
            switch (metricsMerge) {
            case and:
                return currentValue && newValue;
            case or:
                return currentValue || newValue;
            default:
                throw new IllegalStateException("Metric " + name + " was passed booleans but the merge policy is " + metricsMerge);
            }
        }
        else {
            // Must be a config value
            ConfigMerge configMerge = configMetadata.config.get(name);
            if (configMerge == null) {
                throw new IllegalArgumentException("Unknown config or metric: " + name);
            }
            switch (configMerge) {
            case allow_different:
                // TODO:
                return newValue;
            }
        }
        return currentValue;
    }
    
    protected long merge(String name, long currentValue, long newValue) {
        MetricsMerge metricsMerge = configMetadata.metrics.get(name);
        if (metricsMerge != null) {
            switch (metricsMerge) {
            case sum:
            case average:
                // TODO: Average needs a sample count
                return currentValue + newValue;
            default:
                throw new IllegalStateException("Metric " + name + " was passed booleans but the merge policy is " + metricsMerge);
            }
        }
        return currentValue;
    }
    

}
