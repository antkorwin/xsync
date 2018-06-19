package com.antkorwin.xsync;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Created on 17.06.2018.
 *
 * @author Korovin Anatoliy
 */
public class XMutexTest {

    private final String FIRST_KEY = new String("111");
    private final String SECOND_KEY = new String("111");

    @Test
    public void testMutexEquals() {
        // Arrange
        XMutex<String> mutex1 = new XMutex<>(FIRST_KEY);
        XMutex<String> mutex2 = new XMutex<>(SECOND_KEY);

        // Act & Assert
        Assertions.assertThat(FIRST_KEY != SECOND_KEY).isTrue();
        Assertions.assertThat(mutex1).isEqualTo(mutex2);
    }

    @Test
    public void testWeakMapWithTwoEqualMutexes() {
        // Arrange
        XMutex<String> mutex1 = new XMutex<>(FIRST_KEY);
        XMutex<String> mutex2 = new XMutex<>(SECOND_KEY);

        WeakHashMap<XMutex<String>, WeakReference<XMutex<String>>> map = new WeakHashMap<>();

        // Act
        map.put(mutex1, new WeakReference<>(mutex1));
        map.put(mutex2, new WeakReference<>(mutex2));

        // Asserts
        Assertions.assertThat(map.size()).isEqualTo(1);
    }
}