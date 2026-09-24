package com.dnestr.mobile.config;

/**
 * Timeout/polling settings for {@code DriverWait}.
 *
 * @param longTimeout  seconds allowed for a normal element wait (e.g. {@code DriverWait#visible})
 * @param shortTimeout seconds allowed for a "quick check" wait (e.g. {@code DriverWait#visibleShort})
 * @param polling      milliseconds between poll attempts, used for both timeouts
 */
public record WaitConfig(
        long longTimeout,
        long shortTimeout,
        long polling
) {

    /** Builds a {@link WaitConfig} picking the CI or local long/short timeout pair based on {@code isCi} (CI environments typically need longer timeouts than a local dev machine). */
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
