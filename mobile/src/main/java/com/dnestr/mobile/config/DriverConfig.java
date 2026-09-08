package com.dnestr.mobile.config;

import com.dnestr.mobile.enums.MobilePlatform;

import java.net.URL;

public interface DriverConfig {

    MobilePlatform platform();
    URL url();
}
