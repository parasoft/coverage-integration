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
