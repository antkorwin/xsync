package com.antkorwin.xsync;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 03.07.2018.
 *
 * @author Korovin Anatoliy
 */
public class TestUtils {


    public static <TypeT> Set<TypeT> createConcurrentSet() {
        return Collections.newSetFromMap(new ConcurrentHashMap<TypeT, Boolean>());
    }


    public static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
