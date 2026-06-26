/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.api;

import com.parasoft.coverage.integration.core.CoverageApiClientFactory;
import com.parasoft.coverage.integration.core.ParasoftCoverageApiClient;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

/**
 * User-facing client for directly reporting coverage session and test lifecycle
 * events.
 * <p>
 * Most users should prefer the JUnit integration modules. Use this client only
 * when a test is launched manually, such as from a {@code main} method or a
 * custom test harness.
 * </p>
 * <p>
 * Typical standalone usage:
 * </p>
 *
 * <pre>{@code
 * CoverageApiClient client = CoverageApiClient.createFromSettings();
 * String sessionId = client.startSession();
 * CoverageTestContext context = client.startTest("ExampleTest", "runsFromMain");
 *
 * try {
 *     // Execute the code under test here. Send context.getCurrentTestOperatorIdHeader()
 *     // as the Baggage header when calling an application under test.
 *     client.stopTest("ExampleTest", "runsFromMain", context, CoverageTestResult.PASS, null);
 * } catch (RuntimeException e) {
 *     client.stopTest("ExampleTest", "runsFromMain", context, CoverageTestResult.FAIL, e.getMessage());
 *     throw e;
 * } finally {
 *     client.stopSession();
 *     client.publishResults(sessionId, null, null, null);
 * }
 * }</pre>
 */
public final class CoverageApiClient
{
    private final com.parasoft.coverage.integration.core.CoverageApiClient delegate;

    private CoverageApiClient(com.parasoft.coverage.integration.core.CoverageApiClient delegate)
    {
        this.delegate = delegate;
    }

    /**
     * Creates a client from {@code coverage-integration.properties} and system
     * properties.
     *
     * @return a coverage API client
     */
    public static CoverageApiClient createFromSettings()
    {
        return new CoverageApiClient(CoverageApiClientFactory.createFromSettings());
    }

    /**
     * Creates a client for directly reporting coverage events.
     *
     * @param ctpBaseUrl CTP base URL
     * @param environmentId CTP environment identifier
     * @param userId user identifier to send to CTP
     * @param sessionTag optional session tag
     * @return a coverage API client
     */
    public static CoverageApiClient create(String ctpBaseUrl, Long environmentId, String userId, String sessionTag)
    {
        return new CoverageApiClient(new ParasoftCoverageApiClient(ctpBaseUrl, environmentId, userId, sessionTag));
    }

    /**
     * Starts a coverage session.
     *
     * @return the coverage session identifier returned by CTP, or {@code null}
     *         when no session identifier is available
     */
    public String startSession()
    {
        return delegate.startSession();
    }

    /**
     * Starts coverage tracking for a test.
     *
     * @param test test identifier, usually the test class name
     * @param testCase test case identifier, usually the test method name
     * @return context returned from the CTP {@code /test/start} API
     */
    public CoverageTestContext startTest(String test, String testCase)
    {
        return new CoverageTestContext(delegate.startTest(test, testCase));
    }

    /**
     * Stops coverage tracking for a test.
     *
     * @param test test identifier, usually the test class name
     * @param testCase test case identifier, usually the test method name
     * @param testContext context returned by {@link #startTest(String, String)}
     * @param result final test result
     * @param message optional result message, such as a failure summary
     */
    public void stopTest(String test, String testCase, CoverageTestContext testContext, CoverageTestResult result, String message)
    {
        delegate.stopTest(test, testCase, testContext == null ? null : testContext.delegate(), toCoreResult(result), message);
    }

    /**
     * Stops the current coverage session.
     */
    public void stopSession()
    {
        delegate.stopSession();
    }

    /**
     * Publishes coverage results for a finished session.
     *
     * @param sessionId coverage session identifier returned by
     *        {@link #startSession()}
     * @param testConfig optional test configuration name
     * @param userId optional user identifier
     * @param toolName optional tool name
     */
    public void publishResults(String sessionId, String testConfig, String userId, String toolName)
    {
        delegate.publishResults(sessionId, testConfig, userId, toolName);
    }

    private static ResultEnum toCoreResult(CoverageTestResult result)
    {
        if (result == null) {
            return null;
        }
        switch (result) {
            case PASS:
                return ResultEnum.PASS;
            case FAIL:
                return ResultEnum.FAIL;
            case INCOMPLETE:
                return ResultEnum.INCOMPLETE;
            default:
                throw new IllegalArgumentException("Unsupported coverage test result: " + result);
        }
    }
}
