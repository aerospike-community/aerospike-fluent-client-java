package com.aerospike;

import com.aerospike.client.Bin;
import com.aerospike.client.Value;


@Deprecated
public class MultiBinBuilder {
    private final OperationBuilder opBuilder;
    private final String binName;
    private final String[] binNames;
    
    public MultiBinBuilder(OperationBuilder opBuilder, String binName, String ... binNames) {
        this.opBuilder = opBuilder;
        this.binName = binName;
        this.binNames = binNames;
    }
    
    public AbstractOperationBuilder values(Object ... objects) {
        if (objects.length != 1+binNames.length) {
            throw new IllegalArgumentException(String.format(
                    "When calling '.values(...)' to specify the values for multiple bins,"
                    + " the number of values must match the number of bins specified in the '.bins(...)' call."
                    + " This call specified %,d bins, but supplied %,d values.",
                    1+binNames.length,
                    objects.length));
        }
        for (int i = 0; i < objects.length; i++) {
            String binName = i == 0 ? this.binName : this.binNames[i-1];
            opBuilder.setTo(new Bin(binName, Value.get(objects[i])));
        }
        return opBuilder;
    }
    
    public AbstractOperationBuilder setTo(String value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    public AbstractOperationBuilder setTo(int value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    public AbstractOperationBuilder setTo(long value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    public AbstractOperationBuilder setTo(float value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    public AbstractOperationBuilder setTo(double value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    public AbstractOperationBuilder setTo(boolean value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
    public AbstractOperationBuilder setTo(byte[] value) {
        return opBuilder.setTo(new Bin(binName, value));
    }
}