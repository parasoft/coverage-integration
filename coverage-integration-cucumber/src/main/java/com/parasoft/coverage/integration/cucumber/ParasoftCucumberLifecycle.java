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

package com.parasoft.coverage.integration.cucumber;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parasoft.coverage.integration.core.CoverageApiClient;

final class ParasoftCucumberLifecycle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftCucumberLifecycle.class);

    private final CoverageApiClient coverageApiClient;

    private String sessionId;
    private boolean sessionStarted;

    ParasoftCucumberLifecycle(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient = Objects.requireNonNull(
                coverageApiClient,
                "coverageApiClient must not be null");
    }

    synchronized void startSession()
    {
        if (sessionStarted) {
            LOGGER.debug("Parasoft Cucumber coverage session is already started");
            return;
        }

        LOGGER.info("Starting Parasoft coverage session for Cucumber test run");

        sessionId = coverageApiClient.startSession();
        sessionStarted = true;
    }

    synchronized void stopSession()
    {
        if (!sessionStarted) {
            LOGGER.debug("No active Parasoft Cucumber coverage session to stop");
            return;
        }

        String completedSessionId = sessionId;

        try {
            LOGGER.info("Stopping Parasoft coverage session for Cucumber test run");
            coverageApiClient.stopSession();

            if (completedSessionId != null) {
                LOGGER.info("Publishing Parasoft coverage results for Cucumber session {}",
                        completedSessionId);
                coverageApiClient.publishResults(
                        completedSessionId,
                        null,
                        null,
                        null);
            }
        }
        finally {
            sessionId = null;
            sessionStarted = false;
        }
    }

    CoverageApiClient getCoverageApiClient()
    {
        return coverageApiClient;
    }
}
