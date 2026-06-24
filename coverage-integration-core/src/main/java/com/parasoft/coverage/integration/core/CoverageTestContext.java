/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.core;

public final class CoverageTestContext
{
    private final String parallelId;
    private final String baggageHeader;

    public CoverageTestContext(String parallelId, String baggageHeader)
    {
        this.parallelId = parallelId;
        this.baggageHeader = baggageHeader;
    }

    public String getParallelId()
    {
        return parallelId;
    }

    public String getBaggageHeader()
    {
        return baggageHeader;
    }
}
