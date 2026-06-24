/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.junit6;

import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.TestWatcher;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;
import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.ParasoftCoverageApiClient;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public class ParasoftJUnit6Extension implements BeforeEachCallback, TestWatcher
{
    private static final Namespace NAMESPACE = Namespace.create(ParasoftJUnit6Extension.class);

    private final CoverageApiClient coverageApiClient;

    public ParasoftJUnit6Extension()
    {
        this(CoverageApiClientFactory.createFromSettings());
    }

    public ParasoftJUnit6Extension(String ctpBaseUrl, Long environmentId, String userId)
    {
        this(new ParasoftCoverageApiClient(ctpBaseUrl, environmentId, userId, null));
    }

    public ParasoftJUnit6Extension(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient = coverageApiClient;
    }

    @Override
    public void beforeEach(ExtensionContext context)
    {
        String testId = context.getRequiredTestClass().getName();
        String testCaseId = context.getRequiredTestMethod().getName();
        TestExecution execution = new TestExecution(testId, testCaseId);

        context.getStore(NAMESPACE).put(TestExecution.class, execution);

        execution.testContext = coverageApiClient.startTest(testId, testCaseId);
    }

    @Override
    public void testSuccessful(ExtensionContext context)
    {
        stopCurrentTest(context, ResultEnum.PASS, null);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause)
    {
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
        // Test was never started; nothing to stop
    }

    private void stopCurrentTest(ExtensionContext context, ResultEnum result, String failureMessage)
    {
        Store store = context.getStore(NAMESPACE);
        TestExecution execution = store.get(TestExecution.class, TestExecution.class);

        if (execution != null && !execution.stopped) {
            execution.stopped = true;
            coverageApiClient.stopTest(execution.testId, execution.testCaseId, execution.testContext, result, failureMessage);
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
