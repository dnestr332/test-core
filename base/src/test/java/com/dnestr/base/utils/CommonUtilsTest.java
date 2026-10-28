package com.dnestr.base.utils;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommonUtilsTest {

    static class LoginScreen {}

    /** Named the way CGLIB names a proxy of {@link LoginScreen}. */
    static class LoginScreen$$SpringCGLIB$$0 extends LoginScreen {}

    @Test
    void prettyNameIsTheSimpleName() {
        assertThat(CommonUtils.prettyClassName(new LoginScreen())).isEqualTo("LoginScreen");
    }

    @Test
    void prettyNameDropsAProxySuffix() {
        assertThat(CommonUtils.prettyClassName(new LoginScreen$$SpringCGLIB$$0())).isEqualTo("LoginScreen");
    }

    @Test
    void sortedWithEqualNeighbours() {
        assertThat(CommonUtils.isSorted(List.of(1, 2, 2, 5), Comparator.naturalOrder())).isTrue();
    }

    @Test
    void notSortedWhenOnePairIsOutOfOrder() {
        assertThat(CommonUtils.isSorted(List.of(1, 3, 2), Comparator.naturalOrder())).isFalse();
    }

    @Test
    void followsTheGivenOrder() {
        assertThat(CommonUtils.isSorted(List.of(5, 3, 1), Comparator.<Integer>reverseOrder())).isTrue();
    }

    @Test
    void emptyAndSingleListsAreSorted() {
        assertThat(CommonUtils.isSorted(List.<Integer>of(), Comparator.naturalOrder())).isTrue();
        assertThat(CommonUtils.isSorted(List.of(7), Comparator.naturalOrder())).isTrue();
    }
}
