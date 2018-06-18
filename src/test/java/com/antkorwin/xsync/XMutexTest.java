package com.antkorwin.xsync;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Created by Korovin Anatolii on 17.06.2018.
 *
 * @author Korovin Anatolii
 * @version 1.0
 */
public class XMutexTest {


    @Test
    public void testMutexEquals() {
        // Arrange
        String key1 = new String("111");
        String key2 = new String("111");
        XMutex<String> mutex1 = new XMutex<>(key1);
        XMutex<String> mutex2 = new XMutex<>(key2);
        // Act & Assert
        Assertions.assertThat(key1 != key2).isTrue();
        Assertions.assertThat(mutex1).isEqualTo(mutex2);
    }

    @Test
    public void testEqualsAndMap() {
        // Arrange
        WeakHashMap<XMutex<String>, WeakReference<XMutex<String>>> map = new WeakHashMap<>();
        String key1 = new String("111");
        String key2 = new String("111");
        XMutex<String> mutex1 = new XMutex<>(key1);
        XMutex<String> mutex2 = new XMutex<>(key2);

        // Act
        map.put(mutex1, new WeakReference<>(mutex1));
        map.put(mutex2, new WeakReference<>(mutex2));

        // Asserts
        Assertions.assertThat(map.size()).isEqualTo(1);
    }
}