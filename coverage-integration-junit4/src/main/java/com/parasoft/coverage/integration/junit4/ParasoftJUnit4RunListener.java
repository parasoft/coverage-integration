/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
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
        LOGGER.info("JUnit 4 test run started: {}", description);
        ParasoftJUnit4Lifecycle.startSessionFromRunListener(coverageApiClient);
    }

    @Override
    public void testRunFinished(Result result)
    {
        LOGGER.info("JUnit 4 test run finished: runCount={}, failureCount={}, ignoreCount={}",
                result.getRunCount(), result.getFailureCount(), result.getIgnoreCount());
        ParasoftJUnit4Lifecycle.stopSessionFromRunListener(coverageApiClient);
    }
}
