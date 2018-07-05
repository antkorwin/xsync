package com.antkorwin.xsync;

import com.antkorwin.commonutils.concurrent.ConcurrentSet;
import com.antkorwin.commonutils.gc.GcUtils;
import com.antkorwin.xsync.springframework.util.ConcurrentReferenceHashMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created on 17.06.2018.
 *
 * @author Korovin Anatoliy
 */
public class XMutexFactoryTest {

    private static final int TIMEOUT_FOR_PREVENTION_OF_DEADLOCK = 30000;
    private static final int NUMBER_OF_MUTEXES = 100000;
    private static final int NUMBER_OF_ITERATIONS = NUMBER_OF_MUTEXES * 100;
    private static final String ID_STRING = "c117c526-606e-41b6-8197-1a6ba779f69b";

    @Test
    public void testGetSameMutexFromTwoDifferentInstanceOfEqualsKeys() {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
        UUID firstId = UUID.fromString(ID_STRING);
        UUID secondId = UUID.fromString(ID_STRING);
        // Check precondition
        Assertions.assertThat(firstId != secondId).isTrue();
        Assertions.assertThat(firstId).isEqualTo(secondId);

        // Act
        XMutex<UUID> firstMutex = mutexFactory.getMutex(firstId);
        XMutex<UUID> secondMutex = mutexFactory.getMutex(secondId);

        // Asserts
        Assertions.assertThat(firstMutex).isNotNull();
        Assertions.assertThat(secondMutex).isNotNull();
        Assertions.assertThat(firstMutex).isEqualTo(secondMutex);
        Assertions.assertThat(firstMutex == secondMutex).isTrue();
    }

    @Test
    public void testWithRunGCAfterReleaseFirstMutex() throws InterruptedException {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
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
        Assertions.assertThat(mutexFactory.size()).isEqualTo(0);

        XMutex<UUID> secondMutex = mutexFactory.getMutex(secondId);
        int secondHashCode = System.identityHashCode(secondMutex);

        // Asserts
        Assertions.assertThat(mutexFactory.size()).isEqualTo(1L);
        Assertions.assertThat(firstHashCode).isNotEqualTo(secondHashCode);
    }

    @Test
    public void testSizeOfMutexFactoryMap() {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
        UUID firstId = UUID.fromString(ID_STRING);
        UUID secondId = UUID.fromString(ID_STRING);
        UUID thirdId = UUID.randomUUID();

        // Act
        XMutex<UUID> firstMutex = mutexFactory.getMutex(firstId);
        XMutex<UUID> secondMutex = mutexFactory.getMutex(secondId);
        XMutex<UUID> thirdMutex = mutexFactory.getMutex(thirdId);

        // Asserts
        Assertions.assertThat(mutexFactory.size()).isEqualTo(2);
    }

    @Test
    public void testEqualityOfReturnedMutexesBySystemIdentityHashCode() {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
        UUID firstId = UUID.fromString(ID_STRING);
        UUID secondId = UUID.fromString(ID_STRING);
        UUID thirdId = UUID.fromString(ID_STRING);

        // Act
        XMutex<UUID> firstMutex = mutexFactory.getMutex(firstId);
        XMutex<UUID> secondMutex = mutexFactory.getMutex(secondId);
        XMutex<UUID> thirdMutex = mutexFactory.getMutex(thirdId);

        // Assert
        Assertions.assertThat(System.identityHashCode(firstMutex))
                  .isEqualTo(System.identityHashCode(secondMutex));

        Assertions.assertThat(System.identityHashCode(firstMutex))
                  .isEqualTo(System.identityHashCode(thirdMutex));
    }

    @Test
    public void testALotOfHashCodes() {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
        Set<Integer> setOfHash = ConcurrentSet.getInstance();
        Set<XMutex<UUID>> resultReferences = ConcurrentSet.getInstance();
        UUID key = UUID.fromString(ID_STRING);

        XMutex<UUID> firstMutex = mutexFactory.getMutex(key);

        // Act
        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            XMutex<UUID> mutex = mutexFactory.getMutex(UUID.fromString(ID_STRING));
            setOfHash.add(System.identityHashCode(mutex));
            // and now, we save reference in the set of mutexes,
            // because the GC can delete a unused references:
            resultReferences.add(mutex);
            // Assert
            Assertions.assertThat(mutex == firstMutex).isTrue();
        }

        // Assertions
        Assertions.assertThat(setOfHash.size()).isEqualTo(1);
        Assertions.assertThat(resultReferences).hasSize(1);
    }

    @Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
    public void testConcurrency() {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();

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

        Assertions.assertThat(results).hasSize(NUMBER_OF_MUTEXES);

        await().atMost(10, TimeUnit.SECONDS)
               .until(mutexFactory::size, equalTo((long) NUMBER_OF_MUTEXES));

        Assertions.assertThat(mutexFactory.size()).isEqualTo(NUMBER_OF_MUTEXES);
    }

    @Test
    public void testExceptionThrownWhenTryToGetMutexWithNullKey() {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
        // Act
        try {
            mutexFactory.getMutex(null);
        } catch (Exception e) {
            // Asserts
            Assertions.assertThat(e)
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessage("The KEY of mutex must not be null.");
        }
    }


    @Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
    public void testWithCustomConcurrencySettingsWeakAndLevel() {
        // Arrange
        XMutexFactory<UUID> mutexFactory =
                new XMutexFactory<>(8,
                                    ConcurrentReferenceHashMap.ReferenceType.WEAK);

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

        Assertions.assertThat(distinctResult).hasSize(NUMBER_OF_MUTEXES);
    }
}