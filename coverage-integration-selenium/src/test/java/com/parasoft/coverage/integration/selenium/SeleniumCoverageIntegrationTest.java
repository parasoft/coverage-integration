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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.parasoft.coverage.integration.proxy.ParasoftHeaderInjectingProxy;
import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.EdgeCoverageConfig;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.FirefoxCoverageConfig;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.SafariCoverageConfig;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.ChromeCoverageConfig;

import org.junit.Test;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.remote.CapabilityType;

public class SeleniumCoverageIntegrationTest
{
    @Test
    public void createChromeBrowserCoverageCreatesSeparateProxyForEachBrowser()
    {
        try {
            CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-1", "baggage-one"));

            try (ChromeCoverageConfig firstBrowser = SeleniumCoverageIntegration.createChromeBrowserCoverage()) {
                CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-2", "baggage-two"));

                try (ChromeCoverageConfig secondBrowser = SeleniumCoverageIntegration.createChromeBrowserCoverage()) {
                    assertNotSame(firstBrowser.getProxy(), secondBrowser.getProxy());
                    assertNotEquals(firstBrowser.getProxy().getPort(), secondBrowser.getProxy().getPort());
                    assertEquals("baggage-one", firstBrowser.getProxy().getBaggageHeader());
                    assertEquals("baggage-two", secondBrowser.getProxy().getBaggageHeader());
                }
            }
        }
        finally {
            CoverageExecutionContext.clearCurrent();
        }
    }

    @Test
    public void createChromeBrowserCoverageReturnsConfiguredOptions()
    {
        ChromeOptions options = new ChromeOptions();

        try (ChromeCoverageConfig browserCoverage =
                SeleniumCoverageIntegration.createChromeBrowserCoverage(options)) {
            assertSame(options, browserCoverage.getChromeOptions());
            assertNull(browserCoverage.getProxy().getBaggageHeader());
        }
    }

    @Test
    public void configureCdpBaggageHeaderUsesCurrentTestBaggage()
    {
        RecordingCdpBrowser browser = new RecordingCdpBrowser();

        try {
            CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-1", "baggage-one"));

            SeleniumCoverageIntegration.configureCdpBaggageHeader(browser);

            assertCdpBaggageCommands(browser, "baggage-one");
        }
        finally {
            CoverageExecutionContext.clearCurrent();
        }
    }

    @Test
    public void configureCdpBaggageHeaderUsesSuppliedBaggage()
    {
        RecordingCdpBrowser browser = new RecordingCdpBrowser();

        SeleniumCoverageIntegration.configureCdpBaggageHeader(browser, "baggage-two");

        assertCdpBaggageCommands(browser, "baggage-two");
    }

    @Test
    public void configureCdpBaggageHeaderClearsHeadersWhenBaggageIsBlank()
    {
        RecordingCdpBrowser browser = new RecordingCdpBrowser();

        SeleniumCoverageIntegration.configureCdpBaggageHeader(browser, " ");

        assertEquals("Network.enable", browser.commandNames.get(0));
        assertEquals("Network.setExtraHTTPHeaders", browser.commandNames.get(1));
        assertEquals(Map.of(), browser.commandParameters.get(1).get("headers"));
    }

    @Test
    public void configureCdpHeadersUsesSuppliedHeaders()
    {
        RecordingCdpBrowser browser = new RecordingCdpBrowser();

        SeleniumCoverageIntegration.configureCdpHeaders(browser,
                Map.of("Baggage", "baggage-three", "X-Test", "value"));

        assertEquals(List.of("Network.enable", "Network.setExtraHTTPHeaders"), browser.commandNames);
        assertEquals(Map.of("Baggage", "baggage-three", "X-Test", "value"),
                browser.commandParameters.get(1).get("headers"));
    }

    @Test
    public void createEdgeBrowserCoverageCreatesSeparateProxyForEachBrowser()
    {
        try {
            CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-1", "baggage-one"));

            try (EdgeCoverageConfig firstBrowser = SeleniumCoverageIntegration.createEdgeBrowserCoverage()) {
                CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-2", "baggage-two"));

                try (EdgeCoverageConfig secondBrowser = SeleniumCoverageIntegration.createEdgeBrowserCoverage()) {
                    assertNotSame(firstBrowser.getProxy(), secondBrowser.getProxy());
                    assertNotEquals(firstBrowser.getProxy().getPort(), secondBrowser.getProxy().getPort());
                    assertEquals("baggage-one", firstBrowser.getProxy().getBaggageHeader());
                    assertEquals("baggage-two", secondBrowser.getProxy().getBaggageHeader());
                }
            }
        }
        finally {
            CoverageExecutionContext.clearCurrent();
        }
    }

    @Test
    public void createEdgeBrowserCoverageReturnsConfiguredOptions()
    {
        EdgeOptions options = new EdgeOptions();

        try (EdgeCoverageConfig browserCoverage =
                SeleniumCoverageIntegration.createEdgeBrowserCoverage(options)) {
            assertSame(options, browserCoverage.getEdgeOptions());
            assertNull(browserCoverage.getProxy().getBaggageHeader());

            Proxy seleniumProxy = (Proxy) options.getCapability(CapabilityType.PROXY);
            assertChromiumProxy(browserCoverage.getProxy(), seleniumProxy);
        }
    }

    @Test
    public void createFirefoxBrowserCoverageCreatesSeparateProxyForEachBrowser()
    {
        try {
            CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-1", "baggage-one"));

            try (FirefoxCoverageConfig firstBrowser = SeleniumCoverageIntegration.createFirefoxBrowserCoverage()) {
                CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-2", "baggage-two"));

                try (FirefoxCoverageConfig secondBrowser =
                        SeleniumCoverageIntegration.createFirefoxBrowserCoverage()) {
                    assertNotSame(firstBrowser.getProxy(), secondBrowser.getProxy());
                    assertNotEquals(firstBrowser.getProxy().getPort(), secondBrowser.getProxy().getPort());
                    assertEquals("baggage-one", firstBrowser.getProxy().getBaggageHeader());
                    assertEquals("baggage-two", secondBrowser.getProxy().getBaggageHeader());
                }
            }
        }
        finally {
            CoverageExecutionContext.clearCurrent();
        }
    }

    @Test
    public void createFirefoxBrowserCoverageReturnsConfiguredOptions()
    {
        FirefoxOptions options = new FirefoxOptions();

        try (FirefoxCoverageConfig browserCoverage =
                SeleniumCoverageIntegration.createFirefoxBrowserCoverage(options)) {
            assertSame(options, browserCoverage.getFirefoxOptions());
            assertNull(browserCoverage.getProxy().getBaggageHeader());

            Proxy seleniumProxy = (Proxy) options.getCapability(CapabilityType.PROXY);
            assertProxy(browserCoverage.getProxy(), seleniumProxy);
            assertNull(seleniumProxy.getNoProxy());
            assertFirefoxPreference(options, "network.proxy.allow_hijacking_localhost", true);
        }
    }

    @Test
    public void createSafariBrowserCoverageCreatesSeparateProxyForEachBrowser()
    {
        try {
            CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-1", "baggage-one"));

            try (SafariCoverageConfig firstBrowser = SeleniumCoverageIntegration.createSafariBrowserCoverage()) {
                CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-2", "baggage-two"));

                try (SafariCoverageConfig secondBrowser = SeleniumCoverageIntegration.createSafariBrowserCoverage()) {
                    assertNotSame(firstBrowser.getProxy(), secondBrowser.getProxy());
                    assertNotEquals(firstBrowser.getProxy().getPort(), secondBrowser.getProxy().getPort());
                    assertEquals("baggage-one", firstBrowser.getProxy().getBaggageHeader());
                    assertEquals("baggage-two", secondBrowser.getProxy().getBaggageHeader());
                }
            }
        }
        finally {
            CoverageExecutionContext.clearCurrent();
        }
    }

    @Test
    public void createSafariBrowserCoverageReturnsConfiguredOptions()
    {
        SafariOptions options = new SafariOptions();

        try (SafariCoverageConfig browserCoverage =
                SeleniumCoverageIntegration.createSafariBrowserCoverage(options)) {
            assertSame(options, browserCoverage.getSafariOptions());
            assertNull(browserCoverage.getProxy().getBaggageHeader());

            Proxy seleniumProxy = (Proxy) options.getCapability(CapabilityType.PROXY);
            assertProxy(browserCoverage.getProxy(), seleniumProxy);
            assertNull(seleniumProxy.getNoProxy());
        }
    }

    private static void assertChromiumProxy(ParasoftHeaderInjectingProxy proxy, Proxy seleniumProxy)
    {
        assertProxy(proxy, seleniumProxy);
        assertEquals("<-loopback>", seleniumProxy.getNoProxy());
    }

    @SuppressWarnings("unchecked")
    private static void assertCdpBaggageCommands(RecordingCdpBrowser browser, String baggageHeader)
    {
        assertEquals(List.of("Network.enable", "Network.setExtraHTTPHeaders"), browser.commandNames);
        assertEquals(Map.of(), browser.commandParameters.get(0));

        Map<String, String> headers = (Map<String, String>) browser.commandParameters.get(1).get("headers");
        assertEquals(Map.of("Baggage", baggageHeader), headers);
    }

    private static void assertProxy(ParasoftHeaderInjectingProxy proxy, Proxy seleniumProxy)
    {
        assertNotNull(seleniumProxy);

        String expectedProxyAddress = proxy.getHost() + ":" + proxy.getPort();

        assertEquals(expectedProxyAddress, seleniumProxy.getHttpProxy());
        assertEquals(expectedProxyAddress, seleniumProxy.getSslProxy());
    }

    @SuppressWarnings("unchecked")
    private static void assertFirefoxPreference(FirefoxOptions options, String name, Object expectedValue)
    {
        Map<String, Object> firefoxOptions = (Map<String, Object>) options.asMap().get("moz:firefoxOptions");
        Map<String, Object> preferences = (Map<String, Object>) firefoxOptions.get("prefs");

        assertEquals(expectedValue, preferences.get(name));
    }

    private static final class RecordingCdpBrowser
            implements HasCdp
    {
        private final List<String> commandNames = new ArrayList<>();
        private final List<Map<String, Object>> commandParameters = new ArrayList<>();

        @Override
        public Map<String, Object> executeCdpCommand(String commandName, Map<String, Object> parameters)
        {
            commandNames.add(commandName);
            commandParameters.add(parameters);

            return Map.of();
        }
    }
}
