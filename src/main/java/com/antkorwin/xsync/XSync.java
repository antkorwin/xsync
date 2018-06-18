package com.antkorwin.xsync;

/**
 * Created by Korovin Anatolii on 18.06.2018.
 *
 * @author Korovin Anatolii
 * @version 1.0
 */
public class XSync<KeyT> {

    private XMutexFactory<KeyT> mutexFactory = new XMutexFactory<>();

    public void execute(KeyT mutexKey, Runnable runnable) {
        XMutex<KeyT> mutex = mutexFactory.getMutex(mutexKey);
        synchronized (mutex) {
            runnable.run();
        }
    }
}
