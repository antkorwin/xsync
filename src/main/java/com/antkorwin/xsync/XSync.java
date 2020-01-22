package com.antkorwin.xsync;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created on 18.06.2018.
 * <p>
 * XSync is a thread-safe mutex factory, that provide an
 * ability to synchronize by the value of object(not by the reference of object).
 *
 * @author Korovin Anatoliy
 */
public class XSync<KeyT> {

	private final XMutexFactory<KeyT> mutexFactory;

	private static final Object globalLock = new Object();

	/**
	 * Make the new XSync instance with an individual mutex factory
	 */
	public XSync() {
		this.mutexFactory = new XMutexFactoryImpl<>();
	}

	/**
	 * Make the new XSync with selected mutex factory,
	 * it's useful when you need to create a multiple XSync instances
	 * based on the same mutex factory.
	 *
	 * @param mutexFactory the mutex factory instance to obtain all mutexes from key values
	 */
	public XSync(XMutexFactory<KeyT> mutexFactory) {
		this.mutexFactory = mutexFactory;
	}

	/**
	 * Executes a runnable in a synchronization block on a mutex,
	 * which created from the mutexKey value.
	 *
	 * @param mutexKey key for the synchronization locks
	 * @param runnable function that we need to run
	 */
	public void execute(KeyT mutexKey, Runnable runnable) {
		XMutex<KeyT> mutex = mutexFactory.getMutex(mutexKey);
		synchronized (mutex) {
			runnable.run();
		}
	}

	/**
	 * Evaluate a supplier in a synchronization block on a mutex,
	 * which created from the mutexKey value.
	 *
	 * @param mutexKey  key for the synchronization locks
	 * @param supplier  function that we need to run in sync. block
	 * @param <ResultT> type of tht result of a supplier
	 * @return result which return by a supplier
	 */
	public <ResultT> ResultT evaluate(KeyT mutexKey, Supplier<ResultT> supplier) {
		XMutex<KeyT> mutex = mutexFactory.getMutex(mutexKey);
		synchronized (mutex) {
			return supplier.get();
		}
	}


	/**
	 * Execute the runnable within a pair of synchronized blocks
	 * which built from the first and the second keys.
	 * <p>
	 * Note that the ordering of keys in synchronized blocks doesn't depend
	 * on the ordering of keys in this method arguments. The order depends
	 * on the values of keys, it prevents your code from deadlocking.
	 * So, you don't need to care about the order of the keys if you use XSync.
	 *
	 * @param firstKey  the first key to use in the synchronization blocks
	 * @param secondKey the second key
	 * @param runnable  code which you want to synchronize
	 */
	public void execute(KeyT firstKey, KeyT secondKey, Runnable runnable) {

		XMutex<KeyT> firstMutex = mutexFactory.getMutex(firstKey);
		XMutex<KeyT> secondMutex = mutexFactory.getMutex(secondKey);

		int firstHash = System.identityHashCode(firstKey);
		int secondHash = System.identityHashCode(secondKey);

		if (firstHash > secondHash) {
			XMutex<KeyT> tmp = firstMutex;
			firstMutex = secondMutex;
			secondMutex = tmp;
		}

		if (firstHash != secondHash) {
			synchronized (firstMutex) {
				synchronized (secondMutex) {
					runnable.run();
				}
			}
		} else {
			synchronized (globalLock) {
				synchronized (firstMutex) {
					synchronized (secondMutex) {
						runnable.run();
					}
				}
			}
		}
	}


	/**
	 * Evaluate the {@link Supplier} within a pair of synchronized blocks
	 * which built from the first and the second keys.
	 * <p>
	 * Note that the ordering of keys in synchronized blocks doesn't depend
	 * on the ordering of keys in this method arguments. The order depends
	 * on the values of keys, it prevents your code from deadlocking.
	 * So, you don't need to care about the order of the keys if you use XSync.
	 *
	 * @param firstKey  the first key to use in the synchronization blocks
	 * @param secondKey the second key
	 * @param supplier  code which you want to synchronize
	 * @param <ResultT> the type of supplier's result
	 * @return the result of the supplier
	 */
	public <ResultT> ResultT evaluate(KeyT firstKey, KeyT secondKey, Supplier<ResultT> supplier) {

		XMutex<KeyT> firstMutex = mutexFactory.getMutex(firstKey);
		XMutex<KeyT> secondMutex = mutexFactory.getMutex(secondKey);

		int firstHash = System.identityHashCode(firstKey);
		int secondHash = System.identityHashCode(secondKey);

		if (firstHash > secondHash) {
			XMutex<KeyT> tmp = firstMutex;
			firstMutex = secondMutex;
			secondMutex = tmp;
		}

		if (firstHash != secondHash) {
			synchronized (firstMutex) {
				synchronized (secondMutex) {
					return supplier.get();
				}
			}
		} else {
			synchronized (globalLock) {
				synchronized (firstMutex) {
					synchronized (secondMutex) {
						return supplier.get();
					}
				}
			}
		}
	}

	/**
	 * Execute the runnable in a multi-keys synchronization block
	 * which compose step-by-step on the each key from the keys collection.
	 * <p>
	 * Note:<p>
	 * The ordering of synchronization depends on the key value (not on
	 * the key order in the collection), it prevents your code of deadlocks
	 * in another code with synchronized blocks on the same keys.
	 *
	 * @param keys     collection of keys to sequentially synchronization
	 * @param runnable code block which is necessary to synchronize by the sequence of keys
	 */
	public void execute(Collection<KeyT> keys, Runnable runnable) {

		if (keys.size() < 1) {
			throw new RuntimeException("Empty key list");
		}

		List<XMutex<KeyT>> mutexes = getOrderedMutexList(keys);
		if (existCollisionByHashCodes(mutexes)) {
			synchronized (globalLock) {
				recursiveExecute(mutexes, runnable);
			}
		} else {
			recursiveExecute(mutexes, runnable);
		}
	}


	private List<XMutex<KeyT>> getOrderedMutexList(Collection<KeyT> keys) {
		return keys.stream()
		           .map(mutexFactory::getMutex)
		           .sorted(Comparator.comparingInt(System::identityHashCode))
		           .collect(Collectors.toList());
	}


	private boolean existCollisionByHashCodes(List<XMutex<KeyT>> mutexes) {

		List<Integer> hashCodes = mutexes.stream()
		                                 .map(System::identityHashCode)
		                                 .distinct()
		                                 .collect(Collectors.toList());

		return hashCodes.size() < mutexes.size();
	}


	private void recursiveExecute(List<XMutex<KeyT>> mutexes, Runnable runnable) {

		XMutex<KeyT> currentMutex = mutexes.get(0);
		mutexes.remove(currentMutex);

		if (mutexes.size() == 0) {
			synchronized (currentMutex) {
				runnable.run();
			}
		} else {
			synchronized (currentMutex) {
				recursiveExecute(mutexes, runnable);
			}
		}
	}


	/**
	 * Evaluate the supplier in a multi-keys synchronization block
	 * which compose step-by-step sync on the each key from the keys collection.
	 * <p>
	 * Note that the ordering of synchronization depends on the key value (not on
	 * the key order in the collection), it prevents your code of deadlocks
	 * in another code with synchronized blocks on the same keys.
	 *
	 * @param keys      collection of keys to sequentially synchronization
	 * @param supplier  running of this code should be synchronized by the sequence of keys
	 * @param <ResultT> the type of a supplier result
	 * @return the result of supplier execution
	 */
	public <ResultT> ResultT evaluate(Collection<KeyT> keys, Supplier<ResultT> supplier) {

		if (keys.size() < 1) {
			throw new RuntimeException("Empty key list");
		}

		List<XMutex<KeyT>> mutexes = getOrderedMutexList(keys);
		if (existCollisionByHashCodes(mutexes)) {
			synchronized (globalLock) {
				return recursiveEvaluate(mutexes, supplier);
			}
		} else {
			return recursiveEvaluate(mutexes, supplier);
		}
	}


	private <ResultT> ResultT recursiveEvaluate(List<XMutex<KeyT>> mutexes, Supplier<ResultT> supplier) {

		XMutex<KeyT> currentMutex = mutexes.get(0);
		mutexes.remove(currentMutex);

		if (mutexes.size() == 0) {
			synchronized (currentMutex) {
				return supplier.get();
			}
		} else {
			synchronized (currentMutex) {
				return recursiveEvaluate(mutexes, supplier);
			}
		}
	}

}
