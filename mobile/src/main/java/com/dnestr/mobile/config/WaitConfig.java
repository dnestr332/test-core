package com.dnestr.mobile.config;

public record WaitConfig(
        long longTimeout,
        long shortTimeout,
        long polling
) {

    public static WaitConfig of(
            long localLong,
            long ciLong,
            long localShort,
            long ciShort,
            long polling,
            boolean isCi
    ) {
        return new WaitConfig(
                isCi ? ciLong : localLong,
                isCi ? ciShort : localShort,
                polling
        );
    }
}
