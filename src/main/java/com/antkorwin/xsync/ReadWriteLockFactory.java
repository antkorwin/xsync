package com.antkorwin.xsync;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import org.hibernate.validator.internal.util.ConcurrentReferenceHashMap;

/**
 * Created on 22.06.2020.
 * <p>
 * The factory of {@link ReadWriteLock ReadWriteLocks}, based on
 * {@link ConcurrentReferenceHashMap}. Use this if you need to allow
 * multiple concurrent readers but only one writer. You can also control
 * whether or not to support reentrancy and whether or not the lock should
 * be fair. Depending on your usage patterns, you may opt to back the
 * locks with a {@link ReentrantReadWriteLock} (default) or a
 * {@link StampedLock}. Note, for a stamped lock, the optimistic locking
 * model and lock type modification are not supported.
 * </p>
 * 
 * @author Carlos Macasaet
 */
public class ReadWriteLockFactory<KeyT> {

	private static final int DEFAULT_INITIAL_CAPACITY = 16;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
	private static final ConcurrentReferenceHashMap.ReferenceType DEFAULT_REFERENCE_TYPE =
			ConcurrentReferenceHashMap.ReferenceType.WEAK;

	private final ConcurrentMap<KeyT, ReadWriteLock> map;
	private final Supplier<? extends ReadWriteLock> lockSupplier;

	/**
	 * Create a lock factory with default settings
	 */
	public ReadWriteLockFactory() {
        this(DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * Create a lock factory with default settings and a custom lock generator.
	 *
	 * @param lockSupplier a method for creating new {@link ReadWriteLock} instances.
	 */
	public ReadWriteLockFactory(final Supplier<? extends ReadWriteLock> lockSupplier) {
        this(DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE, lockSupplier);
	}

	/**
	 * Create a lock factory with custom settings
	 *
	 * @param concurrencyLevel the expected number of threads
	 *                         that will concurrently write to the map
	 * @param referenceType    the reference type used for entries (soft or weak)
	 */
    public ReadWriteLockFactory(int concurrencyLevel, ConcurrentReferenceHashMap.ReferenceType referenceType) {
        this(concurrencyLevel, referenceType, ReentrantReadWriteLock::new);
    }

	/**
	 * Create a lock factory with custom settings
	 * 
	 * @param concurrencyLevel the expected number of threads
     *                         that will concurrently write to the map
	 * @param referenceType    the reference type used for entries (soft or weak)
	 * @param lockSupplier     a method for creating ReadWriteLock instances
	 */
    public ReadWriteLockFactory(final int concurrencyLevel, final ConcurrentReferenceHashMap.ReferenceType referenceType,
            final Supplier<? extends ReadWriteLock> lockSupplier) {
	    this(new ConcurrentReferenceHashMap<>(DEFAULT_INITIAL_CAPACITY,
                DEFAULT_LOAD_FACTOR,
                concurrencyLevel,
                referenceType,
                referenceType,
                null),
	            lockSupplier);
	}

    protected ReadWriteLockFactory(final ConcurrentMap<KeyT, ReadWriteLock> map,
            final Supplier<? extends ReadWriteLock> lockSupplier) {
        Objects.requireNonNull(map, "map must be provided");
        Objects.requireNonNull(lockSupplier, "lockSupplier must be provided");
        this.map = map;
        this.lockSupplier = lockSupplier;
    }

	/**
	 * Creates and returns a lock by the key.
	 * If the lock for this key already exists in the weak-map,
	 * then returns the same reference of the lock.
	 * 
	 * @param key object which used as a key for synchronization
     * @return lock instance created for this key
	 */
	public ReadWriteLock getReadWriteLock(KeyT key) {
		return this.map.computeIfAbsent(key, k -> lockSupplier.get());
	}

	/**
	 * @return count of locks in this factory.
	 */
	public long size() {
		return this.map.size();
	}

}