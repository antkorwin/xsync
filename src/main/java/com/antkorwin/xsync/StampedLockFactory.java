package com.antkorwin.xsync;


import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.StampedLock;

import org.hibernate.validator.internal.util.ConcurrentReferenceHashMap;


/**
 * Created on 22.06.2020.
 * <p>
 * The factory of locks, based on {@link ConcurrentReferenceHashMap}.
 * Use this if you require the performance characteristics of
 * {@link StampedLock StampedLocks} over other synchronization
 * mechanisms.
 * </p>
 * 
 * @author Carlos Macasaet
 */
public class StampedLockFactory<KeyT> {

	private static final int DEFAULT_INITIAL_CAPACITY = 16;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
	private static final ConcurrentReferenceHashMap.ReferenceType DEFAULT_REFERENCE_TYPE =
			ConcurrentReferenceHashMap.ReferenceType.WEAK;

	private final ConcurrentMap<KeyT, StampedLock> map;

	/**
	 * Create a factory with default settings
	 */
	public StampedLockFactory() {
		this(new ConcurrentReferenceHashMap<>(DEFAULT_INITIAL_CAPACITY,
		                                            DEFAULT_LOAD_FACTOR,
		                                            DEFAULT_CONCURRENCY_LEVEL,
		                                            DEFAULT_REFERENCE_TYPE,
		                                            DEFAULT_REFERENCE_TYPE,
		                                            null));
	}

	/**
	 * Creating a factory with custom settings
	 *
	 * @param concurrencyLevel the expected number of threads
	 *                         that will concurrently write to the map
	 * @param referenceType    the reference type used for entries (soft or weak)
	 */
	public StampedLockFactory(int concurrencyLevel,
	                         ConcurrentReferenceHashMap.ReferenceType referenceType) {
		this(new ConcurrentReferenceHashMap<>(DEFAULT_INITIAL_CAPACITY,
		                                            DEFAULT_LOAD_FACTOR,
		                                            concurrencyLevel,
		                                            referenceType,
		                                            referenceType,
		                                            null));
	}

	protected StampedLockFactory(final ConcurrentMap<KeyT, StampedLock> map) {
	    Objects.requireNonNull(map, "map must be provided");
	    this.map = map;
	}

    /**
     * Creates and returns a lock by the key. If the lock for this key
     * already exists(or use by another thread), then returns the same
     * reference of the lock.
     *
     * @param key object which used as a key for synchronization
     * @return lock instance created for this key
     */
    public StampedLock getLock(KeyT key) {
        return this.map.computeIfAbsent(key, k -> new StampedLock());
    }

	/**
	 * @return count of locks in this factory.
	 */
	public long size() {
		return this.map.size();
	}

}