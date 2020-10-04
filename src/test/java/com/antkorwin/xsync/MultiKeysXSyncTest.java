package com.antkorwin.xsync;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.jupiter.tools.stress.test.concurrency.ExecutionMode;
import com.jupiter.tools.stress.test.concurrency.StressTestRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on 27/12/2019
 * <p>
 * multiple keys test cases
 *
 * @author Korovin Anatoliy
 */
public class MultiKeysXSyncTest {

	private static final int ITERATIONS = 100_000;
	private static final int THREADS_COUNT = 8;
	private static final long INITIAL_BALANCE = 1000L;

	private XSync<Long> xsync = new XSync<>();

	@Test
	void twoKeysSynchronizeExecute() {
		List<Account> accounts = LongStream.range(0, 10)
		                                   .boxed()
		                                   .map(i -> new Account(i, INITIAL_BALANCE))
		                                   .collect(Collectors.toList());

		// add two instance with the same id
		accounts.add(new Account(1L, INITIAL_BALANCE));

		StressTestRunner.test()
		                .mode(ExecutionMode.EXECUTOR_MODE)
		                .threads(THREADS_COUNT)
		                .iterations(ITERATIONS)
		                // deadlock prevention
		                .timeout(1, TimeUnit.MINUTES)
		                .run(() -> {
			                // select 3 different accounts
			                int fromId = randomExclude(accounts.size());
			                Account from = accounts.get(fromId);
			                int toId = randomExclude(accounts.size(), fromId);
			                Account to = accounts.get(toId);
			                // Act
			                transfer(from, to);
		                });
		// Assert
		long sum = accounts.stream()
		                   .peek(System.out::println)
		                   .mapToLong(Account::getBalance)
		                   .sum();

		System.out.println("SUM=" + sum);
		assertThat(sum).isEqualTo(accounts.size() * INITIAL_BALANCE);
	}

	@Test
	void twoKeysSynchronizeEvaluate() {
		List<Account> accounts = LongStream.range(0, 10)
		                                   .boxed()
		                                   .map(i -> new Account(i, INITIAL_BALANCE))
		                                   .collect(Collectors.toList());

		// add two instance with the same id
		accounts.add(new Account(1L, INITIAL_BALANCE));

		StressTestRunner.test()
		                .mode(ExecutionMode.EXECUTOR_MODE)
		                .threads(THREADS_COUNT)
		                .iterations(ITERATIONS)
		                // deadlock prevention
		                .timeout(1, TimeUnit.MINUTES)
		                .run(() -> {
			                // select 3 different accounts
			                int fromId = randomExclude(accounts.size());
			                Account from = accounts.get(fromId);
			                int toId = randomExclude(accounts.size(), fromId);
			                Account to = accounts.get(toId);
			                // Act
			                long balance = transferEval(from, to);
			                assertThat(balance).isGreaterThan(0);
		                });
		// Assert
		long sum = accounts.stream()
		                   .peek(System.out::println)
		                   .mapToLong(Account::getBalance)
		                   .sum();

		System.out.println("SUM=" + sum);
		assertThat(sum).isEqualTo(accounts.size() * INITIAL_BALANCE);
	}

	@Test
	void sameKeysSynchronizeExecute() {
		List<Account> accounts = LongStream.range(0, 10)
										   .boxed()
										   .map(i -> new Account(i, INITIAL_BALANCE))
										   .collect(Collectors.toList());

		// add two instance with the same id
		accounts.add(new Account(1L, INITIAL_BALANCE));

		StressTestRunner.test()
						.mode(ExecutionMode.EXECUTOR_MODE)
						.threads(THREADS_COUNT)
						.iterations(ITERATIONS)
						// deadlock prevention
						.timeout(1, TimeUnit.MINUTES)
						.run(() -> {
							// select 3 different accounts
							int fromId = randomExclude(accounts.size());
							Account from = accounts.get(fromId);
							int toId = randomExclude(accounts.size(), fromId);
							Account to = accounts.get(toId);
							// Act
							transferOnSameKeys(from, to);
						});
		// Assert
		long sum = accounts.stream()
						   .peek(System.out::println)
						   .mapToLong(Account::getBalance)
						   .sum();

		System.out.println("SUM=" + sum);
		assertThat(sum).isEqualTo(accounts.size() * INITIAL_BALANCE);
	}

	/**
	 * check the correct synchronization on the local(for XSync instance) mutex
	 * when internal hash led to a collision of multiple mutexes
	 */
	@Test
	void collectionWithTheSameKeysSynchronizeExecute() {
		List<Account> accounts = LongStream.range(0, 10)
										   .boxed()
										   .map(i -> new Account(i, INITIAL_BALANCE))
										   .collect(Collectors.toList());

		// add two instance with the same id
		accounts.add(new Account(1L, INITIAL_BALANCE));

		StressTestRunner.test()
						.mode(ExecutionMode.EXECUTOR_MODE)
						.threads(THREADS_COUNT)
						.iterations(ITERATIONS)
						// deadlock prevention
						.timeout(1, TimeUnit.MINUTES)
						.run(() -> {
							// select 3 different accounts
							int fromId = randomExclude(accounts.size());
							Account from = accounts.get(fromId);
							int toId = randomExclude(accounts.size(), fromId);
							Account to = accounts.get(toId);
							// Act
							transferOnCollectionWithTheSameKeys(from, to);
						});
		// Assert
		long sum = accounts.stream()
						   .peek(System.out::println)
						   .mapToLong(Account::getBalance)
						   .sum();

		System.out.println("SUM=" + sum);
		assertThat(sum).isEqualTo(accounts.size() * INITIAL_BALANCE);
	}

	@Test
	void multipleKeysExecute() {
		List<Account> accounts = LongStream.range(0, 10)
		                                   .boxed()
		                                   .map(i -> new Account(i, INITIAL_BALANCE))
		                                   .collect(Collectors.toList());

		// add two instance with the same id
		accounts.add(new Account(1L, INITIAL_BALANCE));

		StressTestRunner.test()
		                .mode(ExecutionMode.EXECUTOR_MODE)
		                .threads(THREADS_COUNT)
		                .iterations(ITERATIONS)
		                // deadlock prevention
		                .timeout(1, TimeUnit.MINUTES)
		                .run(() -> {
			                // select 3 different accounts
			                int fromId = randomExclude(accounts.size());
			                Account from = accounts.get(fromId);
			                int toId = randomExclude(accounts.size(), fromId);
			                Account to = accounts.get(toId);
			                int collectorId = randomExclude(accounts.size(), fromId, toId);
			                Account collector = accounts.get(collectorId);
			                // Act
			                transfer(from, to, collector);
		                });
		// Assert
		long sum = accounts.stream()
		                   .peek(System.out::println)
		                   .mapToLong(Account::getBalance)
		                   .sum();

		System.out.println("SUM=" + sum);
		assertThat(sum).isEqualTo(accounts.size() * INITIAL_BALANCE);
	}


	@Test
	void multipleKeysEvaluate() {
		List<Account> accounts = LongStream.range(0, 10)
		                                   .boxed()
		                                   .map(i -> new Account(i, INITIAL_BALANCE))
		                                   .collect(Collectors.toList());

		StressTestRunner.test()
		                .mode(ExecutionMode.EXECUTOR_MODE)
		                .threads(THREADS_COUNT)
		                .iterations(ITERATIONS)
		                // deadlock prevention
		                .timeout(1, TimeUnit.MINUTES)
		                .run(() -> {
			                int fromId = randomExclude(accounts.size());
			                Account from = accounts.get(fromId);
			                int toId = randomExclude(accounts.size(), fromId);
			                Account to = accounts.get(toId);
			                int collectorId = randomExclude(accounts.size(), fromId, toId);
			                Account collector = accounts.get(collectorId);
			                // Act
			                long resultBalance = transferEval(from, to, collector);
			                // Assert
			                assertThat(resultBalance).isGreaterThan(0);
		                });

		// Assert concurrency flow
		long sum = accounts.stream()
		                   .mapToLong(Account::getBalance)
		                   .sum();

		assertThat(sum).isEqualTo(accounts.size() * INITIAL_BALANCE);
	}

	/**
	 * evaluate with multiple same keys
	 */
	@Test
	void multipleKeysEvaluateWithCollision() {
		List<Account> accounts = LongStream.range(0, 10)
										   .boxed()
										   .map(i -> new Account(i, INITIAL_BALANCE))
										   .collect(Collectors.toList());

		StressTestRunner.test()
						.mode(ExecutionMode.EXECUTOR_MODE)
						.threads(THREADS_COUNT)
						.iterations(ITERATIONS)
						// deadlock prevention
						.timeout(1, TimeUnit.MINUTES)
						.run(() -> {
							int fromId = randomExclude(accounts.size());
							Account from = accounts.get(fromId);
							int toId = randomExclude(accounts.size(), fromId);
							Account to = accounts.get(toId);
							int collectorId = randomExclude(accounts.size(), fromId, toId);
							Account collector = accounts.get(collectorId);
							// Act
							long resultBalance = transferEvalForCollectionWithTheSameKeys(from, to, collector);
							// Assert
							assertThat(resultBalance).isGreaterThan(0);
						});

		// Assert concurrency flow
		long sum = accounts.stream()
						   .mapToLong(Account::getBalance)
						   .sum();

		assertThat(sum).isEqualTo(accounts.size() * INITIAL_BALANCE);
	}

	@Test
	void syncByTheEmptyListOfKeys() {

		Exception exception = null;
		try {
			xsync.execute(Arrays.asList(), () -> {
				// nop
			});
		} catch (Exception e) {
			exception = e;
		}

		assertThat(exception).isNotNull();
		assertThat(exception.getClass()).isEqualTo(RuntimeException.class);
		assertThat(exception.getMessage()).isEqualTo("Empty key list");
	}

	@Test
	void evaluateEmptyListOfKeys() {

		Exception exception = null;
		try {
			xsync.evaluate(Arrays.asList(), () -> {
				// nop
				return null;
			});
		} catch (Exception e) {
			exception = e;
		}

		assertThat(exception).isNotNull();
		assertThat(exception.getClass()).isEqualTo(RuntimeException.class);
		assertThat(exception.getMessage()).isEqualTo("Empty key list");
	}


	@Test
	void throwExceptionInFunction() {

		Exception exception = null;
		try {
			xsync.evaluate(Arrays.asList(123L), () -> {
				// nop
				throw new UnsupportedOperationException();
			});
		} catch (Exception e) {
			exception = e;
		}

		assertThat(exception).isNotNull();
		assertThat(exception.getClass()).isEqualTo(UnsupportedOperationException.class);
	}


	private void transfer(Account first, Account second) {
		xsync.execute(first.getId(), second.getId(),
		              () -> {
			              second.balance += first.balance;
			              first.balance -= first.balance;
		              });
	}

	private long transferEval(Account first, Account second) {
		return xsync.evaluate(first.getId(), second.getId(),
		                      () -> {
			                      second.balance += first.balance / 2;
			                      first.balance -= first.balance / 2;
			                      return second.balance;
		                      });
	}

	private void transferOnSameKeys(Account first, Account second) {
		xsync.execute(first.getId(), first.getId(),
					  () -> {
						  second.balance += first.balance;
						  first.balance -= first.balance;
					  });
	}

	private void transfer(Account first, Account second, Account collector) {
		xsync.execute(Arrays.asList(first.getId(), second.getId(), collector.getId()),
		              () -> {
			              collector.balance += first.balance / 2 + second.balance / 2;
			              first.balance -= first.balance / 2;
			              second.balance -= second.balance / 2;
		              });
	}

	private long transferEval(Account first, Account second, Account collector) {
		return xsync.evaluate(Arrays.asList(first.getId(), second.getId(), collector.getId()),
		                      () -> {
			                      collector.balance += first.balance / 2 + second.balance / 2;
			                      first.balance -= first.balance / 2;
			                      second.balance -= second.balance / 2;
			                      return collector.balance;
		                      });
	}

	private long transferEvalForCollectionWithTheSameKeys(Account first, Account second, Account collector) {
		return xsync.evaluate(Arrays.asList(first.getId(), second.getId(), first.getId(), collector.getId()),
							  () -> {
								  collector.balance += first.balance / 2 + second.balance / 2;
								  first.balance -= first.balance / 2;
								  second.balance -= second.balance / 2;
								  return collector.balance;
							  });
	}

	private void transferOnCollectionWithTheSameKeys(Account first, Account second) {
		xsync.execute(Arrays.asList(first.getId(), first.getId(), first.getId()),
					  () -> {
						  second.balance += first.balance;
						  first.balance -= first.balance;
					  });
	}

	private int randomExclude(int maxValue, Integer... excludingValue) {
		List<Integer> exclude = Arrays.asList(excludingValue);
		int rnd = new Random().nextInt(maxValue);
		while (exclude.contains(rnd)) {
			rnd = new Random().nextInt(maxValue);
		}
		return rnd;
	}

	class Account {
		private Long id;
		private long balance;

		public Account(Long id, long balance) {
			this.id = id;
			this.balance = balance;
		}

		public Long getId() {
			return id;
		}

		public long getBalance() {
			return balance;
		}

		@Override
		public String toString() {
			return "Account{" +
			       "id=" + id +
			       ", balance=" + balance +
			       '}';
		}
	}
}
