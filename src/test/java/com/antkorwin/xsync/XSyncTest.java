package com.antkorwin.xsync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;

/**
 * Created by Korovin Anatolii on 18.06.2018.
 *
 * @author Korovin Anatolii
 * @version 1.0
 */
public class XSyncTest {


    public static final int THREAD_CNT = 10000000;
    public static final int ITERATION_CNT = 100;

    volatile NonAtomicInt var = new NonAtomicInt(0);

    @Test
    public void testSync() throws InterruptedException {
        // Arrange
        XSync<UUID> xsync = new XSync<>();
        UUID firstId = UUID.fromString("c117c526-606e-41b6-8197-1a6ba779f69b");

        // Act
        XMutexFactory<UUID> factory = new XMutexFactory<>();
        Set<XMutex<UUID>> results = Collections.newSetFromMap(new ConcurrentHashMap<XMutex<UUID>, Boolean>());
        Set<Integer> resHash = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

        IntStream.range(0, THREAD_CNT)
                 .boxed()
                 .parallel()
                 .forEach(j -> {
//                     xsync.execute(firstId, () -> {
//                         var.incr();
//                     });
                     XMutex<UUID> mutex = factory.getMutex(firstId);
                     results.add(mutex);
                     resHash.add(System.identityHashCode(mutex));
                     synchronized (mutex) {
                         var.incr();
                     }
                 });
        // Asserts
        Thread.sleep(3000);
        System.out.println("weak= " + factory.weakHashMap.size());
        System.out.println("mutex set= " + results.size());
        System.out.println("hash= " + resHash.size());
        System.out.println("var= " + var.getValue());

        Assertions.assertThat(results).hasSize(1);
        Assertions.assertThat(var.getValue()).isEqualTo(THREAD_CNT);
        Assertions.assertThat(resHash).hasSize(1);
    }

    @Test(timeout = 10000)
    public void testXSync() throws InterruptedException {
        // Arrange
        XSync<UUID> xsync = new XSync<>();
        UUID id = UUID.randomUUID();

        // Act
        IntStream.range(0, THREAD_CNT)
                 .boxed()
                 .parallel()
                 .forEach(j -> {
                     xsync.execute(id, () -> {
                         var.incr();
                     });
                 });

        // Asserts
        await().atMost(5, TimeUnit.SECONDS)
               .until(var::getValue, Matchers.greaterThanOrEqualTo(THREAD_CNT));

        Assertions.assertThat(var.getValue()).isEqualTo(THREAD_CNT);
    }

    @Getter
    @AllArgsConstructor
    private class NonAtomicInt {
        private int value;

        public int incr() {
            return value++;
        }
    }
}