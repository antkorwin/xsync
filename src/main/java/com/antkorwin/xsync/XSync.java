package com.antkorwin.xsync;

import java.util.function.Supplier;

/**
 * Created on 18.06.2018.
 *
 * @author Korovin Anatoliy
 */
public class XSync<KeyT> {

    private XMutexFactory<KeyT> mutexFactory = new XMutexFactory<>();

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
     *
     * @return result which return by a supplier
     */
    public <ResultT> ResultT evaluate(KeyT mutexKey, Supplier<ResultT> supplier) {
        XMutex<KeyT> mutex = mutexFactory.getMutex(mutexKey);
        synchronized (mutex) {
            return supplier.get();
        }
    }
}
