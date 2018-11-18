package com.antkorwin.xsync;

import com.antkorwin.commonutils.concurrent.NonAtomicInt;
import com.antkorwin.commonutils.concurrent.ThreadSleep;
import com.antkorwin.commonutils.validation.GuardCheck;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created on 18.06.2018.
 *
 * @author Korovin Anatoliy
 */
public class XSyncTest {

    private static final int TIMEOUT_FOR_PREVENTION_OF_DEADLOCK = 30_000;
    private static final int THREAD_CNT = 10_000_000;

    @Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
    public void testSyncBySingleKeyInConcurrency() {
        // Arrange
        XSync<UUID> xsync = new XSync<>();
        UUID id = UUID.randomUUID();
        NonAtomicInt var = new NonAtomicInt(0);

        // Act
        IntStream.range(0, THREAD_CNT)
                 .boxed()
                 .parallel()
                 .forEach(j -> xsync.execute(id, var::increment));

        // Asserts
        await().atMost(5, TimeUnit.SECONDS)
               .until(var::getValue, equalTo(THREAD_CNT));

        Assertions.assertThat(var.getValue()).isEqualTo(THREAD_CNT);
    }

    @Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
    public void testSyncBySameValueOfKeyInConcurrency() {
        // Arrange
        XSync<UUID> xsync = new XSync<>();
        String id = UUID.randomUUID().toString();
        NonAtomicInt var = new NonAtomicInt(0);

        // Act
        IntStream.range(0, THREAD_CNT)
                 .boxed()
                 .parallel()
                 .forEach(j -> xsync.execute(UUID.fromString(id), var::increment));

        // Asserts
        await().atMost(15, TimeUnit.SECONDS)
               .until(var::getValue, equalTo(THREAD_CNT));

        Assertions.assertThat(var.getValue()).isEqualTo(THREAD_CNT);
    }

    @Test
    public void testLock() throws InterruptedException {
        // Arrange
        NonAtomicInt variable = new NonAtomicInt(0);
        XSync<String> xSync = new XSync<>();

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // Act
        executorService.submit(() -> {
            System.out.println("firstThread started.");
            xSync.execute(new String("key"), () -> {
                System.out.println("firstThread took a lock");
                ThreadSleep.wait(2);
                variable.increment();
                System.out.println("firstThread released a look");
            });
        });

        executorService.submit(() -> {
            ThreadSleep.wait(1);
            System.out.println("secondThread started.");
            xSync.execute(new String("key"), () -> {
                System.out.println("secondThread took a lock");

                // Assert
                Assertions.assertThat(variable.getValue()).isEqualTo(1);
                ThreadSleep.wait(1);
                variable.increment();
                System.out.println("secondThread released a look");
            });
        });

        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // Assert
        Assertions.assertThat(variable.getValue()).isEqualTo(2);
    }

    @Test
    public void testEvaluateSupplier() {
        // Arrange
        XSync<UUID> xsync = new XSync<>();
        String id = UUID.randomUUID().toString();
        NonAtomicInt var = new NonAtomicInt(0);
        long expectedSum = ((long) (THREAD_CNT + 1) * THREAD_CNT) / 2;

        // Act
        long sum = IntStream.range(0, THREAD_CNT)
                            .boxed()
                            .parallel()
                            .mapToLong(i -> xsync.evaluate(UUID.fromString(id), var::increment))
                            .sum();

        // Asserts
        Assertions.assertThat(var.getValue()).isEqualTo(THREAD_CNT);
        Assertions.assertThat(sum).isEqualTo(expectedSum);
    }

    //TODO:
//    @Test
//    public void testWorkWithoutLockingWhenTwoInstanceSXync(){
//        XSync<String> xsyncFirst = new XSync<>();
//        XSync<String> xsyncSecond = new XSync<>();
//
//        xsyncFirst
//    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testThrowExceptionInFunction() throws Exception {
        // Arrange
        XSync<Integer> xSync = new XSync<>();
        // Act
        xSync.evaluate(123, ()-> {
            throw new IndexOutOfBoundsException();
        });
    }
}