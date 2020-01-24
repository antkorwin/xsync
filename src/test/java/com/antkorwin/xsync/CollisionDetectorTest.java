package com.antkorwin.xsync;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static com.antkorwin.xsync.CollisionDetector.existSameHashCodes;
import static org.assertj.core.api.Assertions.assertThat;


class CollisionDetectorTest {

    @Test
    void findCollision() {
        // Arrange
        XMutex<Integer> m1 = XMutex.of(1);
        XMutex<Integer> m2 = XMutex.of(2);
        List<XMutex<Integer>> mutexes = Arrays.asList(m1, m2, m2);
        // Act & assert
        assertThat(existSameHashCodes(mutexes)).isTrue();
    }

    @Test
    void withoutCollision() {
        // Arrange
        XMutex<Integer> m1 = XMutex.of(1);
        XMutex<Integer> m2 = XMutex.of(2);
        List<XMutex<Integer>> mutexes = Arrays.asList(m1, m2);
        // Act & assert
        assertThat(existSameHashCodes(mutexes)).isFalse();
    }
}