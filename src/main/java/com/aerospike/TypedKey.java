package com.aerospike;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Value;

public class TypedKey<T> {
    private Key key;
    private Class<T> clazz;
    
    public TypedKey(Class<T> clazz, String namespace, String setName, String key) throws AerospikeException {
        this.key = new Key(namespace, setName, key);
        this.clazz = clazz;
    }
    
    public String getNamespace() {
        return this.key.namespace;
    }
    
    public String getSetName() {
        return this.key.setName;
    }
    
    public Value getUserKey() {
        return this.key.userKey;
    }
    
    public Key getNativeKey() {
        return this.key;
    }
    
    public Class<T> getClazz() {
        return clazz;
    }

    /**
     * Initialize key from namespace, optional set name and user key.
     * The set name and user defined key are converted to a digest before sending to the server.
     * The user key is not used or returned by the server by default. If the user key needs
     * to persist on the server, use one of the following methods:
     * <ul>
     * <li>Set "WritePolicy.sendKey" to true. In this case, the key will be sent to the server for storage on writes
     * and retrieved on multi-record scans and queries.</li>
     * <li>Explicitly store and retrieve the key in a bin.</li>
     * </ul>
     * <p>
     * The key's byte size is limited to the current thread's buffer size (min 8KB).  To store keys &gt; 8KB, do one of the
     * following:
     * <ul>
     * <li>Set once: <pre>{@code ThreadLocalData.DefaultBufferSize = maxKeySize + maxSetNameSize + 1;}</pre></li>
     * <li>Or for every key:
     * <pre>
     * {@code int len = key.length + setName.length() + 1;
     * if (len > ThreadLocalData.getBuffer().length))
     *     ThreadLocalData.resizeBuffer(len);}
     * </pre>
     * </li>
     * </ul>
     *
     * @param namespace             namespace
     * @param setName               optional set name, enter null when set does not exist
     * @param key                   user defined unique identifier within set.
     * @throws AerospikeException   if digest computation fails
     */
    public TypedKey(Class<T> clazz, String namespace, String setName, byte[] key) throws AerospikeException {
        this.key = new Key(namespace, setName, key);
        this.clazz = clazz;
    }

    /**
     * Initialize key from namespace, optional set name and user key.
     * The set name and user defined key are converted to a digest before sending to the server.
     * The user key is not used or returned by the server by default. If the user key needs
     * to persist on the server, use one of the following methods:
     * <ul>
     * <li>Set "WritePolicy.sendKey" to true. In this case, the key will be sent to the server for storage on writes
     * and retrieved on multi-record scans and queries.</li>
     * <li>Explicitly store and retrieve the key in a bin.</li>
     * </ul>
     * <p>
     * The key's byte size is limited to the current thread's buffer size (min 8KB).  To store keys &gt; 8KB, do one of the
     * following:
     * <ul>
     * <li>Set once: <pre>{@code ThreadLocalData.DefaultBufferSize = maxKeySize + maxSetNameSize + 1;}</pre></li>
     * <li>Or for every key:
     * <pre>
     * {@code int len = length + setName.length() + 1;
     * if (len > ThreadLocalData.getBuffer().length))
     *     ThreadLocalData.resizeBuffer(len);}
     * </pre>
     * </li>
     * </ul>
     *
     * @param namespace             namespace
     * @param setName               optional set name, enter null when set does not exist
     * @param key                   user defined unique identifier within set.
     * @param offset                byte array segment offset
     * @param length                byte array segment length
     * @throws AerospikeException   if digest computation fails
     */
    public TypedKey(Class<T> clazz, String namespace, String setName, byte[] key, int offset, int length) throws AerospikeException {
        this.key = new Key(namespace, setName, key);
        this.clazz = clazz;
    }

    /**
     * Initialize key from namespace, optional set name and user key.
     * The set name and user defined key are converted to a digest before sending to the server.
     * The user key is not used or returned by the server by default. If the user key needs
     * to persist on the server, use one of the following methods:
     * <ul>
     * <li>Set "WritePolicy.sendKey" to true. In this case, the key will be sent to the server for storage on writes
     * and retrieved on multi-record scans and queries.</li>
     * <li>Explicitly store and retrieve the key in a bin.</li>
     * </ul>
     *
     * @param namespace             namespace
     * @param setName               optional set name, enter null when set does not exist
     * @param key                   user defined unique identifier within set.
     * @throws AerospikeException   if digest computation fails
     */
    public TypedKey(Class<T> clazz, String namespace, String setName, int key) throws AerospikeException {
        this.key = new Key(namespace, setName, key);
        this.clazz = clazz;
    }

    /**
     * Initialize key from namespace, optional set name and user key.
     * The set name and user defined key are converted to a digest before sending to the server.
     * The user key is not used or returned by the server by default. If the user key needs
     * to persist on the server, use one of the following methods:
     * <ul>
     * <li>Set "WritePolicy.sendKey" to true. In this case, the key will be sent to the server for storage on writes
     * and retrieved on multi-record scans and queries.</li>
     * <li>Explicitly store and retrieve the key in a bin.</li>
     * </ul>
     *
     * @param namespace             namespace
     * @param setName               optional set name, enter null when set does not exist
     * @param key                   user defined unique identifier within set.
     * @throws AerospikeException   if digest computation fails
     */
    public TypedKey(Class<T> clazz, String namespace, String setName, long key) throws AerospikeException {
        this.key = new Key(namespace, setName, key);
        this.clazz = clazz;
    }

    /**
     * Initialize key from namespace, optional set name and user key.
     * The set name and user defined key are converted to a digest before sending to the server.
     * The user key is not used or returned by the server by default. If the user key needs
     * to persist on the server, use one of the following methods:
     * <ul>
     * <li>Set "WritePolicy.sendKey" to true. In this case, the key will be sent to the server for storage on writes
     * and retrieved on multi-record scans and queries.</li>
     * <li>Explicitly store and retrieve the key in a bin.</li>
     * </ul>
     *
     * @param namespace             namespace
     * @param setName               optional set name, enter null when set does not exist
     * @param key                   user defined unique identifier within set.
     * @throws AerospikeException   if digest computation fails
     */
    public TypedKey(Class<T> clazz, String namespace, String setName, Value key) throws AerospikeException {
        this.key = new Key(namespace, setName, key);
        this.clazz = clazz;
    }

    /*
     * Removed Object constructor because the type must be determined using multiple "instanceof"
     * checks.  If the type is not known, java serialization (slow) is used for byte conversion.
     * These two performance penalties make this constructor unsuitable in all cases from
     * a performance perspective.
     *
     * The preferred method when using compound java key objects is to explicitly convert the
     * object to a byte[], String (or other known type) and call the associated Key constructor.
     *
    public Key(String namespace, String setName, Object key) throws AerospikeException {
        this.namespace = namespace;
        this.setName = setName;
        this.userKey = key;
        digest = computeDigest(setName, Value.get(key));
    } */

    /**
     * Initialize key from namespace, digest, optional set name and optional userKey.
     *
     * @param namespace             namespace
     * @param digest                unique server hash value
     * @param setName               optional set name, enter null when set does not exist
     * @param userKey               optional original user key (not hash digest).
     */
    public TypedKey(Class<T> clazz, String namespace, byte[] digest, String setName, Value userKey) {
        this.key = new Key(namespace, digest, setName, userKey);
        this.clazz = clazz;
    }

}
