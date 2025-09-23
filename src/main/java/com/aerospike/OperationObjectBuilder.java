package com.aerospike;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.client.Operation;

public class OperationObjectBuilder<T> {
    private final DataSet dataSet;
    private final List<Operation> ops = new ArrayList<>();
    private final OpType opType;
    private final Session session;
    
    public OperationObjectBuilder(Session session, DataSet dataSet, OpType type) {
        this.dataSet = dataSet;
        this.opType = type;
        this.session = session;
    }
    
    public ObjectBuilder<T> objects(List<T> elements) {
        return new ObjectBuilder<>(this, elements);
    }
    
    public ObjectBuilder<T> objects(T element1, T element2, T ... elements) {
        List<T> elementList = new ArrayList<>();
        elementList.add(element1);
        elementList.add(element2);
        for (T thisElement : elements) {
            elementList.add(thisElement);
        }
        return new ObjectBuilder<>(this, elementList);
    }
    
    public ObjectBuilder<T> object(T element) {
        return new ObjectBuilder<T>(this, element);
    }
    
    public DataSet getDataSet() {
        return dataSet;
    }
    public Session getSession() {
        return session;
    }
    
    public OpType getOpType() {
        return opType;
    }
}
