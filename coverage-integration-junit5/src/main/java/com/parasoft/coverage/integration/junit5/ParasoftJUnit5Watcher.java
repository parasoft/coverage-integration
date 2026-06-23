/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.junit5;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.ParasoftCoverageApiClient;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public class ParasoftJUnit5Watcher
        implements TestExecutionListener
{
    private static final String ENGINE_SEGMENT_TYPE = "engine";

    private static final String JUNIT_JUPITER_ENGINE_ID = "junit-jupiter";

    private volatile CoverageApiClient coverageApiClient;

    private final Object sessionLock = new Object();

    private final AtomicBoolean jupiterExecutionAvailable =
            new AtomicBoolean(false);

    private final AtomicBoolean sessionActive = new AtomicBoolean(false);

    private final Map<String, TestExecution> activeTests =
            new ConcurrentHashMap<>();

    public ParasoftJUnit5Watcher()
    {
    }

    public ParasoftJUnit5Watcher(
            String ctpBaseUrl,
            Long environmentId,
            String userId)
    {
        this(new ParasoftCoverageApiClient(
                ctpBaseUrl,
                environmentId,
                userId));
    }

    public ParasoftJUnit5Watcher(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient =
                Objects.requireNonNull(coverageApiClient);
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan)
    {
        jupiterExecutionAvailable.set(
                containsJupiterExecution(testPlan));
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier)
    {
        if (!jupiterExecutionAvailable.get()
                || !isJupiterTest(testIdentifier)) {
            return;
        }

        ensureSessionStarted();

        TestExecution execution =
                createTestExecution(testIdentifier);
        String uniqueId = testIdentifier.getUniqueId();

        if (activeTests.putIfAbsent(uniqueId, execution) != null) {
            return;
        }

        try {
            getCoverageApiClient().startTest(
                    execution.testId,
                    execution.testCaseId);
        }
        catch (RuntimeException e) {
            activeTests.remove(uniqueId, execution);
            throw e;
        }
    }

    @Override
    public void executionFinished(
            TestIdentifier testIdentifier,
            TestExecutionResult testExecutionResult)
    {
        TestExecution execution =
                activeTests.remove(testIdentifier.getUniqueId());

        if (execution == null) {
            return;
        }

        switch (testExecutionResult.getStatus()) {
        case SUCCESSFUL:
            stopTest(execution, ResultEnum.PASS, null);
            break;
        case FAILED:
            stopTest(
                    execution,
                    ResultEnum.FAIL,
                    buildFailureMessage(
                            testExecutionResult
                                    .getThrowable()
                                    .orElse(null)));
            break;
        case ABORTED:
            stopTest(
                    execution,
                    ResultEnum.INCOMPLETE,
                    null);
            break;
        default:
            stopTest(
                    execution,
                    ResultEnum.INCOMPLETE,
                    null);
            break;
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan)
    {
        try {
            stopRemainingTestsAsIncomplete();
        }
        finally {
            try {
                stopSessionIfActive();
            }
            finally {
                jupiterExecutionAvailable.set(false);
            }
        }
    }

    private void ensureSessionStarted()
    {
        if (sessionActive.get()) {
            return;
        }

        synchronized (sessionLock) {
            if (sessionActive.get()) {
                return;
            }

            getCoverageApiClient().startSession();
            sessionActive.set(true);
        }
    }

    private void stopSessionIfActive()
    {
        synchronized (sessionLock) {
            if (!sessionActive.compareAndSet(true, false)) {
                return;
            }

            getCoverageApiClient().stopSession();
        }
    }

    private void stopRemainingTestsAsIncomplete()
    {
        for (Map.Entry<String, TestExecution> entry
                : activeTests.entrySet()) {
            if (activeTests.remove(
                    entry.getKey(),
                    entry.getValue())) {
                stopTest(
                        entry.getValue(),
                        ResultEnum.INCOMPLETE,
                        null);
            }
        }
    }

    private void stopTest(
            TestExecution execution,
            ResultEnum result,
            String message)
    {
        getCoverageApiClient().stopTest(
                execution.testId,
                execution.testCaseId,
                result,
                message);
    }

    private CoverageApiClient getCoverageApiClient()
    {
        CoverageApiClient client = coverageApiClient;
        if (client != null) {
            return client;
        }

        synchronized (this) {
            client = coverageApiClient;

            if (client == null) {
                client =
                        ParasoftJUnit5ClientFactory
                                .createFromSystemProperties();
                coverageApiClient = client;
            }
        }

        return client;
    }

    private static boolean containsJupiterExecution(
            TestPlan testPlan)
    {
        return testPlan.getRoots().stream()
                .flatMap(root ->
                        testPlan.getDescendants(root).stream())
                .anyMatch(
                        ParasoftJUnit5Watcher
                                ::belongsToJupiterEngine);
    }

    private static boolean isJupiterTest(
            TestIdentifier testIdentifier)
    {
        return testIdentifier.isTest()
                && belongsToJupiterEngine(testIdentifier);
    }

    private static boolean belongsToJupiterEngine(
            TestIdentifier testIdentifier)
    {
        return testIdentifier
                .getUniqueIdObject()
                .getSegments()
                .stream()
                .anyMatch(segment ->
                        ENGINE_SEGMENT_TYPE.equals(
                                segment.getType())
                                && JUNIT_JUPITER_ENGINE_ID.equals(
                                        segment.getValue()));
    }

    private static TestExecution createTestExecution(
            TestIdentifier testIdentifier)
    {
        TestSource source =
                testIdentifier.getSource().orElse(null);

        if (source instanceof MethodSource) {
            MethodSource methodSource =
                    (MethodSource) source;

            return new TestExecution(
                    methodSource.getClassName(),
                    methodSource.getMethodName());
        }

        return new TestExecution(
                testIdentifier.getUniqueId(),
                testIdentifier.getDisplayName());
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
                message = message.substring(
                        0,
                        newlineIndex);
            }

            message = message.replace('\r', ' ');
        }

        StringBuilder summary = new StringBuilder();
        summary.append(cause.getClass().getSimpleName());

        if (message != null && !message.isBlank()) {
            summary.append(": ").append(message);
        }

        String sanitized = summary.toString()
                .replace('"', '\'')
                .replace("\n", " ")
                .replace("\t", " ");

        int maxLength = 500;

        return sanitized.length() <= maxLength
                ? sanitized
                : sanitized.substring(0, maxLength);
    }

    private static final class TestExecution
    {
        private final String testId;

        private final String testCaseId;

        private TestExecution(
                String testId,
                String testCaseId)
        {
            this.testId = testId;
            this.testCaseId = testCaseId;
        }
    }
}