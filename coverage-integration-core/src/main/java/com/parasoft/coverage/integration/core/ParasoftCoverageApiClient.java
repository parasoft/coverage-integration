/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.core;

import com.parasoft.coverage.integration.core.api.AgentsApi;
import com.parasoft.coverage.integration.core.model.AgentSessionStartModelV3;
import com.parasoft.coverage.integration.core.model.AgentSessionStopModelV3;
import com.parasoft.coverage.integration.core.model.AgentTestStartModelV3;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public class ParasoftCoverageApiClient
        implements CoverageApiClient
{
    private final AgentsApi agentsApi;
    private final Long environmentId;
    private final String userId;

    public ParasoftCoverageApiClient(String ctpBaseUrl, Long environmentId, String userId)
    {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(ctpBaseUrl);

        this.agentsApi = new AgentsApi(apiClient);
        this.environmentId = environmentId;
        this.userId = userId;
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
    public void startTest(String test, String testCase)
    {
        try {
            AgentTestStartModelV3 testStart = new AgentTestStartModelV3();
            testStart.setUserId(userId);
            testStart.setTest(test);
            testStart.setTestCase(testCase);

            agentsApi.startTestPost(environmentId, testStart);
        }
        catch (ApiException e) {
            logApiFailure("start Parasoft test", e);
        }
    }

    @Override
    public void stopTest(String test, String testCase, ResultEnum result, String message)
    {
        try {
            AgentTestStopModelV3 stop = new AgentTestStopModelV3();
            stop.setUserId(userId);
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

    private static void logApiFailure(String action, ApiException e)
    {
        System.err.println("Failed to " + action + ": " + e.getMessage());
    }
}