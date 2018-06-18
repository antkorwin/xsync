package com.antkorwin.xsync;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;

/**
 * Created by Korovin Anatolii on 17.06.2018.
 *
 * @author Korovin Anatolii
 * @version 1.0
 */
public class XMutexFactoryTest {


    @Test
    public void testFirst() {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
        UUID firstId = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");
        UUID secondId = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");
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
    public void testSize() {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
        UUID firstId = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");
        UUID secondId = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");
        UUID thirdId = UUID.randomUUID();

        // Act
        XMutex<UUID> firstMutex = mutexFactory.getMutex(firstId);
        XMutex<UUID> secondMutex = mutexFactory.getMutex(secondId);
        XMutex<UUID> thirdMutex = mutexFactory.getMutex(thirdId);

        // Asserts
        Assertions.assertThat(mutexFactory.size()).isEqualTo(2);
        Assertions.assertThat(firstMutex).isEqualTo(secondMutex);
        Assertions.assertThat(firstMutex).isNotEqualTo(thirdMutex);
    }

    @Test
    public void testWithGC() throws InterruptedException {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
        UUID firstId = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");
        UUID secondId = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");

        // Act
        XMutex<UUID> firstMutex = mutexFactory.getMutex(firstId);
        int fisrtHashCode = System.identityHashCode(firstMutex);
        firstMutex = null;
        System.gc();
        await().atMost(5, TimeUnit.SECONDS)
               .until(mutexFactory::size, Matchers.equalTo(0L));

        XMutex<UUID> secondMutex = mutexFactory.getMutex(secondId);
        int secondHashCode = System.identityHashCode(secondMutex);

        // Asserts
        System.out.println(fisrtHashCode + " " + secondHashCode);
        Assertions.assertThat(mutexFactory.size()).isEqualTo(1L);
        Assertions.assertThat(fisrtHashCode).isNotEqualTo(secondHashCode);
    }

    @Test
    public void testHashCode() {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
        UUID firstId = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");
        UUID secondId = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");
        UUID thirdId = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");

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
        UUID id = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");

        // Act
        Set<Integer> hashs = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
        for (int i = 0; i < 1000; i++) {
            XMutex<UUID> mutex = mutexFactory.getMutex(id);
            hashs.add(System.identityHashCode(mutex));
        }

        Assertions.assertThat(hashs.size()).isEqualTo(1);
    }

    @Test(timeout = 11000)
    public void testConcurrency() throws InterruptedException {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();

        int endExclusive = 1000;

        List<UUID> ids = IntStream.range(0, endExclusive)
                                  .boxed()
                                  .map(i -> UUID.randomUUID())
                                  .collect(Collectors.toList());
        System.out.println("idsSize = " + ids.size());

        Set<XMutex<UUID>> results = Collections
                .newSetFromMap(new ConcurrentHashMap<XMutex<UUID>, Boolean>());

        // Act
//        ExecutorService executorService = Executors.newFixedThreadPool(10);
//        IntStream.range(0, 10000)
//                 .boxed()
//                 .forEach(i -> {
//                     executorService.submit(() -> {
//                         UUID uuid = ids.get(i % endExclusive);
//                         XMutex<UUID> mutex = mutexFactory.getMutex(uuid);
//                         Assertions.assertThat(mutex).isNotNull();
//                         results.add(mutex);
//                     });
//                 });

        IntStream.range(0, 10000)
                 .boxed()
                 .parallel()
                 .forEach(i -> {
                     UUID uuid = ids.get(i % endExclusive);
                     XMutex<UUID> mutex = mutexFactory.getMutex(uuid);
                     Assertions.assertThat(mutex).isNotNull();
                     results.add(mutex);
                 });

        await().atMost(10, TimeUnit.SECONDS)
               .until(results::size, Matchers.equalTo(endExclusive));

        System.out.println("res= " + results.size());
        System.out.println("mf= " + mutexFactory.size());

        // Asserts
        Assertions.assertThat(results).hasSize(endExclusive);
        Assertions.assertThat(mutexFactory.size()).isEqualTo(endExclusive);
    }

    @Test
    public void testWithNullKey() {
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
}