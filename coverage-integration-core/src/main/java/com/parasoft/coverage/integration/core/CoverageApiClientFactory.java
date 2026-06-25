/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoverageApiClientFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageApiClientFactory.class);

    private CoverageApiClientFactory()
    {
    }

    public static CoverageApiClient createFromSettings()
    {
        LOGGER.debug("Creating coverage API client from configured settings");
        CoverageIntegrationSettings settings = new CoverageIntegrationSettings();
        return new ParasoftCoverageApiClient(settings.getCtpUrl(), settings.getEnvironmentId(), settings.getUserId(), settings.getSessionTag(), settings.isParallelIdEnabled());
    }
}
