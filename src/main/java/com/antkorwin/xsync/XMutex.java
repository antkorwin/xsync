package com.antkorwin.xsync;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

/**
 * Created by Korovin Anatolii on 14.06.2018.
 *
 * @author Korovin Anatolii
 * @version 1.0
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
