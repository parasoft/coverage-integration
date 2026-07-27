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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parasoft.coverage.integration.proxy.ParasoftHeaderInjectingProxy;
import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Provides Selenium browser configuration for the currently executing
 * Parasoft coverage test.
 */
public final class SeleniumCoverageIntegration
{
    private static final String BAGGAGE_HEADER_NAME = ParasoftHeaderInjectingProxy.BAGGAGE_HEADER_NAME;
    private static final String LOOPBACK_PROXY_BYPASS = "<-loopback>";
    private static final String FIREFOX_ALLOW_HIJACKING_LOCALHOST = "network.proxy.allow_hijacking_localhost";
    private static final String NETWORK_ENABLE_COMMAND = "Network.enable";
    private static final String NETWORK_SET_EXTRA_HTTP_HEADERS_COMMAND = "Network.setExtraHTTPHeaders";
    private static final String HEADERS_PARAMETER_NAME = "headers";

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleniumCoverageIntegration.class);

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
    public static ChromeCoverageConfig createChromeBrowserCoverage()
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
    public static ChromeCoverageConfig createChromeBrowserCoverage(ChromeOptions options)
    {
        ChromeOptions chromeOptions = requireChromeOptions(options);
        ParasoftHeaderInjectingProxy proxy = configureChromeOptions(chromeOptions);

        return new ChromeCoverageConfig(chromeOptions, proxy);
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
    public static EdgeCoverageConfig createEdgeBrowserCoverage()
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
    public static EdgeCoverageConfig createEdgeBrowserCoverage(EdgeOptions options)
    {
        EdgeOptions edgeOptions = requireEdgeOptions(options);
        ParasoftHeaderInjectingProxy proxy = configureEdgeOptions(edgeOptions);

        return new EdgeCoverageConfig(edgeOptions, proxy);
    }

    /**
     * Creates Firefox options configured with a dedicated Parasoft
     * header-injecting proxy for one browser session.
     * <p>
     * Call this method separately for each browser. The proxy captures the
     * current test's baggage header when the browser options are created, so
     * parallel browsers keep independent proxy and baggage state.
     * </p>
     *
     * @return Firefox browser coverage handle that must be closed after the
     *         browser session ends
     */
    public static FirefoxCoverageConfig createFirefoxBrowserCoverage()
    {
        return createFirefoxBrowserCoverage(new FirefoxOptions());
    }

    /**
     * Configures the supplied Firefox options with a dedicated Parasoft
     * header-injecting proxy for one browser session.
     * <p>
     * Call this method separately for each browser. The proxy captures the
     * current test's baggage header when the browser options are configured, so
     * parallel browsers keep independent proxy and baggage state.
     * </p>
     *
     * @param options Firefox options to configure
     * @return Firefox browser coverage handle that must be closed after the
     *         browser session ends
     */
    public static FirefoxCoverageConfig createFirefoxBrowserCoverage(FirefoxOptions options)
    {
        FirefoxOptions firefoxOptions = requireFirefoxOptions(options);
        ParasoftHeaderInjectingProxy proxy = configureFirefoxOptions(firefoxOptions);

        return new FirefoxCoverageConfig(firefoxOptions, proxy);
    }

    /**
     * Configures a Chrome or Edge browser session to inject the current test's
     * {@code Baggage} header using Chrome DevTools Protocol instead of a proxy.
     * <p>
     * Call this method after creating the {@code WebDriver} session and before
     * navigating to the application under test. Call it separately for each
     * browser session used by parallel tests.
     * </p>
     *
     * @param browser Chrome or Edge driver that supports CDP
     */
    public static void configureCdpBaggageHeader(HasCdp browser)
    {
        configureCdpBaggageHeader(browser, CoverageExecutionContext.getCurrentBaggageHeader());
    }

    /**
     * Configures a Chrome or Edge browser session to inject the supplied
     * {@code Baggage} header using Chrome DevTools Protocol instead of a proxy.
     * <p>
     * Passing {@code null} or a blank value clears previously configured extra
     * HTTP headers for this CDP session.
     * </p>
     *
     * @param browser Chrome or Edge driver that supports CDP
     * @param baggageHeader baggage value to inject, such as
     *        {@code test-operator-id=userId+parallelId}
     */
    public static void configureCdpBaggageHeader(HasCdp browser, String baggageHeader)
    {
        configureCdpHeaders(browser, createCdpBaggageHeaders(baggageHeader));
    }

    /**
     * Configures a Chrome or Edge browser session to inject the supplied HTTP
     * headers using Chrome DevTools Protocol instead of a proxy.
     * <p>
     * Passing {@code null} clears previously configured extra HTTP headers for
     * this CDP session.
     * </p>
     *
     * @param browser Chrome or Edge driver that supports CDP
     * @param headers HTTP headers to inject
     */
    public static void configureCdpHeaders(HasCdp browser, Map<String, String> headers)
    {
        HasCdp cdpBrowser = Objects.requireNonNull(browser, "browser must not be null");
        Map<String, String> extraHeaders = headers == null ? Collections.emptyMap() : Map.copyOf(headers);

        cdpBrowser.executeCdpCommand(NETWORK_ENABLE_COMMAND, Collections.emptyMap());
        cdpBrowser.executeCdpCommand(NETWORK_SET_EXTRA_HTTP_HEADERS_COMMAND,
                Map.of(HEADERS_PARAMETER_NAME, extraHeaders));
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

        chromeOptions.setProxy(createChromiumSeleniumProxy(parasoftProxy));

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

        edgeOptions.setProxy(createChromiumSeleniumProxy(parasoftProxy));

        return parasoftProxy;
    }

    /**
     * Starts a Parasoft header-injecting proxy using the current test's baggage
     * header and configures the supplied Firefox options to use it.
     *
     * @param options Firefox options to configure
     * @return proxy handle that must be closed after the browser session ends
     */
    public static ParasoftHeaderInjectingProxy configureFirefoxOptions(MutableCapabilities options)
    {
        requireFirefoxOptions(options);

        return configureFirefoxOptions(options, new ParasoftHeaderInjectingProxy());
    }

    /**
     * Starts a Parasoft header-injecting proxy using a shared baggage reference
     * and configures the supplied Firefox options to use it.
     *
     * @param options Firefox options to configure
     * @param baggageHeader shared baggage value to inject, such as
     *        {@code test-operator-id=userId+parallelId}
     * @return proxy handle that must be closed after the browser session ends
     */
    public static ParasoftHeaderInjectingProxy configureFirefoxOptions(MutableCapabilities options,
            AtomicReference<String> baggageHeader)
    {
        requireFirefoxOptions(options);

        return configureFirefoxOptions(options, new ParasoftHeaderInjectingProxy(baggageHeader));
    }

    /**
     * Configures the supplied Firefox options to use an existing Parasoft
     * header-injecting proxy.
     *
     * @param options Firefox options to configure
     * @param proxy Parasoft proxy to use
     * @return the supplied proxy
     */
    public static ParasoftHeaderInjectingProxy configureFirefoxOptions(MutableCapabilities options,
            ParasoftHeaderInjectingProxy proxy)
    {
        FirefoxOptions firefoxOptions = requireFirefoxOptions(options);
        ParasoftHeaderInjectingProxy parasoftProxy = Objects.requireNonNull(proxy, "proxy must not be null");

        firefoxOptions.setProxy(createSeleniumProxy(parasoftProxy));
        firefoxOptions.addPreference(FIREFOX_ALLOW_HIJACKING_LOCALHOST, true);

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

    private static FirefoxOptions requireFirefoxOptions(MutableCapabilities options)
    {
        Objects.requireNonNull(options, "options must not be null");

        if (options instanceof FirefoxOptions firefoxOptions) {
            return firefoxOptions;
        }

        throw new IllegalArgumentException("Expected FirefoxOptions but got " + options.getClass().getName());
    }

    private static Proxy createSeleniumProxy(ParasoftHeaderInjectingProxy parasoftProxy)
    {
        String proxyAddress = parasoftProxy.getHost() + ":" + parasoftProxy.getPort();

        return new Proxy()
                .setHttpProxy(proxyAddress)
                .setSslProxy(proxyAddress);
    }

    private static Proxy createChromiumSeleniumProxy(ParasoftHeaderInjectingProxy parasoftProxy)
    {
        return createSeleniumProxy(parasoftProxy)
                .setNoProxy(LOOPBACK_PROXY_BYPASS);
    }

    private static Map<String, String> createCdpBaggageHeaders(String baggageHeader)
    {
        if (baggageHeader == null || baggageHeader.isBlank()) {
            return Collections.emptyMap();
        }

        return Map.of(BAGGAGE_HEADER_NAME, baggageHeader);
    }

    /**
     * Chrome browser coverage state for one Selenium browser session.
     */
    public static final class ChromeCoverageConfig
            implements AutoCloseable
    {
        private final ChromeOptions chromeOptions;
        private final ParasoftHeaderInjectingProxy proxy;

        private ChromeCoverageConfig(ChromeOptions chromeOptions, ParasoftHeaderInjectingProxy proxy)
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
    public static final class EdgeCoverageConfig
            implements AutoCloseable
    {
        private final EdgeOptions edgeOptions;
        private final ParasoftHeaderInjectingProxy proxy;

        private EdgeCoverageConfig(EdgeOptions edgeOptions, ParasoftHeaderInjectingProxy proxy)
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

    /**
     * Firefox browser coverage state for one Selenium browser session.
     */
    public static final class FirefoxCoverageConfig
            implements AutoCloseable
    {
        private final FirefoxOptions firefoxOptions;
        private final ParasoftHeaderInjectingProxy proxy;

        private FirefoxCoverageConfig(FirefoxOptions firefoxOptions, ParasoftHeaderInjectingProxy proxy)
        {
            this.firefoxOptions = firefoxOptions;
            this.proxy = proxy;
        }

        /**
         * @return Firefox options configured for this browser session
         */
        public FirefoxOptions getFirefoxOptions()
        {
            return firefoxOptions;
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
