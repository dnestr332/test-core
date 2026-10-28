package com.dnestr.base.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumUtilsTest {

    enum Sample { FOO_BAR, BAZ }

    @Test
    void parse_resolvesExactMatch() {
        assertThat(EnumUtils.parse(Sample.class, "FOO_BAR")).isEqualTo(Sample.FOO_BAR);
    }

    @Test
    void parse_normalizesCaseAndSeparators() {
        assertThat(EnumUtils.parse(Sample.class, "foo-bar")).isEqualTo(Sample.FOO_BAR);
        assertThat(EnumUtils.parse(Sample.class, "  foo bar  ")).isEqualTo(Sample.FOO_BAR);
    }

    @Test
    void parse_nullValue_throwsWithClassName() {
        assertThatThrownBy(() -> EnumUtils.parse(Sample.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sample");
    }

    @Test
    void parse_blankValue_throws() {
        assertThatThrownBy(() -> EnumUtils.parse(Sample.class, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_invalidValue_listsAvailableOptions() {
        assertThatThrownBy(() -> EnumUtils.parse(Sample.class, "nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nope")
                .hasMessageContaining("FOO_BAR")
                .hasMessageContaining("BAZ");
    }

    @Test
    void tryParse_returnsNullOnFailure() {
        assertThat(EnumUtils.tryParse(Sample.class, "nope")).isNull();
        assertThat(EnumUtils.tryParse(Sample.class, "baz")).isEqualTo(Sample.BAZ);
    }

    @Test
    void parseOrDefault_fallsBackWhenInvalid() {
        assertThat(EnumUtils.parseOrDefault(Sample.class, "nope", Sample.BAZ)).isEqualTo(Sample.BAZ);
        assertThat(EnumUtils.parseOrDefault(Sample.class, "foo_bar", Sample.BAZ)).isEqualTo(Sample.FOO_BAR);
    }

    @Test
    void isValid_reflectsParseability() {
        assertThat(EnumUtils.isValid(Sample.class, "baz")).isTrue();
        assertThat(EnumUtils.isValid(Sample.class, "nope")).isFalse();
    }

    @Test
    void toEnumKey_collapsesSeparatorsAndTrimsEdges() {
        assertThat(EnumUtils.toEnumKey(" foo--bar  ")).isEqualTo("FOO_BAR");
        assertThat(EnumUtils.toEnumKey("__foo__")).isEqualTo("FOO");
        assertThat(EnumUtils.toEnumKey(null)).isEmpty();
    }

    sealed interface Item permits HomeItem, RidesItem {}

    enum HomeItem implements Item { ONLINE_TOGGLE }

    enum RidesItem implements Item { ALL_RIDES_SEGMENT }

    @Test
    void parseSealed_findsTheConstantInWhicheverPermittedEnumHasIt() {
        assertThat(EnumUtils.parseSealed(Item.class, "All Rides Segment")).isEqualTo(RidesItem.ALL_RIDES_SEGMENT);
        assertThat(EnumUtils.parseSealed(Item.class, "online-toggle")).isEqualTo(HomeItem.ONLINE_TOGGLE);
    }

    @Test
    void parseSealed_namesTheTypeForAnUnknownValue() {
        assertThatThrownBy(() -> EnumUtils.parseSealed(Item.class, "Logout"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown Item: 'Logout'");
    }

    @Test
    void parseSealed_rejectsATypeThatIsNotSealed() {
        assertThatThrownBy(() -> EnumUtils.parseSealed(Runnable.class, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a sealed type");
    }
}
