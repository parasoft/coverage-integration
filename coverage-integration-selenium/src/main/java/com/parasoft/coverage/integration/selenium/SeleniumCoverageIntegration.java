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
import org.openqa.selenium.edge.EdgeOptions;

/**
 * Provides Selenium browser configuration for the currently executing
 * Parasoft coverage test.
 */
public final class SeleniumCoverageIntegration
{
    private static final String LOOPBACK_PROXY_BYPASS = "<-loopback>";

    private SeleniumCoverageIntegration()
    {
    }

    /**
     * Creates Chrome options configured with a dedicated Parasoft
     * header-injecting proxy for one browser session.
     * <p>
     * Call this method separately for each browser. The proxy captures the
     * current test's baggage header when the browser options are created, so
     * parallel browsers keep independent proxy and baggage state.
     * </p>
     *
     * @return browser coverage handle that must be closed after the browser
     *         session ends
     */
    public static SeleniumBrowserCoverage createChromeBrowserCoverage()
    {
        return createChromeBrowserCoverage(new ChromeOptions());
    }

    /**
     * Configures the supplied Chrome options with a dedicated Parasoft
     * header-injecting proxy for one browser session.
     * <p>
     * Call this method separately for each browser. The proxy captures the
     * current test's baggage header when the browser options are configured, so
     * parallel browsers keep independent proxy and baggage state.
     * </p>
     *
     * @param options Chrome options to configure
     * @return browser coverage handle that must be closed after the browser
     *         session ends
     */
    public static SeleniumBrowserCoverage createChromeBrowserCoverage(ChromeOptions options)
    {
        ChromeOptions chromeOptions = requireChromeOptions(options);
        ParasoftHeaderInjectingProxy proxy = configureChromeOptions(chromeOptions);

        return new SeleniumBrowserCoverage(chromeOptions, proxy);
    }

    /**
     * Creates Edge options configured with a dedicated Parasoft
     * header-injecting proxy for one browser session.
     * <p>
     * Call this method separately for each browser. The proxy captures the
     * current test's baggage header when the browser options are created, so
     * parallel browsers keep independent proxy and baggage state.
     * </p>
     *
     * @return Edge browser coverage handle that must be closed after the browser
     *         session ends
     */
    public static EdgeBrowserCoverage createEdgeBrowserCoverage()
    {
        return createEdgeBrowserCoverage(new EdgeOptions());
    }

    /**
     * Configures the supplied Edge options with a dedicated Parasoft
     * header-injecting proxy for one browser session.
     * <p>
     * Call this method separately for each browser. The proxy captures the
     * current test's baggage header when the browser options are configured, so
     * parallel browsers keep independent proxy and baggage state.
     * </p>
     *
     * @param options Edge options to configure
     * @return Edge browser coverage handle that must be closed after the browser
     *         session ends
     */
    public static EdgeBrowserCoverage createEdgeBrowserCoverage(EdgeOptions options)
    {
        EdgeOptions edgeOptions = requireEdgeOptions(options);
        ParasoftHeaderInjectingProxy proxy = configureEdgeOptions(edgeOptions);

        return new EdgeBrowserCoverage(edgeOptions, proxy);
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

    /**
     * Starts a Parasoft header-injecting proxy using the current test's baggage
     * header and configures the supplied Edge options to use it.
     *
     * @param options Edge options to configure
     * @return proxy handle that must be closed after the browser session ends
     */
    public static ParasoftHeaderInjectingProxy configureEdgeOptions(MutableCapabilities options)
    {
        requireEdgeOptions(options);

        return configureEdgeOptions(options, new ParasoftHeaderInjectingProxy());
    }

    /**
     * Starts a Parasoft header-injecting proxy using a shared baggage reference
     * and configures the supplied Edge options to use it.
     *
     * @param options Edge options to configure
     * @param baggageHeader shared baggage value to inject, such as
     *        {@code test-operator-id=userId+parallelId}
     * @return proxy handle that must be closed after the browser session ends
     */
    public static ParasoftHeaderInjectingProxy configureEdgeOptions(MutableCapabilities options,
            AtomicReference<String> baggageHeader)
    {
        requireEdgeOptions(options);

        return configureEdgeOptions(options, new ParasoftHeaderInjectingProxy(baggageHeader));
    }

    /**
     * Configures the supplied Edge options to use an existing Parasoft
     * header-injecting proxy.
     *
     * @param options Edge options to configure
     * @param proxy Parasoft proxy to use
     * @return the supplied proxy
     */
    public static ParasoftHeaderInjectingProxy configureEdgeOptions(MutableCapabilities options,
            ParasoftHeaderInjectingProxy proxy)
    {
        EdgeOptions edgeOptions = requireEdgeOptions(options);
        ParasoftHeaderInjectingProxy parasoftProxy = Objects.requireNonNull(proxy, "proxy must not be null");

        String proxyAddress = parasoftProxy.getHost() + ":" + parasoftProxy.getPort();
        Proxy seleniumProxy = new Proxy()
                .setHttpProxy(proxyAddress)
                .setSslProxy(proxyAddress)
                .setNoProxy(LOOPBACK_PROXY_BYPASS);

        edgeOptions.setProxy(seleniumProxy);

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

    private static EdgeOptions requireEdgeOptions(MutableCapabilities options)
    {
        Objects.requireNonNull(options, "options must not be null");

        if (options instanceof EdgeOptions edgeOptions) {
            return edgeOptions;
        }

        throw new IllegalArgumentException("Expected EdgeOptions but got " + options.getClass().getName());
    }

    /**
     * Chrome browser coverage state for one Selenium browser session.
     */
    public static final class SeleniumBrowserCoverage
            implements AutoCloseable
    {
        private final ChromeOptions chromeOptions;
        private final ParasoftHeaderInjectingProxy proxy;

        private SeleniumBrowserCoverage(ChromeOptions chromeOptions, ParasoftHeaderInjectingProxy proxy)
        {
            this.chromeOptions = chromeOptions;
            this.proxy = proxy;
        }

        /**
         * @return Chrome options configured for this browser session
         */
        public ChromeOptions getChromeOptions()
        {
            return chromeOptions;
        }

        /**
         * @return the dedicated Parasoft proxy for this browser session
         */
        public ParasoftHeaderInjectingProxy getProxy()
        {
            return proxy;
        }

        @Override
        public void close()
        {
            proxy.close();
        }
    }

    /**
     * Edge browser coverage state for one Selenium browser session.
     */
    public static final class EdgeBrowserCoverage
            implements AutoCloseable
    {
        private final EdgeOptions edgeOptions;
        private final ParasoftHeaderInjectingProxy proxy;

        private EdgeBrowserCoverage(EdgeOptions edgeOptions, ParasoftHeaderInjectingProxy proxy)
        {
            this.edgeOptions = edgeOptions;
            this.proxy = proxy;
        }

        /**
         * @return Edge options configured for this browser session
         */
        public EdgeOptions getEdgeOptions()
        {
            return edgeOptions;
        }

        /**
         * @return the dedicated Parasoft proxy for this browser session
         */
        public ParasoftHeaderInjectingProxy getProxy()
        {
            return proxy;
        }

        @Override
        public void close()
        {
            proxy.close();
        }
    }
}
