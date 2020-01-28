package com.antkorwin.xsync;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


class CollisionDetectorTest {

	private XMutexFactory<Integer> factory;
	private MutexSorter<Integer> sorter;

	@Nested
	class CollisionDetecting {

		@BeforeEach
		void setUp() {
			factory = mock(XMutexFactory.class);
			sorter = new MutexSorter<>(factory);
		}

		@Test
		void findCollision() {
			// Arrange
			XMutex<Integer> m1 = XMutex.of(1);
			XMutex<Integer> m2 = XMutex.of(2);
			List<XMutex<Integer>> mutexes = Arrays.asList(m1, m2, m2);
			// Act & assert
			assertThat(sorter.existCollision(mutexes)).isTrue();
		}

		@Test
		void withoutCollision() {
			// Arrange
			XMutex<Integer> m1 = XMutex.of(1);
			XMutex<Integer> m2 = XMutex.of(2);
			List<XMutex<Integer>> mutexes = Arrays.asList(m1, m2);
			// Act & assert
			assertThat(sorter.existCollision(mutexes)).isFalse();
		}
	}

	@Nested
	class OrderingTest {

		@BeforeEach
		void setUp() {
			factory = new XMutexFactoryImpl<>();
			sorter = new MutexSorter<>(factory);
		}

		@Test
		void obtainMutexesByKeys() {
			// Arrange
			XMutex<Integer> m1 = factory.getMutex(1);
			XMutex<Integer> m2 = factory.getMutex(2);
			XMutex<Integer> m3 = factory.getMutex(3);
			// Act
			List<XMutex<Integer>> mutexes = sorter.getOrderedMutexList(Arrays.asList(1, 2, 3));
			// Assert
			assertThat(mutexes).contains(m1, m2, m3);
		}

		@Test
		void orderingIndependentOfSequence() {
			// Arrange
			XMutex<Integer> m1 = factory.getMutex(1);
			XMutex<Integer> m2 = factory.getMutex(2);
			XMutex<Integer> m3 = factory.getMutex(3);
			// Act
			List<XMutex<Integer>> asc = sorter.getOrderedMutexList(Arrays.asList(1, 2, 3));
			List<XMutex<Integer>> desc = sorter.getOrderedMutexList(Arrays.asList(3, 2, 1));
			// Assert
			assertThat(asc).containsExactlyElementsOf(desc);
		}
	}
}