package com.dnestr.base.tools.ado;

import com.dnestr.base.tools.bddsync.SyncCandidate;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdoScenarioMetadataParserTest {

    private static final String GHERKIN_BODY = "Given x\nWhen y\nThen z";

    private final AdoScenarioMetadataParser parser = new AdoScenarioMetadataParser();

    private SyncCandidate candidate(List<String> tags) {
        return new SyncCandidate(
                Path.of("features/foo.feature"),
                "My Feature",
                0,
                true,
                null,
                "My Scenario",
                GHERKIN_BODY,
                tags
        );
    }

    @Test
    void parse_returnsSprintAndPbiIds_fromTags() {
        AdoScenarioMetadata metadata = parser.parse(
                candidate(List.of("@S:Sprint1", "@us:111", "@us:222"))
        );

        assertThat(metadata.sprint()).isEqualTo("Sprint1");
        assertThat(metadata.pbiIds()).containsExactly(111L, 222L);
    }

    @Test
    void parse_returnsEmptyPbiIds_whenNoUsTagsPresent() {
        AdoScenarioMetadata metadata = parser.parse(candidate(List.of("@S:Sprint1")));

        assertThat(metadata.pbiIds()).isEmpty();
    }

    @Test
    void parse_dedupesRepeatedPbiIds() {
        AdoScenarioMetadata metadata = parser.parse(
                candidate(List.of("@S:Sprint1", "@us:111", "@us:111"))
        );

        assertThat(metadata.pbiIds()).containsExactly(111L);
    }

    @Test
    void parse_throws_whenNoSprintTagPresent() {
        assertThatThrownBy(() -> parser.parse(candidate(List.of("@us:111"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one @S: tag");
    }

    @Test
    void parse_throws_whenMultipleSprintTagsPresent() {
        assertThatThrownBy(() -> parser.parse(
                candidate(List.of("@S:Sprint1", "@S:Sprint2"))
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one @S: tag");
    }

    @Test
    void parse_returnsEmptyTestCaseId_whenNoTcTagPresent() {
        AdoScenarioMetadata metadata = parser.parse(candidate(List.of("@S:Sprint1")));

        assertThat(metadata.testCaseId()).isEmpty();
    }

    @Test
    void parse_returnsTestCaseId_whenTcTagPresent() {
        AdoScenarioMetadata metadata = parser.parse(
                candidate(List.of("@S:Sprint1", "@tc:9"))
        );

        assertThat(metadata.testCaseId()).isEqualTo(Optional.of(9L));
    }

    @Test
    void parse_throws_whenTcTagIsNotNumeric() {
        assertThatThrownBy(() -> parser.parse(
                candidate(List.of("@S:Sprint1", "@tc:not-a-number"))
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid @tc: test case ID")
                .hasMessageContaining("not-a-number");
    }

    @Test
    void parse_throws_whenMultipleTcTagsPresent() {
        assertThatThrownBy(() -> parser.parse(
                candidate(List.of("@S:Sprint1", "@tc:1", "@tc:2"))
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("multiple @tc: tags");
    }

    @Test
    void parse_throws_whenSprintTagHasNoValue() {
        assertThatThrownBy(() -> parser.parse(candidate(List.of("@S:"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no value");
    }

    @Test
    void parse_throws_whenUsTagIsNotNumeric() {
        assertThatThrownBy(() -> parser.parse(
                candidate(List.of("@S:Sprint1", "@us:not-a-number"))
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid @us: work item ID")
                .hasMessageContaining("not-a-number");
    }
}
