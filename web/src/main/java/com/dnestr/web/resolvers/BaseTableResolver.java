package com.dnestr.web.resolvers;

import com.dnestr.web.pages.TableColumn;

public interface BaseTableResolver<T extends Enum<T>> {

    TableColumn resolveColumn(T table, String key);
}
