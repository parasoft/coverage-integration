/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.junit4;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;
import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.ParasoftCoverageApiClient;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public class ParasoftJUnit4Watcher extends TestWatcher
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftJUnit4Watcher.class);

    private final CoverageApiClient coverageApiClient;

    private final ThreadLocal<TestExecution> currentTest = new ThreadLocal<>();

    public ParasoftJUnit4Watcher()
    {
        this(CoverageApiClientFactory.createFromSettings());
    }

    public ParasoftJUnit4Watcher(String ctpBaseUrl, Long environmentId, String userId) {
        this(new ParasoftCoverageApiClient(ctpBaseUrl, environmentId, userId, null));
    }

    public ParasoftJUnit4Watcher(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient = coverageApiClient;
    }

    @Override
    protected void starting(Description description)
    {
        ParasoftJUnit4Lifecycle.startSessionFromWatcherFallback(coverageApiClient);
        TestExecution execution = new TestExecution(description.getClassName(), description.getMethodName());

        currentTest.set(execution);

        LOGGER.debug("JUnit 4 test starting: test={}, testCase={}", execution.testId, execution.testCaseId);
        execution.testContext = coverageApiClient.startTest(execution.testId, execution.testCaseId);
    }

    @Override
    protected void succeeded(Description description)
    {
        stopCurrentTest(ResultEnum.PASS, null);
    }

    @Override
    protected void failed(Throwable e, Description description)
    {
        LOGGER.info("JUnit 4 test failed: class={}, method={}", description.getClassName(), description.getMethodName());
        stopCurrentTest(ResultEnum.FAIL, buildFailureMessage(e));
    }

    @Override
    protected void finished(Description description)
    {
        stopCurrentTest(ResultEnum.INCOMPLETE, null);
    }

    private void stopCurrentTest(ResultEnum result, String failureMessage)
    {
        TestExecution execution = currentTest.get();

        try {
            if (execution != null && !execution.stopped) {
                execution.stopped = true;
                LOGGER.debug("JUnit 4 test stopping: test={}, testCase={}, result={}",
                        execution.testId, execution.testCaseId, result);
                coverageApiClient.stopTest(execution.testId, execution.testCaseId, execution.testContext, result, failureMessage);
            }
            else {
                LOGGER.debug("No active JUnit 4 test found to stop for result={}", result);
            }
        } finally {
            currentTest.remove();
        }
    }

    private static String buildFailureMessage(Throwable cause)
    {
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

    private static final class TestExecution
    {
        private final String testId;
        private final String testCaseId;
        private CoverageTestContext testContext;
        private boolean stopped;

        private TestExecution(String testId, String testCaseId)
        {
            this.testId = testId;
            this.testCaseId = testCaseId;
        }
    }
}
