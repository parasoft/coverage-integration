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

import java.util.Map;

import com.parasoft.coverage.integration.proxy.ParasoftHeaderInjectingProxy;
import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.EdgeBrowserCoverage;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.FirefoxBrowserCoverage;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.SeleniumBrowserCoverage;

import org.junit.Test;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;

public class SeleniumCoverageIntegrationTest
{
    @Test
    public void createChromeBrowserCoverageCreatesSeparateProxyForEachBrowser()
    {
        try {
            CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-1", "baggage-one"));

            try (SeleniumBrowserCoverage firstBrowser = SeleniumCoverageIntegration.createChromeBrowserCoverage()) {
                CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-2", "baggage-two"));

                try (SeleniumBrowserCoverage secondBrowser = SeleniumCoverageIntegration.createChromeBrowserCoverage()) {
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

        try (SeleniumBrowserCoverage browserCoverage =
                SeleniumCoverageIntegration.createChromeBrowserCoverage(options)) {
            assertSame(options, browserCoverage.getChromeOptions());
            assertNull(browserCoverage.getProxy().getBaggageHeader());
        }
    }

    @Test
    public void createEdgeBrowserCoverageCreatesSeparateProxyForEachBrowser()
    {
        try {
            CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-1", "baggage-one"));

            try (EdgeBrowserCoverage firstBrowser = SeleniumCoverageIntegration.createEdgeBrowserCoverage()) {
                CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-2", "baggage-two"));

                try (EdgeBrowserCoverage secondBrowser = SeleniumCoverageIntegration.createEdgeBrowserCoverage()) {
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

        try (EdgeBrowserCoverage browserCoverage =
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

            try (FirefoxBrowserCoverage firstBrowser = SeleniumCoverageIntegration.createFirefoxBrowserCoverage()) {
                CoverageExecutionContext.setCurrent(new CoverageTestContext("parallel-2", "baggage-two"));

                try (FirefoxBrowserCoverage secondBrowser =
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

        try (FirefoxBrowserCoverage browserCoverage =
                SeleniumCoverageIntegration.createFirefoxBrowserCoverage(options)) {
            assertSame(options, browserCoverage.getFirefoxOptions());
            assertNull(browserCoverage.getProxy().getBaggageHeader());

            Proxy seleniumProxy = (Proxy) options.getCapability(CapabilityType.PROXY);
            assertProxy(browserCoverage.getProxy(), seleniumProxy);
            assertNull(seleniumProxy.getNoProxy());
            assertFirefoxPreference(options, "network.proxy.allow_hijacking_localhost", true);
        }
    }

    private static void assertChromiumProxy(ParasoftHeaderInjectingProxy proxy, Proxy seleniumProxy)
    {
        assertProxy(proxy, seleniumProxy);
        assertEquals("<-loopback>", seleniumProxy.getNoProxy());
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
}
