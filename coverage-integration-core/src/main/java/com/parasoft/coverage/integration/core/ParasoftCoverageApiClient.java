/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.core;

import java.util.UUID;

import com.parasoft.coverage.integration.core.api.AgentsApi;
import com.parasoft.coverage.integration.core.api.CoverageApi;
import com.parasoft.coverage.integration.core.model.AgentSessionStartModelV3;
import com.parasoft.coverage.integration.core.model.AgentSessionStopModelV3;
import com.parasoft.coverage.integration.core.model.AgentStatusModelV3;
import com.parasoft.coverage.integration.core.model.AgentTestStartModelV3;
import com.parasoft.coverage.integration.core.model.CoverageSessionTestResultsModelV3;
import com.parasoft.coverage.integration.core.model.CoverageSessionTestResultsModelV3.StatusEnum;
import com.parasoft.coverage.integration.core.model.CoverageUploadRequestModelV3;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;
import com.parasoft.coverage.integration.core.model.CoverageUploadRequestModelV3.AnalysisTypeEnum;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParasoftCoverageApiClient
        implements CoverageApiClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftCoverageApiClient.class);
    private static final long POLL_INTERVAL_MS = 2000L;
    private static final int POLL_MAX_ATTEMPTS = 300;
    private static final Gson GSON = new Gson();

    private final AgentsApi agentsApi;
    private final CoverageApi coverageApi;
    private final Long environmentId;
    private final String userId;
    private final String sessionTag;
    private final boolean parallelIdEnabled;

    public ParasoftCoverageApiClient(String ctpBaseUrl, Long environmentId, String userId, String sessionTag)
    {
        this(ctpBaseUrl, environmentId, userId, sessionTag, false);
    }

    public ParasoftCoverageApiClient(String ctpBaseUrl, Long environmentId, String userId, String sessionTag, boolean parallelIdEnabled)
    {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(ctpBaseUrl);

        this.agentsApi = new AgentsApi(apiClient);
        this.coverageApi = new CoverageApi(apiClient);
        this.environmentId = environmentId;
        this.userId = userId;
        this.sessionTag = sessionTag;
        this.parallelIdEnabled = parallelIdEnabled;

        LOGGER.info("Configured Parasoft coverage API client for environment {}", environmentId);
    }

    @Override
    public String startSession()
    {
        LOGGER.info("Starting Parasoft coverage session for environment {}", environmentId);
        try {
            AgentSessionStartModelV3 sessionStart = new AgentSessionStartModelV3();
            sessionStart.setUserId(userId);

            AgentStatusModelV3 status = agentsApi.startSessionPost(environmentId, sessionStart);
            LOGGER.debug("Started Parasoft coverage session for environment {}", environmentId);
            return status != null ? status.getSession() : null;
        }
        catch (ApiException e) {
            logApiFailure("start Parasoft session", e);
            return null;
        }
    }

    @Override
    public CoverageTestContext startTest(String test, String testCase)
    {
        String parallelId = createParallelId();

        LOGGER.debug("Starting Parasoft coverage test: test={}, testCase={}", test, testCase);
        try {
            AgentTestStartModelV3 testStart = new AgentTestStartModelV3();
            testStart.setUserId(userId);
            testStart.setParallelId(parallelId);
            testStart.setTest(test);
            testStart.setTestCase(testCase);

            AgentStatusModelV3 status = agentsApi.startTestPost(environmentId, testStart);

            String baggageHeader = status == null ? null : status.getBaggage();

            LOGGER.debug("Started Parasoft coverage test: test={}, testCase={}", test, testCase);
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
        LOGGER.debug("Stopping Parasoft coverage test: test={}, testCase={}, result={}", test, testCase, result);
        try {
            AgentTestStopModelV3 stop = new AgentTestStopModelV3();
            stop.setUserId(userId);
            stop.setParallelId(testContext == null ? null : testContext.getParallelId());
            stop.setTest(test);
            stop.setTestCase(testCase);
            stop.setResult(result);
            stop.setMessage(message);

            agentsApi.stopTestPost(environmentId, stop);
            LOGGER.debug("Stopped Parasoft coverage test: test={}, testCase={}, result={}", test, testCase, result);
        }
        catch (ApiException e) {
            logApiFailure("stop Parasoft test", e);
        }
    }

    @Override
    public void stopSession()
    {
        LOGGER.info("Stopping Parasoft coverage session for environment {}", environmentId);
        try {
            AgentSessionStopModelV3 stop = new AgentSessionStopModelV3();
            stop.setUserId(userId);

            agentsApi.stopSessionPost(environmentId, stop);
            LOGGER.debug("Stopped Parasoft coverage session for environment {}", environmentId);
        }
        catch (ApiException e) {
            logApiFailure("stop Parasoft session", e);
        }
    }
    private String createParallelId()
    {
        return parallelIdEnabled ? UUID.randomUUID().toString() : null;
    }
    @Override
    public void publishResults(String sessionId, String testConfig, String userId, String toolName)
    {
        try {
            CoverageUploadRequestModelV3 uploadRequest = new CoverageUploadRequestModelV3();
            uploadRequest.setSessionTag(sessionTag);
            uploadRequest.setAnalysisType(AnalysisTypeEnum.UNIT_TEST);

            coverageApi.uploadCoverage(environmentId, sessionId, testConfig, userId, toolName, true, uploadRequest);
            Thread pollThread = new Thread(() -> pollPublishStatus(sessionId), "parasoft-coverage-publish-poll");
            pollThread.setDaemon(true);
            pollThread.start();
        }
        catch (ApiException e) {
            logApiFailure("publish coverage results", e);
        }
    }

    private void pollPublishStatus(String sessionId)
    {
        for (int attempt = 0; attempt < POLL_MAX_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);

                CoverageSessionTestResultsModelV3 result =
                        coverageApi.getCoverageSessionPublishStatus(environmentId, sessionId, userId);

                if (result != null) {
                    System.out.println(GSON.toJson(result));

                    StatusEnum status = result.getStatus();
                    if (status == StatusEnum.PUBLISHED || status == StatusEnum.ERROR) {
                        return;
                    }
                }
            }
            catch (ApiException e) {
                logApiFailure("poll coverage publish status", e);
                return;
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void logApiFailure(String action, ApiException e)
    {
        LOGGER.error("Failed to {}: {}", action, e.getMessage(), e);
    }
}
