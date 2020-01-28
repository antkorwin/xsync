package com.antkorwin.xsync;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created on 24/01/2020
 * <p>
 * Help with the sorting of mutexes in the same order,
 * based on the System::identityHashCode.
 *
 * @author Korovin Anatoliy
 */
class MutexSorter<KeyT> {

	private final XMutexFactory<KeyT> factory;

	MutexSorter(XMutexFactory<KeyT> factory) {
		this.factory = factory;
	}

	/**
	 * Check existing of collisions in the list of mutexes.
	 *
	 * @param mutexes list of mutexes to run checking
	 * @return true if there are at least two mutexes with the same identityHashCode
	 * value in the list, and false if all mutexes have different hash values.
	 */
	boolean existCollision(List<XMutex<KeyT>> mutexes) {

		Set<Integer> hashCodes = mutexes.stream()
		                                .map(System::identityHashCode)
		                                .collect(Collectors.toSet());

		return hashCodes.size() < mutexes.size();
	}

	/**
	 * Obtain a list of mutexes from {@link XMutexFactory} by the collection
	 * of keys and sort this list by identityHashCode values of a mutex.
	 *
	 * @param keys collection of keys which necessary to sort in the same order
	 *             depends just on the external values of identityHashCode.
	 * @return sorted list of mutexes
	 */
	List<XMutex<KeyT>> getOrderedMutexList(Collection<KeyT> keys) {
		return keys.stream()
		           .map(factory::getMutex)
		           .sorted(Comparator.comparingInt(System::identityHashCode))
		           .collect(Collectors.toList());
	}
}
