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

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;

public class ParasoftJUnit4RunListener
        extends RunListener
{
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
        ParasoftJUnit4Lifecycle.startSessionFromRunListener(coverageApiClient);
    }

    @Override
    public void testRunFinished(Result result)
    {
        ParasoftJUnit4Lifecycle.stopSessionFromRunListener(coverageApiClient);
    }
}