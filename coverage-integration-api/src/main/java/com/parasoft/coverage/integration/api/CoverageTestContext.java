/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.api;

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
     * Gets the Baggage header value containing the current
     * {@code test-operator-id} returned by the CTP {@code /test/start} API.
     *
     * @return the {@code Baggage} header value containing
     *         {@code test-operator-id}, or {@code null} when CTP did not return
     *         one
     */
    public String getCurrentTestOperatorIdHeader()
    {
        return getBaggageHeader();
    }

    com.parasoft.coverage.integration.core.CoverageTestContext delegate()
    {
        return delegate;
    }
}
