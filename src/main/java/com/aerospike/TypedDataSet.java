package com.aerospike;

import java.util.ArrayList;
import java.util.List;

/**
 * A dataset bound to a Java entity type {@code T}, used for type-safe keys and object mapping.
 *
 * <p>Create with {@link #of(String, String, Class)} and use {@link #id(String)} (and overloads)
 * to obtain {@link TypedKey} instances. Use with {@link Session#query(TypedDataSet)},
 * {@link Session#insert(TypedDataSet)}, etc.</p>
 *
 * @param <T> the mapped entity type
 */
public class TypedDataSet<T> {
    private final String namespace;
    private final String setName;
    private final Class<T> clazz;

    private TypedDataSet(String namespace, String set, Class<T> clazz) {
        this.clazz = clazz;
        this.namespace = namespace;
        this.setName = set;
    }

    public static <R> TypedDataSet<R> of(String namespace, String set, Class<R> clazz) {
        return new TypedDataSet<>(namespace, set, clazz);
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSet() {
        return setName;
    }

    public TypedKey<T> id(String id) {
        return new TypedKey<>(clazz, namespace, setName, id);
    }

    public TypedKey<T> id(byte[] id) {
        return new TypedKey<>(clazz, namespace, setName, id);
    }

    public TypedKey<T> id(byte[] id, int offset, int length) {
        return new TypedKey<>(clazz, namespace, setName, id, offset, length);
    }

    public TypedKey<T> id(int id) {
        return new TypedKey<>(clazz, namespace, setName, id);
    }

    public TypedKey<T> id(long id) {
        return new TypedKey<>(clazz, namespace, setName, id);
    }

    public TypedKey<T> idForObject(Object object) {
        if (object instanceof String) {
            return id((String) object);
        } else if (object instanceof Byte || object instanceof Short || object instanceof Integer
                || object instanceof Long) {
            return id(((Number) object).longValue());
        } else if (object.getClass().isArray()
                && Byte.class.isAssignableFrom(object.getClass().getComponentType())) {
            return id((byte[]) object);
        }
        throw new IllegalArgumentException("Cannot construct a key for object of type "
                + object.getClass().getSimpleName()
                + ". Only String, int, long and byte[] are supported.");
    }

    public List<TypedKey<T>> ids(List<? extends Object> ids) {
        List<TypedKey<T>> results = new ArrayList<>();
        for (Object id : ids) {
            results.add(idForObject(id));
        }
        return results;
    }

    public List<TypedKey<T>> ids(int... ids) {
        List<TypedKey<T>> results = new ArrayList<>();
        for (int thisId : ids) {
            results.add(id(thisId));
        }
        return results;
    }

    public List<TypedKey<T>> ids(long... ids) {
        List<TypedKey<T>> results = new ArrayList<>();
        for (long thisId : ids) {
            results.add(id(thisId));
        }
        return results;
    }

    public List<TypedKey<T>> ids(String... ids) {
        List<TypedKey<T>> results = new ArrayList<>();
        for (String thisId : ids) {
            results.add(id(thisId));
        }
        return results;
    }

    public List<TypedKey<T>> ids(byte[]... ids) {
        List<TypedKey<T>> results = new ArrayList<>();
        for (byte[] thisId : ids) {
            results.add(id(thisId));
        }
        return results;
    }

    public List<TypedKey<T>> idsFromDigests(byte[]... digests) {
        List<TypedKey<T>> results = new ArrayList<>();
        for (byte[] digest : digests) {
            results.add(new TypedKey<>(clazz, namespace, digest, setName, null));
        }
        return results;
    }

    public TypedKey<T> idFromDigest(byte[] digest) {
        return new TypedKey<>(clazz, namespace, digest, setName, null);
    }

    @Override
    public String toString() {
        return "TypedDataSet [namespace=" + namespace + ", setName=" + setName + ", type=" + clazz.getName() + "]";
    }
}
