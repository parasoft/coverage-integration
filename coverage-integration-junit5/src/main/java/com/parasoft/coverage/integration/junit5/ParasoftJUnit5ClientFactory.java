/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.junit5;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.ParasoftCoverageApiClient;

final class ParasoftJUnit5ClientFactory
{
    private ParasoftJUnit5ClientFactory()
    {
    }

    static CoverageApiClient createFromSystemProperties()
    {
        String ctpBaseUrl = requireSystemProperty("parasoft.ctp.url");
        String environmentId = requireSystemProperty("parasoft.ctp.environmentId");
        String userId = System.getProperty("parasoft.ctp.userId");

        return new ParasoftCoverageApiClient(
                ctpBaseUrl,
                Long.parseLong(environmentId),
                userId);
    }

    private static String requireSystemProperty(String name)
    {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: " + name);
        }
        return value;
    }
}