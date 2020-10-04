package com.antkorwin.xsync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.antkorwin.commonutils.concurrent.NonAtomicInt;
import com.antkorwin.commonutils.concurrent.ThreadSleep;
import com.jupiter.tools.stress.test.concurrency.ExecutionMode;
import com.jupiter.tools.stress.test.concurrency.StressTestRunner;

/**
 * Created on 18.06.2018.
 *
 * @author Korovin Anatoliy
 */
public class XSyncTest {

	private static final int TIMEOUT_FOR_PREVENTION_OF_DEADLOCK = 30_000;
	private static final int THREAD_CNT = 10_000_000;
	private static final int ITERATION_COUNT = 100_000;

	@Test
	@Timeout(value = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK, unit = TimeUnit.MILLISECONDS)
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

		assertThat(var.getValue()).isEqualTo(THREAD_CNT);
	}

	@Test
	@Timeout(value = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK, unit = TimeUnit.MILLISECONDS)
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

		assertThat(var.getValue()).isEqualTo(THREAD_CNT);
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
				assertThat(variable.getValue()).isEqualTo(1);
				ThreadSleep.wait(1);
				variable.increment();
				System.out.println("secondThread released a look");
			});
		});

		executorService.awaitTermination(5, TimeUnit.SECONDS);

		// Assert
		assertThat(variable.getValue()).isEqualTo(2);
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
		assertThat(var.getValue()).isEqualTo(THREAD_CNT);
		assertThat(sum).isEqualTo(expectedSum);
	}

	@Test
	public void testThrowExceptionInFunction() throws Exception {
		// Arrange
		XSync<Integer> xSync = new XSync<>();

		// Act
		assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> {
			xSync.evaluate(123, () -> {
				throw new IndexOutOfBoundsException();
			});
		});
	}

	@Test
	public void testLockingWhenTwoInstanceOfXSyncBasedOnSameFactory() {

		// making two synchronization primitives with the same mutex factory
		XMutexFactory<String> factory = new XMutexFactoryImpl<>();
		XSync<String> xsyncFirst = new XSync<>(factory);
		XSync<String> xsyncSecond = new XSync<>(factory);

		String key = "123456789";

		List<Integer> results = new ArrayList<>();
		AtomicInteger counter = new AtomicInteger(0);

		StressTestRunner.test()
		                .iterations(ITERATION_COUNT)
		                .threads(8)
		                .mode(ExecutionMode.EXECUTOR_MODE)
		                .timeout(1, TimeUnit.MINUTES)
		                .run(() -> {
			                XSync<String> sync;
			                // select different xsync for different threads
			                if (counter.incrementAndGet() % 2 == 0) {
				                sync = xsyncFirst;
			                } else {
				                sync = xsyncSecond;
			                }
			                sync.execute(key, () -> results.add(1));
		                });

		long sum = results.stream()
		                  .mapToLong(i -> i)
		                  .sum();

		assertThat(sum).isEqualTo(ITERATION_COUNT);
	}

	@Test
	public void xSyncWithDifferentMutexFactoriesDoesntLock() {

		// making two synchronization primitives with individual mutex factories
		XSync<String> xsyncFirst = new XSync<>();
		XSync<String> xsyncSecond = new XSync<>();

		String key = "123456789";

		List<Integer> results = new ArrayList<>();
		AtomicInteger counter = new AtomicInteger(0);

		StressTestRunner.test()
		                .iterations(ITERATION_COUNT)
		                .threads(8)
		                .mode(ExecutionMode.EXECUTOR_MODE)
		                .timeout(1, TimeUnit.MINUTES)
		                .run(() -> {
			                XSync<String> sync;
			                // select different xsync for different threads
			                if (counter.incrementAndGet() % 2 == 0) {
				                sync = xsyncFirst;
			                } else {
				                sync = xsyncSecond;
			                }
			                sync.execute(key, () -> results.add(1));
		                });

		long sum = results.stream()
		                  .mapToLong(i -> i == null ? 0 : (long)i)
		                  .sum();

		assertThat(sum).isNotEqualTo(ITERATION_COUNT);
	}
}