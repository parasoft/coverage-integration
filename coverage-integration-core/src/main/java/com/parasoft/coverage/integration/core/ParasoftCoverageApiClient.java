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

package com.parasoft.coverage.integration.core;

import java.util.Objects;
import java.util.UUID;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParasoftCoverageApiClient
        implements CoverageApiClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftCoverageApiClient.class);
    private static final long POLL_INTERVAL_MS = 2000L;
    private static final int POLL_MAX_ATTEMPTS = 300;
    private static final String TEST_OPERATOR_ID_BAGGAGE_PREFIX = "test-operator-id=";

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
        this(ctpBaseUrl, environmentId, userId, sessionTag, parallelIdEnabled, null, null, null);
    }

    public ParasoftCoverageApiClient(String ctpBaseUrl, Long environmentId, String userId, String sessionTag, boolean parallelIdEnabled, String username, String password, String token)
    {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        if (token != null && !token.isBlank()) {
            LOGGER.debug("Configuring Parasoft coverage API client with bearer token authentication");
            final String bearerToken = token;
            httpClientBuilder.addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                        .header("Authorization", "Bearer " + bearerToken)
                        .build();
                return chain.proceed(request);
            });
        } else if (username != null && !username.isBlank()) {
            boolean passwordConfigured = password != null && !password.isEmpty();
            LOGGER.debug(
                    "Configuring Parasoft coverage API client with basic authentication: user='{}', passwordConfigured={}",
                    username,
                    passwordConfigured);
            final String credentials = Credentials.basic(username, password != null ? password : "");
            httpClientBuilder.addInterceptor(chain -> {
                LOGGER.debug(
                        "Sending {} {} with Basic authentication for user '{}' (Authorization value redacted)",
                        chain.request().method(),
                        chain.request().url(),
                        username);
                Request request = chain.request().newBuilder()
                        .header("Authorization", credentials)
                        .build();
                return chain.proceed(request);
            });
        } else {
            LOGGER.warn("No credentials configured for Parasoft coverage API client - requests will be unauthenticated");
        }

        ApiClient apiClient = new ApiClient(httpClientBuilder.build());
        apiClient.setBasePath(toApiBaseUrl(ctpBaseUrl));

        this.agentsApi = new AgentsApi(apiClient);
        this.coverageApi = new CoverageApi(apiClient);
        this.environmentId = environmentId;
        this.userId = userId;
        this.sessionTag = sessionTag;
        this.parallelIdEnabled = parallelIdEnabled;

        LOGGER.debug("Configured Parasoft coverage API client for environment {}", environmentId);
    }

    private static String toApiBaseUrl(String ctpBaseUrl)
    {
        String normalized = Objects.requireNonNull(ctpBaseUrl, "ctpBaseUrl must not be null")
                .replaceFirst("/+$", "");
        return normalized.endsWith("/api") ? normalized : normalized + "/api";
    }

    @Override
    public String startSession()
    {
        LOGGER.info(
                "Starting Parasoft coverage session for environment {} with userId='{}'",
                environmentId,
                userId);
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
            boolean baggageMissing = baggageHeader == null || baggageHeader.isBlank();

            if (parallelIdEnabled && baggageMissing) {
                LOGGER.debug(
                        "CTP startTest response did not include the baggage property: test={}, testCase={}, parallelId={}, responsePresent={}",
                        test,
                        testCase,
                        parallelId,
                        status != null);
                LOGGER.warn("This version of CTP does not support parallel tests within a single coverage session.");
            }

            if (baggageMissing && userId != null && !userId.isBlank()) {
                baggageHeader = TEST_OPERATOR_ID_BAGGAGE_PREFIX + userId;
            }

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
        LOGGER.info("Publishing coverage and test results to DTP...");
        try {
            String effectiveUserId = userId != null ? userId : this.userId;
            CoverageUploadRequestModelV3 uploadRequest = new CoverageUploadRequestModelV3();
            uploadRequest.setSessionTag(sessionTag);
            uploadRequest.setAnalysisType(AnalysisTypeEnum.UNIT_TEST);

            coverageApi.uploadCoverage(environmentId, sessionId, testConfig, effectiveUserId, toolName, true, uploadRequest);
            pollPublishStatus(sessionId);
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
                    StatusEnum status = result.getStatus();
                    String message = result.getMessage();

                    if (status == StatusEnum.PUBLISHED) {
                        LOGGER.info(message);
                        return;
                    }
                    if (status == StatusEnum.ERROR) {
                        LOGGER.error(message);
                        return;
                    }

                    LOGGER.debug(message);
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
