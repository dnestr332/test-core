package com.dnestr.base.utils;

import java.util.Comparator;
import java.util.List;

/** Small general-purpose helpers that have no closer home. */
public final class CommonUtils {

    private CommonUtils() {}

    /**
     * The simple class name of {@code object}, without the suffix a runtime proxy appends after a {@code $} (Spring's
     * CGLIB, ByteBuddy, Hibernate), so a proxied object logs as {@code LoginScreen} rather than
     * {@code LoginScreen$$SpringCGLIB$$0}.
     */
    public static String prettyClassName(Object object) {
        String name = object.getClass().getSimpleName();
        int proxySuffix = name.indexOf('$');
        return proxySuffix > 0 ? name.substring(0, proxySuffix) : name;
    }

    /** Whether no element of {@code list} sorts before the one ahead of it; equal neighbours count as sorted. */
    public static <T> boolean isSorted(List<T> list, Comparator<? super T> order) {
        for (int i = 1; i < list.size(); i++) {
            if (order.compare(list.get(i - 1), list.get(i)) > 0) return false;
        }
        return true;
    }
}
