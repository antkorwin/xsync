package com.antkorwin.xsync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.antkorwin.commonutils.concurrent.ConcurrentSet;
import com.antkorwin.commonutils.gc.GcUtils;
import org.hibernate.validator.internal.util.ConcurrentReferenceHashMap;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created on 17.06.2018.
 *
 * @author Korovin Anatoliy
 */
public class XMutexFactoryImplTest {

	private static final int TIMEOUT_FOR_PREVENTION_OF_DEADLOCK = 30000;
	private static final int NUMBER_OF_MUTEXES = 100_000;
	private static final int NUMBER_OF_ITERATIONS = NUMBER_OF_MUTEXES * 100;
	private static final String ID_STRING = "c117c526-606e-41b6-8197-1a6ba779f69b";

	@Test
	public void testGetSameMutexFromTwoDifferentInstanceOfEqualsKeys() {
		// Arrange
		XMutexFactoryImpl<UUID> mutexFactory = new XMutexFactoryImpl<>();
		UUID firstId = UUID.fromString(ID_STRING);
		UUID secondId = UUID.fromString(ID_STRING);
		// Check precondition
		assertThat(firstId).isNotSameAs(secondId);
		assertThat(firstId).isEqualTo(secondId);

		// Act
		XMutex<UUID> firstMutex = mutexFactory.getMutex(firstId);
		XMutex<UUID> secondMutex = mutexFactory.getMutex(secondId);

		// Asserts
		assertThat(firstMutex).isNotNull();
		assertThat(secondMutex).isNotNull();
		assertThat(firstMutex).isEqualTo(secondMutex);
		assertThat(firstMutex).isSameAs(secondMutex);
	}

	@Test
	public void testWithRunGCAfterReleaseFirstMutex() throws InterruptedException {
		// Arrange
		XMutexFactoryImpl<UUID> mutexFactory = new XMutexFactoryImpl<>();
		UUID firstId = UUID.fromString(ID_STRING);
		UUID secondId = UUID.fromString(ID_STRING);

		// Act
		XMutex<UUID> firstMutex = mutexFactory.getMutex(firstId);
		int firstHashCode = System.identityHashCode(firstMutex);
		firstMutex = null;

		GcUtils.tryToAllocateAllAvailableMemory();
		GcUtils.fullFinalization();
		mutexFactory.purgeUnreferenced();

		await().atMost(5, TimeUnit.SECONDS)
		       .until(mutexFactory::size, equalTo(0L));
		// Now, the weak-map of mutex factory is empty,
		// because all of the mutex references released
		assertThat(mutexFactory.size()).isEqualTo(0);

		XMutex<UUID> secondMutex = mutexFactory.getMutex(secondId);
		int secondHashCode = System.identityHashCode(secondMutex);

		// Asserts
		assertThat(mutexFactory.size()).isEqualTo(1L);
		assertThat(firstHashCode).isNotEqualTo(secondHashCode);
	}

	@Test
	public void testSizeOfMutexFactoryMap() {
		// Arrange
		XMutexFactoryImpl<UUID> mutexFactory = new XMutexFactoryImpl<>();
		UUID firstId = UUID.fromString(ID_STRING);
		UUID secondId = UUID.fromString(ID_STRING);
		UUID thirdId = UUID.randomUUID();

		// Act
		XMutex<UUID> firstMutex = mutexFactory.getMutex(firstId);
		XMutex<UUID> secondMutex = mutexFactory.getMutex(secondId);
		XMutex<UUID> thirdMutex = mutexFactory.getMutex(thirdId);

		// Asserts
		assertThat(mutexFactory.size()).isEqualTo(2);
	}

	@Test
	public void testEqualityOfReturnedMutexesBySystemIdentityHashCode() {
		// Arrange
		XMutexFactoryImpl<UUID> mutexFactory = new XMutexFactoryImpl<>();
		UUID firstId = UUID.fromString(ID_STRING);
		UUID secondId = UUID.fromString(ID_STRING);
		UUID thirdId = UUID.fromString(ID_STRING);

		// Act
		XMutex<UUID> firstMutex = mutexFactory.getMutex(firstId);
		XMutex<UUID> secondMutex = mutexFactory.getMutex(secondId);
		XMutex<UUID> thirdMutex = mutexFactory.getMutex(thirdId);

		// Assert
		assertThat(System.identityHashCode(firstMutex))
				.isEqualTo(System.identityHashCode(secondMutex));

		assertThat(System.identityHashCode(firstMutex))
				.isEqualTo(System.identityHashCode(thirdMutex));
	}

	@Test
	public void testALotOfHashCodes() {
		// Arrange
		XMutexFactoryImpl<UUID> mutexFactory = new XMutexFactoryImpl<>();
		Set<Integer> setOfHash = ConcurrentSet.getInstance();
		List<UUID> references = Collections.synchronizedList(new ArrayList<>());

		// first key and mutex:
		UUID key = UUID.fromString(ID_STRING);
		XMutex<UUID> firstMutex = mutexFactory.getMutex(key);

		// check that all same keys (by the value) will give
		// the only one instance of mutex
		for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {

			UUID sameId = UUID.fromString(ID_STRING);
			// and now, we save the key-reference in the list,
			// because the GC can delete an unused reference:
			references.add(sameId);

			// Act
			XMutex<UUID> mutex = mutexFactory.getMutex(sameId);
			setOfHash.add(System.identityHashCode(mutex));

			// Assert
			assertThat(mutex).isSameAs(firstMutex);
		}

		// Assertions
		assertThat(mutexFactory.size()).isEqualTo(1);
		assertThat(setOfHash.size()).isEqualTo(1);
		assertThat(references).hasSize(NUMBER_OF_ITERATIONS);
		assertThat(key).isNotNull();
	}

	@Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
	public void testConcurrency() {
		// Arrange
		XMutexFactoryImpl<UUID> mutexFactory = new XMutexFactoryImpl<>();

		List<UUID> ids = IntStream.range(0, NUMBER_OF_MUTEXES)
		                          .boxed()
		                          .map(i -> UUID.randomUUID())
		                          .collect(toList());

		Set<XMutex<UUID>> results = ConcurrentSet.getInstance();

		// Act
		IntStream.range(0, NUMBER_OF_ITERATIONS)
		         .boxed()
		         .parallel()
		         .forEach(i -> {
			         UUID uuid = ids.get(i % NUMBER_OF_MUTEXES);
			         XMutex<UUID> mutex = mutexFactory.getMutex(uuid);
			         results.add(mutex);
		         });

		// Asserts
		await().atMost(10, TimeUnit.SECONDS)
		       .until(results::size, equalTo(NUMBER_OF_MUTEXES));

		assertThat(results).hasSize(NUMBER_OF_MUTEXES);

		await().atMost(10, TimeUnit.SECONDS)
		       .until(mutexFactory::size, equalTo((long) NUMBER_OF_MUTEXES));

		assertThat(mutexFactory.size()).isEqualTo(NUMBER_OF_MUTEXES);
	}

	@Test
	public void testExceptionThrownWhenTryToGetMutexWithNullKey() {
		// Arrange
		XMutexFactoryImpl<UUID> mutexFactory = new XMutexFactoryImpl<>();
		// Act
		try {
			mutexFactory.getMutex(null);
		} catch (Exception e) {
			// Asserts
			assertThat(e).isInstanceOf(NullPointerException.class);
		}
	}


	@Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
	public void testWithCustomConcurrencySettingsWeakAndLevel() {
		// Arrange
		XMutexFactoryImpl<UUID> mutexFactory =
				new XMutexFactoryImpl<>(8, ConcurrentReferenceHashMap.ReferenceType.WEAK);

		List<UUID> ids = IntStream.range(0, NUMBER_OF_MUTEXES)
		                          .boxed()
		                          .map(i -> UUID.randomUUID())
		                          .collect(toList());

		List<XMutex<UUID>> results = Collections.synchronizedList(new ArrayList<>());

		// Act
		IntStream.range(0, NUMBER_OF_ITERATIONS)
		         .boxed()
		         .parallel()
		         .forEach(i -> {
			         UUID uuid = ids.get(i % NUMBER_OF_MUTEXES);
			         XMutex<UUID> mutex = mutexFactory.getMutex(uuid);
			         results.add(mutex);
		         });

		// Asserts
		await().atMost(10, TimeUnit.SECONDS)
		       .until(results::size, equalTo(NUMBER_OF_ITERATIONS));

		Set<XMutex<UUID>> distinctResult = results.stream()
		                                          .distinct()
		                                          .collect(toSet());

		assertThat(distinctResult).hasSize(NUMBER_OF_MUTEXES);
	}
}