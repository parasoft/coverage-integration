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

package com.parasoft.coverage.integration.api;

import java.util.Collections;
import java.util.Map;

import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;

/**
 * Utility methods for user tests and applications under test that need access
 * to the current Parasoft coverage integration context.
 */
public final class CoverageIntegration
{
    /**
     * Name of the HTTP header that carries W3C baggage.
     */
    public static final String BAGGAGE_HEADER_NAME = "Baggage";

    /**
     * W3C baggage key used for the current test operator identifier.
     */
    public static final String TEST_OPERATOR_ID_BAGGAGE_KEY = "test-operator-id";

    private CoverageIntegration()
    {
    }

    /**
     * Creates a coverage API client from {@code coverage-integration.properties}
     * and system properties.
     * <p>
     * This is intended for rare standalone use cases, such as a {@code main}
     * method or custom test harness. JUnit users should prefer the JUnit
     * integration modules.
     * </p>
     *
     * @return a coverage API client
     */
    public static CoverageApiClient createApiClient()
    {
        return CoverageApiClient.createFromSettings();
    }

    /**
     * Returns the value to use for the Baggage HTTP header for the currently
     * executing test.
     *
     * @return the Baggage header value returned by the CTP {@code /test/start}
     *         API, or {@code null} when no baggage value applies to the current
     *         test
     */
    public static String getBaggageHeader()
    {
        return CoverageExecutionContext.getCurrentBaggageHeader();
    }

    /**
     * Returns the Baggage HTTP header that contains the current
     * {@code test-operator-id} returned by the CTP {@code /test/start} API.
     *
     * @return a map containing the {@code Baggage} header name and value, or an
     *         empty map when no current test operator ID applies to the current
     *         test
     */
    public static Map<String, String> getCurrentTestOperatorIdHeader()
    {
        String baggageHeader = getBaggageHeader();

        return baggageHeader == null ? Collections.emptyMap()
                : Collections.singletonMap(BAGGAGE_HEADER_NAME, baggageHeader);
    }
}
