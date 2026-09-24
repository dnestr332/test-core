package com.dnestr.base.actions;

/**
 * Label describing what kind of step is being performed, passed to {@code BaseFailureCatcher}/
 * {@code BasePrettyPrinter} purely for logging (the enum name is what shows up in the "action" part
 * of a log line, e.g. {@code CLICK  [GetByRoleLocator] role=button}) — it has no other behavior.
 */
public enum ElementAction {

    FIND,
    FIND_LIST,
    CLICK,
    CLICK_BY_NATIVE,
    TYPE,
    GET_TEXT,
    SCROLL,
    ZOOM,
    HOVER
}
