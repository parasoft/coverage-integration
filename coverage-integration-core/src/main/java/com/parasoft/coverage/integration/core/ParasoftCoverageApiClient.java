/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.core;

import java.util.UUID;

import com.parasoft.coverage.integration.core.api.AgentsApi;
import com.parasoft.coverage.integration.core.model.AgentSessionStartModelV3;
import com.parasoft.coverage.integration.core.model.AgentSessionStopModelV3;
import com.parasoft.coverage.integration.core.model.AgentStatusModelV3;
import com.parasoft.coverage.integration.core.model.AgentTestStartModelV3;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public class ParasoftCoverageApiClient
        implements CoverageApiClient
{
    private final AgentsApi agentsApi;
    private final Long environmentId;
    private final String userId;
    private final boolean parallelIdEnabled;

    public ParasoftCoverageApiClient(String ctpBaseUrl, Long environmentId, String userId)
    {
        this(ctpBaseUrl, environmentId, userId, false);
    }

    public ParasoftCoverageApiClient(String ctpBaseUrl, Long environmentId, String userId, boolean parallelIdEnabled)
    {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(ctpBaseUrl);

        this.agentsApi = new AgentsApi(apiClient);
        this.environmentId = environmentId;
        this.userId = userId;
        this.parallelIdEnabled = parallelIdEnabled;
    }

    @Override
    public void startSession()
    {
        try {
            AgentSessionStartModelV3 sessionStart = new AgentSessionStartModelV3();
            sessionStart.setUserId(userId);

            agentsApi.startSessionPost(environmentId, sessionStart);
        }
        catch (ApiException e) {
            logApiFailure("start Parasoft session", e);
        }
    }

    @Override
    public CoverageTestContext startTest(String test, String testCase)
    {
        String parallelId = createParallelId();

        try {
            AgentTestStartModelV3 testStart = new AgentTestStartModelV3();
            testStart.setUserId(userId);
            testStart.setParallelId(parallelId);
            testStart.setTest(test);
            testStart.setTestCase(testCase);

            AgentStatusModelV3 status = agentsApi.startTestPost(environmentId, testStart);

            String baggageHeader = status == null ? null : status.getBaggage();

            return new CoverageTestContext(parallelId, baggageHeader);
        }
        catch (ApiException e) {
            logApiFailure("start Parasoft test", e);

            return new CoverageTestContext(parallelId, null);
        }
    }

    @Override
    public void stopTest(String test, String testCase, CoverageTestContext testContext, ResultEnum result, String message)
    {
        try {
            AgentTestStopModelV3 stop = new AgentTestStopModelV3();
            stop.setUserId(userId);
            stop.setParallelId(testContext == null ? null : testContext.getParallelId());
            stop.setTest(test);
            stop.setTestCase(testCase);
            stop.setResult(result);
            stop.setMessage(message);

            agentsApi.stopTestPost(environmentId, stop);
        }
        catch (ApiException e) {
            logApiFailure("stop Parasoft test", e);
        }
    }

    @Override
    public void stopSession()
    {
        try {
            AgentSessionStopModelV3 stop = new AgentSessionStopModelV3();
            stop.setUserId(userId);

            agentsApi.stopSessionPost(environmentId, stop);
        }
        catch (ApiException e) {
            logApiFailure("stop Parasoft session", e);
        }
    }

    private String createParallelId()
    {
        return parallelIdEnabled ? UUID.randomUUID().toString() : null;
    }

    private static void logApiFailure(String action, ApiException e)
    {
        System.err.println("Failed to " + action + ": " + e.getMessage());
    }
}
