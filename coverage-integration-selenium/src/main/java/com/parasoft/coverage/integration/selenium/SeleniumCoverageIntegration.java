/*
 * Copyright 2026 Parasoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.parasoft.coverage.integration.selenium;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.parasoft.coverage.integration.proxy.ParasoftHeaderInjectingProxy;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Provides Selenium Chrome configuration for the currently executing Parasoft
 * coverage test.
 */
public final class SeleniumCoverageIntegration
{
    private static final String LOOPBACK_PROXY_BYPASS = "<-loopback>";

    private SeleniumCoverageIntegration()
    {
    }

    /**
     * Starts a Parasoft header-injecting proxy using the current test's baggage
     * header and configures the supplied Chrome options to use it.
     *
     * @param options Chrome options to configure
     * @return proxy handle that must be closed after the browser session ends
     */
    public static ParasoftHeaderInjectingProxy configureChromeOptions(MutableCapabilities options)
    {
        requireChromeOptions(options);

        return configureChromeOptions(options, new ParasoftHeaderInjectingProxy());
    }

    /**
     * Starts a Parasoft header-injecting proxy using a shared baggage reference
     * and configures the supplied Chrome options to use it.
     *
     * @param options Chrome options to configure
     * @param baggageHeader shared baggage value to inject, such as
     *        {@code test-operator-id=userId+parallelId}
     * @return proxy handle that must be closed after the browser session ends
     */
    public static ParasoftHeaderInjectingProxy configureChromeOptions(MutableCapabilities options,
            AtomicReference<String> baggageHeader)
    {
        requireChromeOptions(options);

        return configureChromeOptions(options, new ParasoftHeaderInjectingProxy(baggageHeader));
    }

    /**
     * Configures the supplied Chrome options to use an existing Parasoft
     * header-injecting proxy.
     *
     * @param options Chrome options to configure
     * @param proxy Parasoft proxy to use
     * @return the supplied proxy
     */
    public static ParasoftHeaderInjectingProxy configureChromeOptions(MutableCapabilities options,
            ParasoftHeaderInjectingProxy proxy)
    {
        ChromeOptions chromeOptions = requireChromeOptions(options);
        ParasoftHeaderInjectingProxy parasoftProxy = Objects.requireNonNull(proxy, "proxy must not be null");

        String proxyAddress = parasoftProxy.getHost() + ":" + parasoftProxy.getPort();
        Proxy seleniumProxy = new Proxy()
                .setHttpProxy(proxyAddress)
                .setSslProxy(proxyAddress)
                .setNoProxy(LOOPBACK_PROXY_BYPASS);

        chromeOptions.setProxy(seleniumProxy);

        return parasoftProxy;
    }

    private static ChromeOptions requireChromeOptions(MutableCapabilities options)
    {
        Objects.requireNonNull(options, "options must not be null");

        if (options instanceof ChromeOptions chromeOptions) {
            return chromeOptions;
        }

        throw new IllegalArgumentException("Expected ChromeOptions but got " + options.getClass().getName());
    }
}
