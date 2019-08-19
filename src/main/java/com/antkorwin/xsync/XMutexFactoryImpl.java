package com.antkorwin.xsync;




import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.concurrent.ConcurrentMap;

/**
 * Created on 14.06.2018.
 *
 * @author Korovin Anatoliy
 */
public class XMutexFactoryImpl<KeyT> implements XMutexFactory<KeyT> {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
    private static final ConcurrentReferenceHashMap.ReferenceType DEFAULT_REFERENCE_TYPE =
            ConcurrentReferenceHashMap.ReferenceType.SOFT;

    private final ConcurrentMap<KeyT, XMutex<KeyT>> map;

    /**
     * Create a mutex factory with default settings
     */
    public XMutexFactoryImpl() {
        this.map = new ConcurrentReferenceHashMap<>(DEFAULT_INITIAL_CAPACITY,
                                                    DEFAULT_LOAD_FACTOR,
                                                    DEFAULT_CONCURRENCY_LEVEL,
                                                    DEFAULT_REFERENCE_TYPE);
    }

    /**
     * Creating a mutex factory with custom settings
     *
     * @param concurrencyLevel the expected number of threads
     *                         that will concurrently write to the map
     * @param referenceType    the reference type used for entries (soft or weak)
     */
    public XMutexFactoryImpl(int concurrencyLevel,
                             ConcurrentReferenceHashMap.ReferenceType referenceType) {
        this.map = new ConcurrentReferenceHashMap<>(DEFAULT_INITIAL_CAPACITY,
                                                    DEFAULT_LOAD_FACTOR,
                                                    concurrencyLevel,
                                                    referenceType);
    }

    /**
     * Creates and returns a mutex by the key.
     * If the mutex for this key already exists in the weak-map,
     * then returns the same reference of the mutex.
     */
    @Override
    public XMutex<KeyT> getMutex(KeyT key) {
        return this.map.computeIfAbsent(key, XMutex::new);
    }

    /**
     * @return count of mutexes in this factory.
     */
    @Override
    public long size() {
        return this.map.size();
    }

    /**
     * Remove any entries that have been garbage collected and are no longer referenced.
     * Under normal circumstances garbage collected entries are automatically purged
     * when new items are created by a factory. This method can be used to force a purge.
     */
    public void purgeUnreferenced() {
        ((ConcurrentReferenceHashMap) this.map).purgeUnreferencedEntries();
    }
}
