/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.core.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.parasoft.coverage.integration.core.CoverageTestContext;

public final class CoverageExecutionContext
{
    private static final Map<Long, CoverageTestContext> CURRENT_TESTS = new ConcurrentHashMap<>();

    private CoverageExecutionContext()
    {
    }

    public static long setCurrent(CoverageTestContext testContext)
    {
        long threadId = Thread.currentThread().getId();

        if (testContext == null) {
            CURRENT_TESTS.remove(threadId);
        }
        else {
            CURRENT_TESTS.put(threadId, testContext);
        }

        return threadId;
    }

    public static String getCurrentBaggageHeader()
    {
        CoverageTestContext testContext = CURRENT_TESTS.get(Thread.currentThread().getId());

        return testContext == null ? null : testContext.getBaggageHeader();
    }

    public static void clearCurrent()
    {
        CURRENT_TESTS.remove(Thread.currentThread().getId());
    }

    public static void clear(long threadId)
    {
        if (threadId >= 0) {
            CURRENT_TESTS.remove(threadId);
        }
    }
}
