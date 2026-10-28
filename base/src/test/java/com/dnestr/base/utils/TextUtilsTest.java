package com.dnestr.base.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextUtilsTest {

    @Test
    void getStringAsDouble_parsesPlainInteger() {
        assertThat(TextUtils.getStringAsDouble("42")).isEqualTo(42.0);
    }

    @Test
    void getStringAsDouble_parsesDecimal() {
        assertThat(TextUtils.getStringAsDouble("$12.50")).isEqualTo(12.50);
    }

    @Test
    void getStringAsDouble_parsesNegativeValue() {
        assertThat(TextUtils.getStringAsDouble("-5.3")).isEqualTo(-5.3);
        assertThat(TextUtils.getStringAsDouble("Balance: -5.3 USD")).isEqualTo(-5.3);
    }

    @Test
    void getStringAsDouble_parsesNegativeInteger() {
        assertThat(TextUtils.getStringAsDouble("-42")).isEqualTo(-42.0);
    }

    @Test
    void getStringAsDouble_noNumericValue_throws() {
        assertThatThrownBy(() -> TextUtils.getStringAsDouble("no digits here"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no digits here");
    }

    @Test
    void getAmountAfterLast_readsTheNumberAfterTheLastMarker() {
        assertThat(TextUtils.getAmountAfterLast("Was $12.00, now $1,009.50", '$')).isEqualTo(1009.5);
    }

    @Test
    void getAmountAfterLast_rejectsTextWithoutTheMarker() {
        assertThatThrownBy(() -> TextUtils.getAmountAfterLast("12.00", '$'))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No '$'");
    }

    @Test
    void lastDigits_ignoresEverythingButDigits() {
        assertThat(TextUtils.lastDigits("+1 (212) 555-0100", 10)).isEqualTo("2125550100");
        assertThat(TextUtils.lastDigits("•••• 4242", 4)).isEqualTo("4242");
    }

    @Test
    void lastDigits_returnsAllDigitsWhenThereAreFewer() {
        assertThat(TextUtils.lastDigits("ext. 42", 4)).isEqualTo("42");
        assertThat(TextUtils.lastDigits(null, 4)).isEmpty();
    }

    @Test
    void lastDigits_rejectsANegativeCount() {
        assertThatThrownBy(() -> TextUtils.lastDigits("1", -1)).isInstanceOf(IllegalArgumentException.class);
    }

    //region parseDateTime
    private static final String[] PATTERNS = {"EEE, MMM d, h:mm a", "EEE, MMM d, HH:mm"};

    @Test
    void parsesWithTheFirstPatternThatFitsAndTheDefaultYear() {
        assertThat(TextUtils.parseDateTime("Mon, Jan 5, 9:30 AM", Locale.US, 2026, PATTERNS))
                .contains(LocalDateTime.of(2026, 1, 5, 9, 30));
        assertThat(TextUtils.parseDateTime("Mon, Jan 5, 21:30", Locale.US, 2026, PATTERNS))
                .contains(LocalDateTime.of(2026, 1, 5, 21, 30));
    }

    @Test
    void ignoresCaseAndExtraWhitespace() {
        assertThat(TextUtils.parseDateTime("  mon,   jan 5,  9:30 am ", Locale.US, 2026, PATTERNS))
                .contains(LocalDateTime.of(2026, 1, 5, 9, 30));
    }

    @Test
    void aPatternWithItsOwnYearKeepsIt() {
        assertThat(TextUtils.parseDateTime("01/05/2025 09:30", Locale.US, 2026, "MM/dd/yyyy HH:mm"))
                .contains(LocalDateTime.of(2025, 1, 5, 9, 30));
    }

    @Test
    void isEmptyForBlankTextOrNoFittingPattern() {
        assertThat(TextUtils.parseDateTime(" ", Locale.US, 2026, PATTERNS)).isEmpty();
        assertThat(TextUtils.parseDateTime(null, Locale.US, 2026, PATTERNS)).isEmpty();
        assertThat(TextUtils.parseDateTime("tomorrow", Locale.US, 2026, PATTERNS)).isEmpty();
    }

    @Test
    void aWeekdayThatDoesNotFitTheDefaultYearIsNotADate() {
        assertThat(TextUtils.parseDateTime("Tue, Jan 5, 9:30 AM", Locale.US, 2026, PATTERNS)).isEmpty();
    }
    //endregion
}
