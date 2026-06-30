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

/**
 * Context returned by the CTP {@code /test/start} API for an individual test.
 */
public final class CoverageTestContext
{
    private final com.parasoft.coverage.integration.core.CoverageTestContext delegate;

    CoverageTestContext(com.parasoft.coverage.integration.core.CoverageTestContext delegate)
    {
        this.delegate = delegate;
    }

    /**
     * Gets the parallel execution identifier used for this test.
     *
     * @return the parallel ID, or {@code null} when parallel ID mode is disabled
     */
    public String getParallelId()
    {
        return delegate == null ? null : delegate.getParallelId();
    }

    /**
     * Gets the W3C Baggage header value returned by the CTP
     * {@code /test/start} API.
     *
     * @return the {@code Baggage} header value, or {@code null} when CTP did not
     *         return one
     */
    public String getBaggageHeader()
    {
        return delegate == null ? null : delegate.getBaggageHeader();
    }

    /**
     * Gets the Baggage header containing the current {@code test-operator-id}.
     *
     * @return a map containing the {@code Baggage} header name and value, or an
     *         empty map when no header value is available
     */
    public Map<String, String> getCurrentTestOperatorIdHeader()
    {
        String baggageHeader = getBaggageHeader();

        return baggageHeader == null ? Collections.emptyMap()
                : Collections.singletonMap(CoverageIntegration.BAGGAGE_HEADER_NAME, baggageHeader);
    }

    com.parasoft.coverage.integration.core.CoverageTestContext delegate()
    {
        return delegate;
    }
}
