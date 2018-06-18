package com.antkorwin.xsync;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Created by Korovin Anatolii on 14.06.2018.
 *
 * @author Korovin Anatolii
 * @version 1.0
 */
public class XMutexFactory<KeyT> {

    public final Map<XMutex<KeyT>, WeakReference<XMutex<KeyT>>> weakHashMap =
            Collections.synchronizedMap(new WeakHashMap<XMutex<KeyT>, WeakReference<XMutex<KeyT>>>());


    public XMutex<KeyT> getMutex(KeyT key) {
        synchronized (weakHashMap) {
            validateKey(key);
            return getExist(key)
                    .orElseGet(() -> saveNewReference(key));
        }
    }

    private void validateKey(KeyT key) {
        if (key == null) {
            throw new IllegalArgumentException("The KEY of mutex must not be null.");
        }
    }

    private Optional<XMutex<KeyT>> getExist(KeyT key) {
        return Optional.ofNullable(weakHashMap.get(XMutex.of(key)))
                       .map(WeakReference::get);
    }

    private XMutex<KeyT> saveNewReference(KeyT key) {

        XMutex<KeyT> mutex = XMutex.of(key);

        WeakReference<XMutex<KeyT>> res = weakHashMap.put(mutex, new WeakReference<>(mutex));
        if (res != null && res.get() != null) {
            return res.get();
        }
        return mutex;
    }

    public long size() {
        return weakHashMap.size();
    }
}
