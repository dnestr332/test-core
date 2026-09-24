package com.dnestr.web.pages;

/**
 * Marker implemented by an app's table enum (one constant per data table/grid on a page), giving
 * the locator for that table's root element. Paired with {@link TableColumn} (a column within the
 * table) and looked up via {@code BaseTableResolver<T>}.
 */
public interface PageTable {

    /** A locator string (e.g. a CSS selector) identifying the table's root element on the page. */
    String getLocator();
}
