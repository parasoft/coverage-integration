/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.junit4;

import java.util.concurrent.atomic.AtomicBoolean;

import com.parasoft.coverage.integration.core.CoverageApiClient;

final class ParasoftJUnit4Lifecycle
{
    private enum SessionOwner
    {
        NONE,
        RUN_LISTENER,
        WATCHER_FALLBACK
    }

    private static final AtomicBoolean SESSION_STOPPED = new AtomicBoolean(false);
    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);
    private static volatile SessionOwner sessionOwner = SessionOwner.NONE;

    private ParasoftJUnit4Lifecycle()
    {
    }

    static synchronized void startSessionFromRunListener(CoverageApiClient coverageApiClient)
    {
        if (sessionOwner == SessionOwner.NONE) {
            coverageApiClient.startSession();
        }
        sessionOwner = SessionOwner.RUN_LISTENER;
    }

    static synchronized void stopSessionFromRunListener(CoverageApiClient coverageApiClient)
    {
        if (sessionOwner == SessionOwner.RUN_LISTENER && SESSION_STOPPED.compareAndSet(false, true)) {
            coverageApiClient.stopSession();
        }
    }

    static synchronized void startSessionFromWatcherFallback(CoverageApiClient coverageApiClient)
    {
        if (sessionOwner != SessionOwner.NONE) {
            return;
        }

        coverageApiClient.startSession();
        sessionOwner = SessionOwner.WATCHER_FALLBACK;

        if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopSessionFromWatcherFallback(coverageApiClient),
                    "parasoft-coverage-session-stop"));
        }
    }

    private static synchronized void stopSessionFromWatcherFallback(CoverageApiClient coverageApiClient)
    {
        if (sessionOwner == SessionOwner.WATCHER_FALLBACK && SESSION_STOPPED.compareAndSet(false, true)) {
            coverageApiClient.stopSession();
        }
    }
}