package com.antkorwin.xsync;

import java.util.Objects;


/**
 * Created on 14.06.2018.
 * <p>
 * Internal synchronization primitive which use by XSync
 *
 * @author Korovin Anatoliy
 */
public class XMutex<KeyT> {

	private final KeyT key;

	public XMutex(KeyT key) {
		this.key = key;
	}

	/**
	 * Static factory method to create a new {@link XMutex} instance
	 *
	 * @param key object which value will be use as a key
	 * @param <KeyT> type of a key
	 * @return XMutex instance
	 */
	public static <KeyT> XMutex<KeyT> of(KeyT key) {
		return new XMutex<>(key);
	}

	public KeyT getKey() {
		return key;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		XMutex<?> xMutex = (XMutex<?>) o;
		return Objects.equals(key, xMutex.key);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key);
	}
}
