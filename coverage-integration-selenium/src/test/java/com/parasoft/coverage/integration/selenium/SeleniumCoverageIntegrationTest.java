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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.SeleniumBrowserCoverage;

import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;

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
}
