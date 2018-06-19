package com.antkorwin.xsync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Created on 18.06.2018.
 *
 * @author Korovin Anatoliy
 */
public class XSyncTest {

    private static final int THREAD_CNT = 10000000;

    @Test(timeout = 10000)
    public void testSyncBySingleKeyInConcurrency() {
        // Arrange
        XSync<UUID> xsync = new XSync<>();
        UUID id = UUID.randomUUID();
        NonAtomicInt var = new NonAtomicInt(0);

        // Act
        IntStream.range(0, THREAD_CNT)
                 .boxed()
                 .parallel()
                 .forEach(j -> xsync.execute(id, var::increment));

        // Asserts
        await().atMost(5, TimeUnit.SECONDS)
               .until(var::getValue, equalTo(THREAD_CNT));

        Assertions.assertThat(var.getValue()).isEqualTo(THREAD_CNT);
    }

    @Getter
    @AllArgsConstructor
    private class NonAtomicInt {
        private int value;

        public int increment() {
            return value++;
        }
    }
}