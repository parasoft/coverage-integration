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

package com.parasoft.coverage.integration.playwright;

import java.util.Map;

import org.slf4j.Logger;

import com.microsoft.playwright.Browser;
import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;

/**
 * Provides Playwright configuration for the currently executing Parasoft
 * coverage test.
 */
public final class PlaywrightCoverageIntegration
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PlaywrightCoverageIntegration.class);
    private static final String BAGGAGE_HEADER_NAME = "Baggage";

    private PlaywrightCoverageIntegration()
    {
    }

    /**
     * Creates browser context options containing the Baggage header returned by
     * CTP for the currently executing test.
     * <p>
     * When the current test has no baggage value, such as in single-user mode,
     * the returned options contain no additional HTTP headers.
     * </p>
     *
     * @return new Playwright browser context options for the current test
     */
    public static Browser.NewContextOptions createBrowserContextOptions()
    {
        Browser.NewContextOptions options = new Browser.NewContextOptions();
        updateBrowserContextOptions(options);
        return options;
    }

    /**
     * Updates the given Playwright browser context options with the Baggage header
     * returned by CTP for the currently executing test.
     * <p>
     * When the current test has no baggage value, such as in single-user mode,
     * the options remain unchanged.
     * </p>
     *
     * @param options the Playwright browser context options to update
     */
    public static void updateBrowserContextOptions(Browser.NewContextOptions options)
    {
        String baggageHeader = CoverageExecutionContext.getCurrentBaggageHeader();
        if (baggageHeader != null && !baggageHeader.isBlank()) {
            LOGGER.debug("Setting Baggage header for Playwright browser context: {}", baggageHeader);
            options.setExtraHTTPHeaders(Map.of(BAGGAGE_HEADER_NAME, baggageHeader));
        }
    }
}
