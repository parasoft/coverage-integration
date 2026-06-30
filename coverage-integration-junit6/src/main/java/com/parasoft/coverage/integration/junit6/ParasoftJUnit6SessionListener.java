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
