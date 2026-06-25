/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.junit6;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;

public class ParasoftJUnit6SessionListener implements LauncherSessionListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftJUnit6SessionListener.class);

    private final CoverageApiClient coverageApiClient;
    private String sessionId;

    public ParasoftJUnit6SessionListener()
    {
        this(CoverageApiClientFactory.createFromSettings());
    }

    public ParasoftJUnit6SessionListener(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient = coverageApiClient;
    }

    @Override
    public void launcherSessionOpened(LauncherSession session)
    {
        LOGGER.info("JUnit 6 launcher session opened; starting Parasoft coverage session");
        sessionId = coverageApiClient.startSession();
    }

    @Override
    public void launcherSessionClosed(LauncherSession session)
    {
        LOGGER.info("JUnit 6 launcher session closed; stopping Parasoft coverage session");
        coverageApiClient.stopSession();
        if (sessionId != null) {
            coverageApiClient.publishResults(sessionId, null, null, null);
        }
    }
}
