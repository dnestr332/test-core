package com.dnestr.web.resolvers;

import com.dnestr.web.pages.TableColumn;

/**
 * Resolves a table column reference from a BDD step (e.g. a column header text like
 * {@code "Email"}) to the matching {@link TableColumn} constant for a given table. {@code T} is
 * typically an enum with one constant per table on a page, conventionally implementing
 * {@link com.dnestr.web.pages.PageTable} (not enforced by this interface's bound, only by
 * convention). {@code test-core} ships no implementation; each consuming app implements it and
 * wires it into its step-definition layer.
 *
 * @param <T> the app's table enum
 */
public interface BaseTableResolver<T extends Enum<T>> {

    /** Returns the {@link TableColumn} of {@code table} identified by {@code key} (e.g. a column header). */
    TableColumn resolveColumn(T table, String key);
}
