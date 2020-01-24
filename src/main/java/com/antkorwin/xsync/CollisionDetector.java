package com.antkorwin.xsync;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created on 24/01/2020
 * <p>
 * TODO: replace on the JavaDoc
 *
 * @author Korovin Anatoliy
 */
class CollisionDetector {


    static <KeyT> boolean existSameHashCodes(List<XMutex<KeyT>> mutexes) {

        Set<Integer> hashCodes = mutexes.stream()
                                        .map(System::identityHashCode)
                                        .collect(Collectors.toSet());

        return hashCodes.size() < mutexes.size();
    }
}
