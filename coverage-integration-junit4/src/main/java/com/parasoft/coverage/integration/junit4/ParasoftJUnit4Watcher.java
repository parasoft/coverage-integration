/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.junit4;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.ParasoftCoverageApiClient;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public class ParasoftJUnit4Watcher extends TestWatcher {
    private final CoverageApiClient coverageApiClient;

    private final ThreadLocal<TestExecution> currentTest = new ThreadLocal<>();

    public ParasoftJUnit4Watcher() {
        this(ParasoftJUnit4ClientFactory.createFromSystemProperties());
    }

    public ParasoftJUnit4Watcher(String ctpBaseUrl, Long environmentId, String userId) {
        this(new ParasoftCoverageApiClient(ctpBaseUrl, environmentId, userId));
    }

    public ParasoftJUnit4Watcher(CoverageApiClient coverageApiClient) {
        this.coverageApiClient = coverageApiClient;
    }

    @Override
    protected void starting(Description description) {
        ParasoftJUnit4Lifecycle.startSessionFromWatcherFallback(coverageApiClient);
        TestExecution execution = new TestExecution(description.getClassName(), description.getMethodName());

        currentTest.set(execution);

        coverageApiClient.startTest(execution.testId, execution.testCaseId);
    }

    @Override
    protected void succeeded(Description description) {
        stopCurrentTest(ResultEnum.PASS, null);
    }

    @Override
    protected void failed(Throwable e, Description description) {
        stopCurrentTest(ResultEnum.FAIL, buildFailureMessage(e));
    }

    @Override
    protected void finished(Description description) {
        stopCurrentTest(ResultEnum.INCOMPLETE, null);
    }

    private void stopCurrentTest(ResultEnum result, String failureMessage) {
        TestExecution execution = currentTest.get();

        try {
            if (execution != null && !execution.stopped) {
                execution.stopped = true;
                coverageApiClient.stopTest(execution.testId, execution.testCaseId, result, failureMessage);
            }
        } finally {
            currentTest.remove();
        }
    }

    private static String buildFailureMessage(Throwable cause) {
        if (cause == null) {
            return null;
        }

        String message = cause.getMessage();
        if (message != null) {
            int newlineIndex = message.indexOf('\n');
            if (newlineIndex >= 0) {
                message = message.substring(0, newlineIndex);
            }
            message = message.replace('\r', ' ');
        }

        StringBuilder summary = new StringBuilder();
        summary.append(cause.getClass().getSimpleName());

        if (message != null && !message.isBlank()) {
            summary.append(": ").append(message);
        }

        String sanitized = summary.toString().replace('"', '\'').replace("\n", " ").replace("\t", " ");

        int maxLength = 500;
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private static final class TestExecution {
        private final String testId;
        private final String testCaseId;
        private boolean stopped;

        private TestExecution(String testId, String testCaseId) {
            this.testId = testId;
            this.testCaseId = testCaseId;
        }
    }
}