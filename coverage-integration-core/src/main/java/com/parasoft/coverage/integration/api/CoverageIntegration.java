/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.api;

import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;

public final class CoverageIntegration
{
    public static final String BAGGAGE_HEADER_NAME = "Baggage";

    private CoverageIntegration()
    {
    }

    /**
     * Returns the value to use for the Baggage HTTP header for the currently
     * executing test.
     *
     * @return the Baggage header value, or {@code null} when no baggage value
     *         applies to the current test
     */
    public static String getBaggageHeader()
    {
        return CoverageExecutionContext.getCurrentBaggageHeader();
    }
}
