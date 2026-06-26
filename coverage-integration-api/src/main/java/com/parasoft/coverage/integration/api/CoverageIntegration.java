/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.api;

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
     * Returns the Baggage HTTP header value that contains the current
     * {@code test-operator-id} returned by the CTP {@code /test/start} API.
     *
     * @return the Baggage header value containing {@code test-operator-id}, or
     *         {@code null} when no current test operator ID applies to the
     *         current test
     */
    public static String getCurrentTestOperatorIdHeader()
    {
        return getBaggageHeader();
    }
}
