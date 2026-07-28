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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

class ParasoftCoverageApiClientTest
{
    private static final long ENVIRONMENT_ID = 42L;

    private static final String USER_ID = "automation-user";
    private static final String SESSION_TAG = "nightly-junit";
    private static final String BEARER_TOKEN = "coverage-token";

    private static final String SESSION_ID = "coverage-session-123";
    private static final String TEST_ID = "com.example.CalculatorTest";
    private static final String TEST_CASE_ID = "addsNumbers";
    private static final String BAGGAGE_HEADER = "test-operator-id=automation-user+parallel-id";
    private static final String FAILURE_MESSAGE = "expected 4 but was 5";

    private static final String MISSING_BAGGAGE_WARNING =
            "This version of CTP does not support parallel tests within a single coverage session.";
    private static final String PUBLISH_STATUS_MESSAGE =
            "Publishing coverage and test results to DTP...";
    private static final String PUBLISH_SUCCESS_MESSAGE =
            "Successfully published coverage and results to DTP.";
    private static final String PUBLISH_FAILURE_MESSAGE =
            "Failed to publish coverage and results to DTP.";

    private static final String SESSION_START_PATH = "/api/v3/environments/42/agents/session/start";
    private static final String TEST_START_PATH = "/api/v3/environments/42/agents/test/start";
    private static final String TEST_STOP_PATH = "/api/v3/environments/42/agents/test/stop";
    private static final String SESSION_STOP_PATH = "/api/v3/environments/42/agents/session/stop";
    private static final String COVERAGE_PATH = "/api/v3/environments/42/coverage/" + SESSION_ID;

    private static final String SESSION_STATUS_RESPONSE = """
            {
              "test": null,
              "testCase": null,
              "session": "coverage-session-123"
            }
            """;

    private static final String TEST_STATUS_RESPONSE = """
            {
              "baggage": "test-operator-id=automation-user+parallel-id",
              "test": "com.example.CalculatorTest",
              "testCase": "addsNumbers",
              "session": "coverage-session-123"
            }
            """;

    private static final String TEST_STATUS_WITHOUT_BAGGAGE_RESPONSE = """
            {
              "test": "com.example.CalculatorTest",
              "testCase": "addsNumbers",
              "session": "coverage-session-123"
            }
            """;

    @RegisterExtension
    static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final Logger clientLogger =
            (Logger) LoggerFactory.getLogger(ParasoftCoverageApiClient.class);
    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    private Level originalLogLevel;
    private boolean originalAdditive;

    @BeforeEach
    void attachLogAppender()
    {
        originalLogLevel = clientLogger.getLevel();
        originalAdditive = clientLogger.isAdditive();
        clientLogger.setLevel(Level.DEBUG);
        clientLogger.setAdditive(false);
        logAppender.start();
        clientLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender()
    {
        clientLogger.detachAppender(logAppender);
        logAppender.stop();
        clientLogger.setLevel(originalLogLevel);
        clientLogger.setAdditive(originalAdditive);
    }

    @Test
    void executesCoverageLifecycleWithBearerAuthenticationAndParallelId()
    {
        WIREMOCK.stubFor(post(urlEqualTo(SESSION_START_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        WIREMOCK.stubFor(post(urlEqualTo(TEST_START_PATH))
                .willReturn(okJson(TEST_STATUS_RESPONSE)));

        WIREMOCK.stubFor(post(urlEqualTo(TEST_STOP_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        WIREMOCK.stubFor(post(urlEqualTo(SESSION_STOP_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                true,
                null,
                null,
                BEARER_TOKEN);

        String sessionId = client.startSession();
        CoverageTestContext testContext = client.startTest(TEST_ID, TEST_CASE_ID);

        client.stopTest(
                TEST_ID,
                TEST_CASE_ID,
                testContext,
                ResultEnum.FAIL,
                FAILURE_MESSAGE);

        client.stopSession();

        assertEquals(SESSION_ID, sessionId);
        assertNotNull(testContext);
        assertNotNull(testContext.getParallelId());
        assertFalse(testContext.getParallelId().isBlank());
        assertDoesNotThrow(() -> UUID.fromString(testContext.getParallelId()));
        assertEquals(BAGGAGE_HEADER, testContext.getBaggageHeader());

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_START_PATH))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withRequestBody(equalToJson("""
                        {
                          "userId": "automation-user"
                        }
                        """)));

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(TEST_START_PATH))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withRequestBody(equalToJson("""
                        {
                          "test": "com.example.CalculatorTest",
                          "testCase": "addsNumbers",
                          "userId": "automation-user",
                          "parallelId": "%s",
                          "workItems": []
                        }
                        """.formatted(testContext.getParallelId()))));

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(TEST_STOP_PATH))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withRequestBody(equalToJson("""
                        {
                          "test": "com.example.CalculatorTest",
                          "testCase": "addsNumbers",
                          "userId": "automation-user",
                          "parallelId": "%s",
                          "result": "FAIL",
                          "message": "expected 4 but was 5"
                        }
                        """.formatted(testContext.getParallelId()))));

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_STOP_PATH))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withRequestBody(equalToJson("""
                        {
                          "userId": "automation-user"
                        }
                        """)));
    }

    @Test
    void omitsParallelIdWhenParallelExecutionIsDisabled()
    {
        WIREMOCK.stubFor(post(urlEqualTo(TEST_START_PATH))
                .willReturn(okJson(TEST_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        CoverageTestContext testContext = client.startTest(TEST_ID, TEST_CASE_ID);

        assertNotNull(testContext);
        assertNull(testContext.getParallelId());
        assertEquals(BAGGAGE_HEADER, testContext.getBaggageHeader());

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(TEST_START_PATH))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "test": "com.example.CalculatorTest",
                          "testCase": "addsNumbers",
                          "userId": "automation-user",
                          "workItems": []
                        }
                        """)));
    }

    @Test
    void logsDebugAndWarningWhenParallelStartTestResponseDoesNotIncludeBaggage()
    {
        WIREMOCK.stubFor(post(urlEqualTo(TEST_START_PATH))
                .willReturn(okJson(TEST_STATUS_WITHOUT_BAGGAGE_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                true,
                null,
                null,
                BEARER_TOKEN);

        CoverageTestContext testContext = client.startTest(TEST_ID, TEST_CASE_ID);

        assertNotNull(testContext);
        assertNotNull(testContext.getParallelId());
        assertNull(testContext.getBaggageHeader());
        assertEquals(1, countLogEvents(
                Level.DEBUG,
                "CTP startTest response did not include the baggage property: test="
                        + TEST_ID
                        + ", testCase="
                        + TEST_CASE_ID
                        + ", parallelId="
                        + testContext.getParallelId()
                        + ", responsePresent=true"));
        assertEquals(1, countLogEvents(Level.WARN, MISSING_BAGGAGE_WARNING));
    }

    @Test
    void doesNotLogMissingBaggageWarningWhenParallelExecutionIsDisabled()
    {
        WIREMOCK.stubFor(post(urlEqualTo(TEST_START_PATH))
                .willReturn(okJson(TEST_STATUS_WITHOUT_BAGGAGE_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        CoverageTestContext testContext = client.startTest(TEST_ID, TEST_CASE_ID);

        assertNotNull(testContext);
        assertNull(testContext.getParallelId());
        assertNull(testContext.getBaggageHeader());
        assertEquals(0, countLogEvents(Level.WARN, MISSING_BAGGAGE_WARNING));
    }

    @Test
    void usesBasicAuthenticationWhenBearerTokenIsNotConfigured()
    {
        WIREMOCK.stubFor(post(urlEqualTo(SESSION_START_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                false,
                "admin",
                "secret",
                null);

        String sessionId = client.startSession();

        assertEquals(SESSION_ID, sessionId);

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_START_PATH))
                .withHeader("Authorization", equalTo("Basic YWRtaW46c2VjcmV0"))
                .withRequestBody(equalToJson("""
                        {
                          "userId": "automation-user"
                        }
                        """)));
    }

    @Test
    void appendsApiPathAndRemovesTrailingSlashFromCtpUrl()
    {
        String path = "/em/api/v3/environments/42/agents/session/start";
        WIREMOCK.stubFor(post(urlEqualTo(path))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = new ParasoftCoverageApiClient(
                "http://localhost:" + WIREMOCK.getRuntimeInfo().getHttpPort() + "/em/",
                ENVIRONMENT_ID,
                USER_ID,
                SESSION_TAG,
                false,
                "admin",
                "secret",
                null);

        assertEquals(SESSION_ID, client.startSession());
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(path)));
    }

    @Test
    void prefersBearerAuthenticationWhenBearerAndBasicCredentialsAreConfigured()
    {
        WIREMOCK.stubFor(post(urlEqualTo(SESSION_START_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                false,
                "admin",
                "secret",
                BEARER_TOKEN);

        String sessionId = client.startSession();

        assertEquals(SESSION_ID, sessionId);

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_START_PATH))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "userId": "automation-user"
                        }
                        """)));
    }

    @Test
    void usesBasicAuthenticationWhenBearerTokenIsBlank()
    {
        WIREMOCK.stubFor(post(urlEqualTo(SESSION_START_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                false,
                "admin",
                "secret",
                "   ");

        String sessionId = client.startSession();

        assertEquals(SESSION_ID, sessionId);

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_START_PATH))
                .withHeader(
                        "Authorization",
                        equalTo("Basic YWRtaW46c2VjcmV0"))
                .withRequestBody(equalToJson("""
                        {
                          "userId": "automation-user"
                        }
                        """)));
    }

    @Test
    void sendsUnauthenticatedRequestWhenCredentialsAreBlank()
    {
        WIREMOCK.stubFor(post(urlEqualTo(SESSION_START_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                false,
                "   ",
                "secret",
                "   ");

        String sessionId = client.startSession();

        assertEquals(SESSION_ID, sessionId);

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_START_PATH))
                .withHeader("Authorization", absent())
                .withRequestBody(equalToJson("""
                        {
                          "userId": "automation-user"
                        }
                        """)));
    }

    @Test
    void omitsUserIdWhenUserIsNotConfigured()
    {
        WIREMOCK.stubFor(post(urlEqualTo(SESSION_START_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client =
                new ParasoftCoverageApiClient(
                        "http://localhost:"
                                + WIREMOCK.getRuntimeInfo().getHttpPort() + "/api/",
                        ENVIRONMENT_ID,
                        null,
                        SESSION_TAG,
                        false,
                        null,
                        null,
                        BEARER_TOKEN);

        String sessionId = client.startSession();

        assertEquals(SESSION_ID, sessionId);

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_START_PATH))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                        }
                        """)));
    }

    @Test
    void omitsTestCaseWhenTestCaseIsNotProvided()
    {
        WIREMOCK.stubFor(post(urlEqualTo(TEST_START_PATH))
                .willReturn(okJson(TEST_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        CoverageTestContext testContext =
                client.startTest(TEST_ID, null);

        assertNotNull(testContext);
        assertNull(testContext.getParallelId());
        assertEquals(BAGGAGE_HEADER, testContext.getBaggageHeader());

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(TEST_START_PATH))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "test": "com.example.CalculatorTest",
                          "userId": "automation-user",
                          "workItems": []
                        }
                        """)));
    }

    @Test
    void omitsParallelIdAndMessageWhenStoppingWithoutTestContext()
    {
        WIREMOCK.stubFor(post(urlEqualTo(TEST_STOP_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        assertDoesNotThrow(() -> client.stopTest(
                TEST_ID,
                TEST_CASE_ID,
                null,
                ResultEnum.PASS,
                null));

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(TEST_STOP_PATH))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "test": "com.example.CalculatorTest",
                          "testCase": "addsNumbers",
                          "userId": "automation-user",
                          "result": "PASS"
                        }
                        """)));
    }

    @Test
    void omitsOptionalPublishDataWhenItIsNotConfigured()
    {
        WIREMOCK.stubFor(post(urlPathEqualTo(COVERAGE_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "message": "simulated CTP failure"
                                }
                                """)));

        ParasoftCoverageApiClient client =
                new ParasoftCoverageApiClient(
                        "http://localhost:"
                                + WIREMOCK.getRuntimeInfo().getHttpPort() + "/api/",
                        ENVIRONMENT_ID,
                        null,
                        null,
                        false,
                        null,
                        null,
                        BEARER_TOKEN);

        assertDoesNotThrow(() -> client.publishResults(
                SESSION_ID,
                null,
                null,
                null));

        WIREMOCK.verify(1, postRequestedFor(urlPathEqualTo(COVERAGE_PATH))
                .withQueryParam("testConfig", absent())
                .withQueryParam("userId", absent())
                .withQueryParam("toolName", absent())
                .withQueryParam("async", equalTo("true"))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "analysisType": "UNIT_TEST"
                        }
                        """)));

        WIREMOCK.verify(
                0,
                getRequestedFor(urlPathEqualTo(COVERAGE_PATH)));
    }

    @Test
    void usesConfiguredUserIdWhenPublishUserIdIsNotProvided()
    {
        WIREMOCK.stubFor(post(urlPathEqualTo(COVERAGE_PATH))
                .willReturn(aResponse().withStatus(500)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        assertDoesNotThrow(() -> client.publishResults(
                SESSION_ID,
                null,
                null,
                null));

        WIREMOCK.verify(1, postRequestedFor(urlPathEqualTo(COVERAGE_PATH))
                .withQueryParam("userId", equalTo(USER_ID)));
    }

    @Test
    void usesEmptyPasswordWhenBasicPasswordIsNotConfigured()
    {
        WIREMOCK.stubFor(post(urlEqualTo(SESSION_START_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                false,
                "admin",
                null,
                null);

        String sessionId = client.startSession();

        assertEquals(SESSION_ID, sessionId);

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_START_PATH))
                .withHeader(
                        "Authorization",
                        equalTo("Basic YWRtaW46"))
                .withRequestBody(equalToJson("""
                        {
                          "userId": "automation-user"
                        }
                        """)));
    }

    @Test
    void returnsNullWhenStartSessionResponseHasNoBody()
    {
        WIREMOCK.stubFor(post(urlEqualTo(SESSION_START_PATH))
                .willReturn(aResponse()
                        .withStatus(204)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        String sessionId = client.startSession();

        assertNull(sessionId);

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_START_PATH))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "userId": "automation-user"
                        }
                        """)));
    }

    @Test
    void returnsTestContextWithoutBaggageWhenStartTestResponseHasNoBody()
    {
        WIREMOCK.stubFor(post(urlEqualTo(TEST_START_PATH))
                .willReturn(aResponse()
                        .withStatus(204)));

        ParasoftCoverageApiClient client = createClient(
                true,
                null,
                null,
                BEARER_TOKEN);

        CoverageTestContext testContext =
                client.startTest(TEST_ID, TEST_CASE_ID);

        assertNotNull(testContext);
        assertNotNull(testContext.getParallelId());
        assertFalse(testContext.getParallelId().isBlank());
        assertDoesNotThrow(
                () -> UUID.fromString(testContext.getParallelId()));
        assertNull(testContext.getBaggageHeader());

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(TEST_START_PATH))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "test": "com.example.CalculatorTest",
                          "testCase": "addsNumbers",
                          "userId": "automation-user",
                          "parallelId": "%s",
                          "workItems": []
                        }
                        """.formatted(testContext.getParallelId()))));
    }

    @Test
    void serializesIncompleteResultWithoutMessage()
    {
        WIREMOCK.stubFor(post(urlEqualTo(TEST_STOP_PATH))
                .willReturn(okJson(SESSION_STATUS_RESPONSE)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        CoverageTestContext testContext =
                new CoverageTestContext(
                        "parallel-123",
                        BAGGAGE_HEADER);

        assertDoesNotThrow(() -> client.stopTest(
                TEST_ID,
                TEST_CASE_ID,
                testContext,
                ResultEnum.INCOMPLETE,
                null));

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(TEST_STOP_PATH))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "test": "com.example.CalculatorTest",
                          "testCase": "addsNumbers",
                          "userId": "automation-user",
                          "parallelId": "parallel-123",
                          "result": "INCOMPLETE"
                        }
                        """)));
    }

    @Test
    void returnsNullWhenStartSessionRequestFails()
    {
        WIREMOCK.stubFor(post(urlEqualTo(SESSION_START_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "message": "simulated CTP failure"
                                }
                                """)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        String sessionId = client.startSession();

        assertNull(sessionId);

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_START_PATH))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "userId": "automation-user"
                        }
                        """)));
    }

    @Test
    void returnsTestContextWithoutBaggageWhenStartTestRequestFails()
    {
        WIREMOCK.stubFor(post(urlEqualTo(TEST_START_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "message": "simulated CTP failure"
                                }
                                """)));

        ParasoftCoverageApiClient client = createClient(
                true,
                null,
                null,
                BEARER_TOKEN);

        CoverageTestContext testContext =
                client.startTest(TEST_ID, TEST_CASE_ID);

        assertNotNull(testContext);
        assertNotNull(testContext.getParallelId());
        assertFalse(testContext.getParallelId().isBlank());
        assertDoesNotThrow(() -> UUID.fromString(testContext.getParallelId()));
        assertNull(testContext.getBaggageHeader());
        assertEquals(0, countLogEvents(Level.WARN, MISSING_BAGGAGE_WARNING));

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(TEST_START_PATH))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "test": "com.example.CalculatorTest",
                          "testCase": "addsNumbers",
                          "userId": "automation-user",
                          "parallelId": "%s",
                          "workItems": []
                        }
                        """.formatted(testContext.getParallelId()))));
    }

    @Test
    void doesNotThrowWhenStopTestRequestFails()
    {
        WIREMOCK.stubFor(post(urlEqualTo(TEST_STOP_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "message": "simulated CTP failure"
                                }
                                """)));

        ParasoftCoverageApiClient client = createClient(
                true,
                null,
                null,
                BEARER_TOKEN);

        CoverageTestContext testContext =
                new CoverageTestContext("parallel-123", BAGGAGE_HEADER);

        assertDoesNotThrow(() -> client.stopTest(
                TEST_ID,
                TEST_CASE_ID,
                testContext,
                ResultEnum.FAIL,
                FAILURE_MESSAGE));

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(TEST_STOP_PATH))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "test": "com.example.CalculatorTest",
                          "testCase": "addsNumbers",
                          "userId": "automation-user",
                          "parallelId": "parallel-123",
                          "result": "FAIL",
                          "message": "expected 4 but was 5"
                        }
                        """)));
    }

    @Test
    void doesNotThrowWhenStopSessionRequestFails()
    {
        WIREMOCK.stubFor(post(urlEqualTo(SESSION_STOP_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "message": "simulated CTP failure"
                                }
                                """)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        assertDoesNotThrow(client::stopSession);

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo(SESSION_STOP_PATH))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "userId": "automation-user"
                        }
                        """)));
    }

    @Test
    void doesNotStartPollingWhenPublishRequestFails()
    {
        WIREMOCK.stubFor(post(urlPathEqualTo(COVERAGE_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "message": "simulated CTP failure"
                                }
                                """)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        assertDoesNotThrow(() -> client.publishResults(
                SESSION_ID,
                "Unit Test Configuration",
                USER_ID,
                "JUnit"));

        WIREMOCK.verify(1, postRequestedFor(urlPathEqualTo(COVERAGE_PATH))
                .withQueryParam("testConfig", equalTo("Unit Test Configuration"))
                .withQueryParam("userId", equalTo(USER_ID))
                .withQueryParam("toolName", equalTo("JUnit"))
                .withQueryParam("async", equalTo("true"))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withRequestBody(equalToJson("""
                        {
                          "sessionTag": "nightly-junit",
                          "analysisType": "UNIT_TEST"
                        }
                        """)));

        WIREMOCK.verify(0, getRequestedFor(urlPathEqualTo(COVERAGE_PATH)));
    }

    @Test
    void publishesCoverageAsynchronouslyAndPollsUntilPublished()
            throws InterruptedException
    {
        WIREMOCK.stubFor(post(urlPathEqualTo(COVERAGE_PATH))
                .withQueryParam("testConfig", equalTo("Unit Test Configuration"))
                .withQueryParam("userId", equalTo(USER_ID))
                .withQueryParam("toolName", equalTo("JUnit"))
                .withQueryParam("async", equalTo("true"))
                .willReturn(okJson("""
                        {
                          "status": "PUBLISHING"
                        }
                        """)));

        WIREMOCK.stubFor(get(urlPathEqualTo(COVERAGE_PATH))
                .withQueryParam("userId", equalTo(USER_ID))
                .willReturn(okJson("""
                        {
                          "status": "PUBLISHED",
                          "message": "Successfully published coverage and results to DTP.",
                          "passed": 1,
                          "failed": 0,
                          "incomplete": 0
                        }
                        """)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        client.publishResults(
                SESSION_ID,
                "Unit Test Configuration",
                USER_ID,
                "JUnit");

        WIREMOCK.verify(1, postRequestedFor(urlPathEqualTo(COVERAGE_PATH))
                .withQueryParam("testConfig", equalTo("Unit Test Configuration"))
                .withQueryParam("userId", equalTo(USER_ID))
                .withQueryParam("toolName", equalTo("JUnit"))
                .withQueryParam("async", equalTo("true"))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withRequestBody(equalToJson("""
                        {
                          "sessionTag": "nightly-junit",
                          "analysisType": "UNIT_TEST"
                        }
                        """)));

        RequestPatternBuilder pollRequest =
                getRequestedFor(urlPathEqualTo(COVERAGE_PATH))
                        .withQueryParam("userId", equalTo(USER_ID))
                        .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN))
                        .withHeader("Accept", equalTo("application/json"));

        awaitRequest(pollRequest, Duration.ofSeconds(10));

        WIREMOCK.verify(1, pollRequest);
    }

    @Test
    void logsPublishStartAndCompletionOnceWithoutRepeatingIntermediateInfoMessages()
    {
        WIREMOCK.stubFor(post(urlPathEqualTo(COVERAGE_PATH))
                .willReturn(okJson("""
                        {
                          "status": "PUBLISHING"
                        }
                        """)));

        String scenarioName = "publish status logging";
        String secondPollState = "second poll";
        String publishedState = "published";

        WIREMOCK.stubFor(get(urlPathEqualTo(COVERAGE_PATH))
                .inScenario(scenarioName)
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson("""
                        {
                          "status": "PUBLISHING",
                          "message": "Publishing coverage and test results to DTP..."
                        }
                        """))
                .willSetStateTo(secondPollState));

        WIREMOCK.stubFor(get(urlPathEqualTo(COVERAGE_PATH))
                .inScenario(scenarioName)
                .whenScenarioStateIs(secondPollState)
                .willReturn(okJson("""
                        {
                          "status": "PUBLISHING",
                          "message": "Publishing coverage and test results to DTP..."
                        }
                        """))
                .willSetStateTo(publishedState));

        WIREMOCK.stubFor(get(urlPathEqualTo(COVERAGE_PATH))
                .inScenario(scenarioName)
                .whenScenarioStateIs(publishedState)
                .willReturn(okJson("""
                        {
                          "status": "PUBLISHED",
                          "message": "Successfully published coverage and results to DTP.",
                          "passed": 1,
                          "failed": 0,
                          "incomplete": 0
                        }
                        """)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        client.publishResults(
                SESSION_ID,
                "Unit Test Configuration",
                USER_ID,
                "JUnit");

        assertEquals(1, countLogEvents(Level.INFO, PUBLISH_STATUS_MESSAGE));
        assertEquals(2, countLogEvents(Level.DEBUG, PUBLISH_STATUS_MESSAGE));
        assertEquals(1, countLogEvents(Level.INFO, PUBLISH_SUCCESS_MESSAGE));
    }

    @Test
    void logsPublishFailureOnce()
    {
        WIREMOCK.stubFor(post(urlPathEqualTo(COVERAGE_PATH))
                .willReturn(okJson("""
                        {
                          "status": "PUBLISHING"
                        }
                        """)));

        WIREMOCK.stubFor(get(urlPathEqualTo(COVERAGE_PATH))
                .willReturn(okJson("""
                        {
                          "status": "ERROR",
                          "message": "Failed to publish coverage and results to DTP."
                        }
                        """)));

        ParasoftCoverageApiClient client = createClient(
                false,
                null,
                null,
                BEARER_TOKEN);

        client.publishResults(
                SESSION_ID,
                "Unit Test Configuration",
                USER_ID,
                "JUnit");

        assertEquals(1, countLogEvents(Level.INFO, PUBLISH_STATUS_MESSAGE));
        assertEquals(1, countLogEvents(Level.ERROR, PUBLISH_FAILURE_MESSAGE));
    }

    private long countLogEvents(Level level, String message)
    {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == level)
                .filter(event -> message.equals(event.getFormattedMessage()))
                .count();
    }

    private static ParasoftCoverageApiClient createClient(
            boolean parallelIdEnabled,
            String username,
            String password,
            String token)
    {
        return new ParasoftCoverageApiClient(
                "http://localhost:" + WIREMOCK.getRuntimeInfo().getHttpPort() + "/api/",
                ENVIRONMENT_ID,
                USER_ID,
                SESSION_TAG,
                parallelIdEnabled,
                username,
                password,
                token);
    }

    private static void awaitRequest(
            RequestPatternBuilder requestPattern,
            Duration timeout)
            throws InterruptedException
    {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            if (!WIREMOCK.findAll(requestPattern).isEmpty()) {
                return;
            }

            Thread.sleep(10L);
        }

        fail("Timed out waiting for the expected WireMock request: "
                + requestPattern.build());
    }
}
