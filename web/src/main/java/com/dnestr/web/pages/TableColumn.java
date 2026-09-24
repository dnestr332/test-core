package com.dnestr.web.pages;

/**
 * Marker implemented by an app's table-column enum (one constant per column of a given
 * {@link PageTable}), giving that column's position so a page object can build a cell-level
 * locator (row + column) from it. Looked up via {@code BaseTableResolver<T>#resolveColumn}.
 */
public interface TableColumn {

    /** The column's position within its table, per the implementing app's own indexing convention (0- or 1-based). */
    int getIndex();
}
