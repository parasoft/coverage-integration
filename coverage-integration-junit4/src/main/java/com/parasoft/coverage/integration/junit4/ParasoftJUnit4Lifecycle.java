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

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parasoft.coverage.integration.core.CoverageApiClient;

final class ParasoftJUnit4Lifecycle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftJUnit4Lifecycle.class);

    private enum SessionOwner
    {
        NONE,
        RUN_LISTENER,
        WATCHER_FALLBACK
    }

    private static final AtomicBoolean SESSION_STOPPED = new AtomicBoolean(false);
    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);
    private static volatile SessionOwner sessionOwner = SessionOwner.NONE;
    private static volatile String sessionId = null;

    private ParasoftJUnit4Lifecycle()
    {
    }

    static synchronized void startSessionFromRunListener(CoverageApiClient coverageApiClient)
    {
        if (sessionOwner == SessionOwner.NONE) {
            LOGGER.info("Starting Parasoft coverage session from JUnit 4 run listener");
            sessionId = coverageApiClient.startSession();
        } else {
            LOGGER.debug("JUnit 4 coverage session is already owned by {}", sessionOwner);
        }
        sessionOwner = SessionOwner.RUN_LISTENER;
    }

    static synchronized void stopSessionFromRunListener(CoverageApiClient coverageApiClient)
    {
        if (sessionOwner == SessionOwner.RUN_LISTENER && SESSION_STOPPED.compareAndSet(false, true)) {
            LOGGER.info("Stopping Parasoft coverage session from JUnit 4 run listener");
            coverageApiClient.stopSession();
            if (sessionId != null) {
                coverageApiClient.publishResults(sessionId, null, null, null);
            }
        } else {
            LOGGER.debug("Skipping JUnit 4 run listener session stop because owner={} stopped={}",
                    sessionOwner, SESSION_STOPPED.get());
        }
    }

    static synchronized void startSessionFromWatcherFallback(CoverageApiClient coverageApiClient)
    {
        if (sessionOwner != SessionOwner.NONE) {
            LOGGER.debug("Skipping JUnit 4 watcher fallback session start because owner={}", sessionOwner);
            return;
        }

        LOGGER.warn("Starting Parasoft coverage session from JUnit 4 watcher fallback; register ParasoftJUnit4RunListener for full run lifecycle coverage");
        sessionId = coverageApiClient.startSession();
        sessionOwner = SessionOwner.WATCHER_FALLBACK;

        if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            LOGGER.debug("Registering JUnit 4 watcher fallback shutdown hook");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopSessionFromWatcherFallback(coverageApiClient),
                    "parasoft-coverage-session-stop"));
        }
    }

    private static synchronized void stopSessionFromWatcherFallback(CoverageApiClient coverageApiClient)
    {
        if (sessionOwner == SessionOwner.WATCHER_FALLBACK && SESSION_STOPPED.compareAndSet(false, true)) {
            LOGGER.info("Stopping Parasoft coverage session from JUnit 4 watcher fallback");
            coverageApiClient.stopSession();
            if (sessionId != null) {
                coverageApiClient.publishResults(sessionId, null, null, null);
            }
        }
    }
}
