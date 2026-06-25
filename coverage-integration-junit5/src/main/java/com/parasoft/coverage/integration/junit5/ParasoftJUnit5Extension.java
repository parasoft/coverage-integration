/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.junit5;

import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;
import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.ParasoftCoverageApiClient;
import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public class ParasoftJUnit5Extension implements BeforeEachCallback, TestWatcher
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftJUnit5Extension.class);

    private static final Namespace NAMESPACE = Namespace.create(ParasoftJUnit5Extension.class);

    private final CoverageApiClient coverageApiClient;

    public ParasoftJUnit5Extension()
    {
        this(CoverageApiClientFactory.createFromSettings());
    }

    public ParasoftJUnit5Extension(String ctpBaseUrl, Long environmentId, String userId)
    {
        this(new ParasoftCoverageApiClient(ctpBaseUrl, environmentId, userId, null));
    }

    public ParasoftJUnit5Extension(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient = coverageApiClient;
    }

    @Override
    public void beforeEach(ExtensionContext context)
    {
        String testId = context.getRequiredTestClass().getName();
        String testCaseId = context.getRequiredTestMethod().getName();
        String executionKey = context.getUniqueId();
        TestExecution execution = new TestExecution(testId, testCaseId);

        Store store = getExecutionStore(context);
        store.put(executionKey, execution);

        LOGGER.debug("JUnit 5 test starting: test={}, testCase={}", testId, testCaseId);

        try {
            execution.testContext = coverageApiClient.startTest(testId, testCaseId);
            execution.contextThreadId = CoverageExecutionContext.setCurrent(execution.testContext);
        }
        catch (RuntimeException e) {
            LOGGER.error("Failed to initialize Parasoft coverage context for JUnit 5 test: test={}, testCase={}",
                    testId, testCaseId, e);
            store.remove(executionKey);
            CoverageExecutionContext.clearCurrent();
            throw e;
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context)
    {
        stopCurrentTest(context, ResultEnum.PASS, null);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause)
    {
        LOGGER.info("JUnit 5 test failed: {}", context.getDisplayName());
        stopCurrentTest(context, ResultEnum.FAIL, buildFailureMessage(cause));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause)
    {
        stopCurrentTest(context, ResultEnum.INCOMPLETE, null);
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason)
    {
        LOGGER.debug("JUnit 5 test disabled: displayName={}, reason={}", context.getDisplayName(), reason.orElse(null));
        // Test was never started; nothing to stop
    }

    private void stopCurrentTest(ExtensionContext context, ResultEnum result, String failureMessage)
    {
        Store store = getExecutionStore(context);
        TestExecution execution = store.remove(context.getUniqueId(), TestExecution.class);

        try {
            if (execution != null && !execution.stopped) {
                execution.stopped = true;
                LOGGER.debug("JUnit 5 test stopping: test={}, testCase={}, result={}",
                        execution.testId, execution.testCaseId, result);
                coverageApiClient.stopTest(
                        execution.testId,
                        execution.testCaseId,
                        execution.testContext,
                        result,
                        failureMessage);
            }
            else {
                LOGGER.debug("No active JUnit 5 test found to stop for displayName={}, result={}",
                        context.getDisplayName(), result);
            }
        }
        finally {
            if (execution == null) {
                CoverageExecutionContext.clearCurrent();
            }
            else {
                CoverageExecutionContext.clear(execution.contextThreadId);
            }
        }
    }

    private static Store getExecutionStore(ExtensionContext context)
    {
        ExtensionContext storeContext = context.getParent().orElse(context);
        return storeContext.getStore(NAMESPACE);
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
        private long contextThreadId = -1;
        private boolean stopped;

        private TestExecution(String testId, String testCaseId)
        {
            this.testId = testId;
            this.testCaseId = testCaseId;
        }
    }
}
