/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.junit5;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;

public class ParasoftJUnit5SessionListener implements LauncherSessionListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftJUnit5SessionListener.class);

    private final CoverageApiClient coverageApiClient;
    private String sessionId;

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
        LOGGER.info("JUnit 5 launcher session opened; starting Parasoft coverage session");
        sessionId = coverageApiClient.startSession();
    }

    @Override
    public void launcherSessionClosed(LauncherSession session)
    {
        LOGGER.info("JUnit 5 launcher session closed; stopping Parasoft coverage session");
        coverageApiClient.stopSession();
        if (sessionId != null) {
            coverageApiClient.publishResults(sessionId, null, null, null);
        }
    }
}
