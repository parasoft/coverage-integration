/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.junit5;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;

public class ParasoftJUnit5SessionListener implements LauncherSessionListener
{
    private final CoverageApiClient coverageApiClient;

    public ParasoftJUnit5SessionListener()
    {
        this(CoverageApiClientFactory.createFromSettings());
    }

    public ParasoftJUnit5SessionListener(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient = coverageApiClient;
    }

    @Override
    public void launcherSessionOpened(LauncherSession session)
    {
        coverageApiClient.startSession();
    }

    @Override
    public void launcherSessionClosed(LauncherSession session)
    {
        coverageApiClient.stopSession();
    }
}
