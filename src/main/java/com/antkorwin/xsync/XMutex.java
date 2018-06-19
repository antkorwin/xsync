package com.antkorwin.xsync;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created on 14.06.2018.
 *
 * @author Korovin Anatoliy
 */
@EqualsAndHashCode
@Getter
public class XMutex<KeyT> {

    private KeyT key;

    public XMutex(KeyT key) {
        this.key = key;
    }

    public static <KeyT> XMutex<KeyT> of(KeyT key) {
        return new XMutex<>(key);
    }
}
