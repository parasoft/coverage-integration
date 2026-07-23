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

package com.parasoft.coverage.integration.junit4;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;

public class ParasoftJUnit4RunListener
        extends RunListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftJUnit4RunListener.class);

    private final CoverageApiClient coverageApiClient;
    
    public ParasoftJUnit4RunListener()
    {
        this(CoverageApiClientFactory.createFromSettings());
    }

    public ParasoftJUnit4RunListener(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient = coverageApiClient;
    }

    @Override
    public void testRunStarted(Description description)
    {
        LOGGER.debug("JUnit 4 test run started: {}", description);
        ParasoftJUnit4Lifecycle.startSessionFromRunListener(coverageApiClient);
    }

    @Override
    public void testRunFinished(Result result)
    {
        LOGGER.debug("JUnit 4 test run finished: runCount={}, failureCount={}, ignoreCount={}",
                result.getRunCount(), result.getFailureCount(), result.getIgnoreCount());
        ParasoftJUnit4Lifecycle.stopSessionFromRunListener(coverageApiClient);
    }
}
