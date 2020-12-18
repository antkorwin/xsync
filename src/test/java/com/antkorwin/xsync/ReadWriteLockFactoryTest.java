package com.antkorwin.xsync;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hibernate.validator.internal.util.ConcurrentReferenceHashMap;
import org.junit.Test;

import com.antkorwin.commonutils.concurrent.ConcurrentSet;
import com.antkorwin.commonutils.gc.GcUtils;

/**
 * Created on 22.06.2020.
 * 
 * @author Carlos Macasaet
 */
public class ReadWriteLockFactoryTest {

	private static final int TIMEOUT_FOR_PREVENTION_OF_DEADLOCK = 30000;
	private static final int NUMBER_OF_LOCKS = 100_000;
	private static final int NUMBER_OF_ITERATIONS = NUMBER_OF_LOCKS * 100;
	private static final String ID_STRING = "c117c526-606e-41b6-8197-1a6ba779f69b";

	@Test
	public void testGetSameLockFromTwoDifferentInstanceOfEqualsKeys() {
		// Arrange
		ReadWriteLockFactory<UUID> factory = new ReadWriteLockFactory<>();
		UUID firstId = UUID.fromString(ID_STRING);
		UUID secondId = UUID.fromString(ID_STRING);
		// Check precondition
		assertThat(firstId).isNotSameAs(secondId);
		assertThat(firstId).isEqualTo(secondId);

		// Act
		ReadWriteLock firstLock = factory.getReadWriteLock(firstId);
		ReadWriteLock secondLock = factory.getReadWriteLock(secondId);

		// Asserts
		assertThat(firstLock).isNotNull();
		assertThat(secondLock).isNotNull();
		assertThat(firstLock).isEqualTo(secondLock);
		assertThat(firstLock).isSameAs(secondLock);
	}

	@Test
	public void testWithRunGCAfterReleaseFirstLock() throws InterruptedException {
        // Arrange
        ConcurrentReferenceHashMap<UUID, ReadWriteLock> map = new ConcurrentReferenceHashMap<>(16, 0.75f, 16,
                ConcurrentReferenceHashMap.ReferenceType.WEAK, ConcurrentReferenceHashMap.ReferenceType.WEAK, null);
	    ReadWriteLockFactory<UUID> factory = new ReadWriteLockFactory<>(map, ReentrantReadWriteLock::new);
		UUID firstId = UUID.fromString(ID_STRING);
		UUID secondId = UUID.fromString(ID_STRING);

		// Act
		ReadWriteLock firstLock = factory.getReadWriteLock(firstId);
		int firstHashCode = System.identityHashCode(firstLock);
		firstLock = null;

		GcUtils.tryToAllocateAllAvailableMemory();
		GcUtils.fullFinalization();
		map.purgeStaleEntries();

		await().atMost(5, TimeUnit.SECONDS)
		       .until(factory::size, equalTo(0L));
		// Now, the weak-map of lock factory is empty,
		// because all of the lock references released
		assertThat(factory.size()).isEqualTo(0);

		ReadWriteLock secondLock = factory.getReadWriteLock(secondId);
		int secondHashCode = System.identityHashCode(secondLock);

		// Asserts
		assertThat(factory.size()).isEqualTo(1L);
		assertThat(firstHashCode).isNotEqualTo(secondHashCode);
	}

	@Test
	public void testSizeOfLockFactoryMap() {
		// Arrange
	    ReadWriteLockFactory<UUID> factory = new ReadWriteLockFactory<>();
		UUID firstId = UUID.fromString(ID_STRING);
		UUID secondId = UUID.fromString(ID_STRING);
		UUID thirdId = UUID.randomUUID();
		Collection<ReadWriteLock> set = new HashSet<>();

		// Act
		set.add(factory.getReadWriteLock(firstId));
		set.add(factory.getReadWriteLock(secondId));
		set.add(factory.getReadWriteLock(thirdId));

		// Asserts
		assertThat(factory.size()).isEqualTo(2);
	}

	@Test
	public void testEqualityOfReturnedLocksBySystemIdentityHashCode() {
		// Arrange
	    ReadWriteLockFactory<UUID> factory = new ReadWriteLockFactory<>();
		UUID firstId = UUID.fromString(ID_STRING);
		UUID secondId = UUID.fromString(ID_STRING);
		UUID thirdId = UUID.fromString(ID_STRING);

		// Act
		ReadWriteLock firstLock = factory.getReadWriteLock(firstId);
		ReadWriteLock secondLock = factory.getReadWriteLock(secondId);
		ReadWriteLock thirdLock = factory.getReadWriteLock(thirdId);

		// Assert
		assertThat(System.identityHashCode(firstLock))
				.isEqualTo(System.identityHashCode(secondLock));

		assertThat(System.identityHashCode(firstLock))
				.isEqualTo(System.identityHashCode(thirdLock));
	}

	@Test
	public void testALotOfHashCodes() {
		// Arrange
	    ReadWriteLockFactory<UUID> factory = new ReadWriteLockFactory<>();
		Set<Integer> setOfHash = ConcurrentSet.getInstance();
		List<UUID> references = Collections.synchronizedList(new ArrayList<>());

		// first key and lock:
		UUID key = UUID.fromString(ID_STRING);
		ReadWriteLock firstLock = factory.getReadWriteLock(key);

		// check that all same keys (by the value) will give
		// the only one instance of lock
		for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {

			UUID sameId = UUID.fromString(ID_STRING);
			// and now, we save the key-reference in the list,
			// because the GC can delete an unused reference:
			references.add(sameId);

			// Act
			ReadWriteLock lock = factory.getReadWriteLock(sameId);
			setOfHash.add(System.identityHashCode(lock));

			// Assert
			assertThat(lock).isSameAs(firstLock);
		}

		// Assertions
		assertThat(factory.size()).isEqualTo(1);
		assertThat(setOfHash.size()).isEqualTo(1);
		assertThat(references).hasSize(NUMBER_OF_ITERATIONS);
		assertThat(key).isNotNull();
	}

	@Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
	public void testConcurrency() {
		// Arrange
	    ReadWriteLockFactory<UUID> factory = new ReadWriteLockFactory<>();

		List<UUID> ids = IntStream.range(0, NUMBER_OF_LOCKS)
		                          .boxed()
		                          .map(i -> UUID.randomUUID())
		                          .collect(toList());

		Set<ReadWriteLock> results = ConcurrentSet.getInstance();

		// Act
		IntStream.range(0, NUMBER_OF_ITERATIONS)
		         .boxed()
		         .parallel()
		         .forEach(i -> {
			         UUID uuid = ids.get(i % NUMBER_OF_LOCKS);
			         ReadWriteLock lock = factory.getReadWriteLock(uuid);
			         results.add(lock);
		         });

		// Asserts
		await().atMost(10, TimeUnit.SECONDS)
		       .until(results::size, equalTo(NUMBER_OF_LOCKS));

		assertThat(results).hasSize(NUMBER_OF_LOCKS);

		await().atMost(10, TimeUnit.SECONDS)
		       .until(factory::size, equalTo((long) NUMBER_OF_LOCKS));

		assertThat(factory.size()).isEqualTo(NUMBER_OF_LOCKS);
	}

	@Test
	public void testExceptionThrownWhenTryToGetLockWithNullKey() {
		// Arrange
	    ReadWriteLockFactory<UUID> factory = new ReadWriteLockFactory<>();

		// Act / Asserts
	    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> factory.getReadWriteLock(null));
	}

	@Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
	public void testWithCustomConcurrencySettingsWeakAndLevel() {
		// Arrange
        ReadWriteLockFactory<UUID> factory = new ReadWriteLockFactory<>(8,
                ConcurrentReferenceHashMap.ReferenceType.WEAK);

		List<UUID> ids = IntStream.range(0, NUMBER_OF_LOCKS)
		                          .boxed()
		                          .map(i -> UUID.randomUUID())
		                          .collect(toList());

		List<ReadWriteLock> results = Collections.synchronizedList(new ArrayList<>());

		// Act
		IntStream.range(0, NUMBER_OF_ITERATIONS)
		         .boxed()
		         .parallel()
		         .forEach(i -> {
			         UUID uuid = ids.get(i % NUMBER_OF_LOCKS);
			         ReadWriteLock lock = factory.getReadWriteLock(uuid);
			         results.add(lock);
		         });

		// Asserts
		await().atMost(10, TimeUnit.SECONDS)
		       .until(results::size, equalTo(NUMBER_OF_ITERATIONS));

		Set<ReadWriteLock> distinctResult = results.stream()
		                                          .distinct()
		                                          .collect(toSet());

		assertThat(distinctResult).hasSize(NUMBER_OF_LOCKS);
	}

	@Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
    public void testSupportForReentrantImplementations() {
        final Supplier<ReentrantReadWriteLock> unfairSupplier = ReentrantReadWriteLock::new;
        final Supplier<ReentrantReadWriteLock> fairSupplier = () -> new ReentrantReadWriteLock(true);
        Stream.of(unfairSupplier, fairSupplier).forEach(this::testSupportForSupplier);
    }

	public void testSupportForSupplier(final Supplier<ReentrantReadWriteLock> supplier) {
        // Arrange
        final ReadWriteLockFactory<UUID> factory = new ReadWriteLockFactory<>(supplier);
        final Map<UUID, Integer> cache = new ConcurrentHashMap<>();
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();

        // Act
        Stream.of(first, second, first, second).parallel().forEach(id -> {
            final ReadWriteLock lock = factory.getReadWriteLock(id);
            lock.readLock().lock();
            try {
                if (!cache.containsKey(id)) {
                    lock.readLock().unlock();
                    lock.writeLock().lock();
                    try {
                        if (!cache.containsKey(id)) {
                            cache.put(id, 0);
                        }
                    } finally {
                        lock.readLock().lock();
                        lock.writeLock().unlock();
                    }
                }

                // Asserts
                assertThat(cache.get(id)).isEqualTo(0);
            } finally {
                lock.readLock().unlock();
            }
        });
	}

	@Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
	public void testSupportForNonReentrantImplementation() {
	    // Arrange
	    final ReadWriteLockFactory<UUID> factory = new ReadWriteLockFactory<UUID>(() -> new StampedLock().asReadWriteLock());
	    final Map<UUID, Integer> cache = new ConcurrentHashMap<>();
	    final UUID id = UUID.fromString(ID_STRING);
        final Runnable readTask = () -> {
            final ReadWriteLock lock = factory.getReadWriteLock(id);
            lock.readLock().lock();
            try {
                cache.containsKey(id);
            } finally {
                lock.readLock().unlock();
            }
        };
	    final Runnable writeTask = () -> {
	        final ReadWriteLock lock = factory.getReadWriteLock(id);
	        lock.writeLock().lock();
	        try {
	            final int count = cache.containsKey(id) ? cache.get(id) : 0;
	            cache.put(id, count + 1);
	        } finally {
	            lock.writeLock().unlock();
	        }
	    };
	    final ExecutorService executor = Executors.newFixedThreadPool(2);

	    // Act
	    executor.execute(readTask);
	    executor.execute(readTask);
	    executor.execute(writeTask);
	    executor.execute(readTask);
	    executor.execute(writeTask);
	    executor.execute(readTask);
	    executor.execute(readTask);
	    executor.shutdown();
	    await().atMost(1, TimeUnit.SECONDS).until(executor::isTerminated);

	    // Asserts
	    assertThat(cache.containsKey(id)).isTrue();
	    assertThat(cache.get(id)).isEqualTo(2);
	}

}