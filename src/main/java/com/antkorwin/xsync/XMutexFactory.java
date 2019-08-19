package com.antkorwin.xsync;

/**
 * Created on 29.07.2018.
 *
 * @author Korovin Anatoliy
 */
public interface XMutexFactory<KeyT> {

    /**
     * Creates and returns a mutex by the key.
     * If the mutex for this key already exists(or use by another thread),
     * then returns the same reference of the mutex.
     *
     * @param key object which used as a key for synchronization
     * @return mutext instance created for this key
     */
    XMutex<KeyT> getMutex(KeyT key);

    /**
     * @return count of mutexes in this factory.
     */
    long size();
}
