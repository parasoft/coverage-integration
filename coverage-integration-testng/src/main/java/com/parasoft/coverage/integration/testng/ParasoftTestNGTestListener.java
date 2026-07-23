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

package com.parasoft.coverage.integration.testng;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;
import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public class ParasoftTestNGTestListener implements ITestListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftTestNGTestListener.class);

    private final CoverageApiClient coverageApiClient;
    private final ThreadLocal<TestExecution> currentExecution = new ThreadLocal<>();

    public ParasoftTestNGTestListener()
    {
        this(CoverageApiClientFactory.createFromSettings());
    }

    public ParasoftTestNGTestListener(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient = coverageApiClient;
    }

    @Override
    public void onTestStart(ITestResult result)
    {
        String testId = getTestId(result);
        String testCaseId = result.getName();
        TestExecution execution = new TestExecution(testId, testCaseId);
        currentExecution.set(execution);

        LOGGER.debug("TestNG test starting: test={}, testCase={}", testId, testCaseId);

        try {
            execution.testContext = coverageApiClient.startTest(testId, testCaseId);
            execution.contextThreadId = CoverageExecutionContext.setCurrent(execution.testContext);
        }
        catch (RuntimeException e) {
            LOGGER.error("Failed to initialize Parasoft coverage context for TestNG test: test={}, testCase={}",
                    testId, testCaseId, e);
            currentExecution.remove();
            CoverageExecutionContext.clearCurrent();
            throw e;
        }
    }

    @Override
    public void onTestSuccess(ITestResult result)
    {
        stopCurrentTest(ResultEnum.PASS, null);
    }

    @Override
    public void onTestFailure(ITestResult result)
    {
        LOGGER.debug("TestNG test failed: {}", result.getName());
        stopCurrentTest(ResultEnum.FAIL, buildFailureMessage(result.getThrowable()));
    }

    @Override
    public void onTestSkipped(ITestResult result)
    {
        stopCurrentTest(ResultEnum.INCOMPLETE, null);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result)
    {
        stopCurrentTest(ResultEnum.PASS, null);
    }

    @Override
    public void onTestFailedWithTimeout(ITestResult result)
    {
        LOGGER.debug("TestNG test failed with timeout: {}", result.getName());
        stopCurrentTest(ResultEnum.FAIL, buildFailureMessage(result.getThrowable()));
    }

    @Override
    public void onStart(ITestContext context)
    {
        // Session lifecycle is managed by ParasoftTestNGSuiteListener
    }

    @Override
    public void onFinish(ITestContext context)
    {
        // Session lifecycle is managed by ParasoftTestNGSuiteListener
    }

    private void stopCurrentTest(ResultEnum result, String failureMessage)
    {
        TestExecution execution = currentExecution.get();
        currentExecution.remove();

        try {
            if (execution != null && !execution.stopped) {
                execution.stopped = true;
                LOGGER.debug("TestNG test stopping: test={}, testCase={}, result={}",
                        execution.testId, execution.testCaseId, result);
                coverageApiClient.stopTest(
                        execution.testId,
                        execution.testCaseId,
                        execution.testContext,
                        result,
                        failureMessage);
            }
            else {
                LOGGER.debug("No active TestNG test found to stop, result={}", result);
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

    private static String getTestId(ITestResult result)
    {
        return result.getTestClass().getRealClass().getName() + '#' + result.getName();
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
