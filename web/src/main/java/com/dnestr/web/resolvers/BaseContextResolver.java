package com.dnestr.web.resolvers;

import com.dnestr.web.pages.PageElement;

/**
 * Per-app hook for turning the free-form string values that come out of BDD step text into the
 * concrete values a scenario actually needs, before they reach Playwright. {@code test-core} ships
 * no implementation of this interface — each consuming app implements it once and wires it into
 * its own step-definition layer.
 * <p>
 * The convention observed across existing implementations (not enforced by this interface itself):
 * a small set of reserved sentinel strings in step text (e.g. {@code "valid"}, {@code "expected"})
 * trigger special handling — generating fresh test data, or looking back at a value stashed earlier
 * in the scenario — while any other string is treated as a literal and passed through (or parsed,
 * for the row/count methods) as-is.
 */
public interface BaseContextResolver {

    /**
     * Resolves a step's input value for {@code element}. Convention: a sentinel like
     * {@code "valid"} generates fresh test data appropriate to {@code element} and returns it
     * (typically stashing it in scenario context for later reuse via
     * {@link #resolveExpectedValue}); any other string is returned unchanged as a literal.
     */
    String resolveInput(PageElement element, String value);

    /**
     * Resolves a step's expected value, keyed by {@code key} (typically a field or column
     * identifier). Convention: {@code "expected"} looks up a value stashed earlier in the
     * scenario (e.g. by {@link #resolveInput}) rather than being compared against literally;
     * any other string is the literal expected value.
     */
    String resolveExpectedValue(Object key, String value);

    /**
     * Resolves a step's row reference to a 0-based row index. Convention: a numeric string is
     * 1-based in the Gherkin step and converted to 0-based ({@code Integer.parseInt(index) - 1});
     * a sentinel like {@code "expected"} looks up a row index remembered earlier in the scenario.
     */
    int resolveRowIndex(String index);

    /**
     * Resolves a named count field (e.g. a "results count" reference in step text) to its current
     * live value, typically read off the relevant page object. Existing implementations throw for
     * an unrecognized field name rather than returning a sentinel value.
     */
    int resolveCountValue(String field);

    /**
     * Resolves a named path alias (as used in a navigation step) to the actual URL/path to
     * navigate to. Convention: a small closed set of known aliases map to dynamically built paths
     * (e.g. incorporating a token generated earlier in the scenario); anything else is returned
     * unchanged and treated as a literal path.
     */
    String resolvePath(String path);
}
