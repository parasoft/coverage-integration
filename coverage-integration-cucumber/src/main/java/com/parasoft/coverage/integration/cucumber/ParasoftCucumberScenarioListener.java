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

package com.parasoft.coverage.integration.cucumber;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.cucumber.gherkin.GherkinParser;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.Status;
import io.cucumber.messages.types.Feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

/**
 * Cucumber hooks that report individual scenario lifecycle events to Parasoft
 * CTP.
 */
public class ParasoftCucumberScenarioListener
{
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ParasoftCucumberScenarioListener.class);

    private static final int COVERAGE_HOOK_ORDER = Integer.MIN_VALUE;
    private static final int MAX_RESULT_MESSAGE_LENGTH = 500;
    private static final String CLASSPATH_SCHEME = "classpath";
    private static final Map<URI, String> FEATURE_NAME_CACHE = new ConcurrentHashMap<>();

    private final CoverageApiClient coverageApiClient;

    private TestExecution currentTest;

    /**
     * Creates a listener using {@code coverage-integration.properties} and
     * system properties.
     */
    public ParasoftCucumberScenarioListener()
    {
        this(ParasoftCucumberFeatureListener.getCoverageApiClient());
    }

    ParasoftCucumberScenarioListener(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient = Objects.requireNonNull(
                coverageApiClient,
                "coverageApiClient must not be null");
    }

    /**
     * Starts coverage tracking before the scenario and before any user-defined
     * scenario setup hooks execute.
     *
     * @param scenario current Cucumber scenario
     */
    @Before(order = COVERAGE_HOOK_ORDER)
    public void beforeScenario(Scenario scenario)
    {
        Scenario currentScenario = Objects.requireNonNull(
                scenario,
                "scenario must not be null");

        String scenarioName = resolveScenarioName(currentScenario);
        String testId = buildTestId(currentScenario.getUri(), scenarioName);

        startScenario(testId, scenarioName);
    }

    /**
     * Stops coverage tracking after the scenario and after all user-defined
     * scenario cleanup hooks have executed.
     *
     * @param scenario completed Cucumber scenario
     */
    @After(order = COVERAGE_HOOK_ORDER)
    public void afterScenario(Scenario scenario)
    {
        Scenario completedScenario = Objects.requireNonNull(
                scenario,
                "scenario must not be null");

        ResultEnum result = toCoverageResult(completedScenario.getStatus());
        String resultMessage = result == ResultEnum.PASS
                ? null
                : buildResultMessage(completedScenario);

        stopScenario(result, resultMessage);
    }

    void startScenario(String testId, String testCaseId)
    {
        if (currentTest != null && !currentTest.stopped) {
            throw new IllegalStateException(
                    "A Parasoft Cucumber coverage test is already active");
        }

        TestExecution execution = new TestExecution(testId, testCaseId);
        currentTest = execution;

        LOGGER.debug("Starting Parasoft Cucumber scenario: test={}, testCase={}",
                testId,
                testCaseId);

        try {
            execution.testContext =
                    coverageApiClient.startTest(testId, testCaseId);
            execution.contextThreadId =
                    CoverageExecutionContext.setCurrent(execution.testContext);
        }
        catch (RuntimeException e) {
            LOGGER.error(
                    "Failed to initialize Parasoft coverage for Cucumber scenario: test={}, testCase={}",
                    testId,
                    testCaseId,
                    e);

            CoverageExecutionContext.clearCurrent();
            currentTest = null;
            throw e;
        }
    }

    void stopScenario(ResultEnum result, String resultMessage)
    {
        TestExecution execution = currentTest;

        try {
            if (execution != null && !execution.stopped) {
                execution.stopped = true;

                LOGGER.debug(
                        "Stopping Parasoft Cucumber scenario: test={}, testCase={}, result={}",
                        execution.testId,
                        execution.testCaseId,
                        result);

                coverageApiClient.stopTest(
                        execution.testId,
                        execution.testCaseId,
                        execution.testContext,
                        result,
                        resultMessage);
            }
            else {
                LOGGER.debug(
                        "No active Parasoft Cucumber scenario to stop for result={}",
                        result);
            }
        }
        finally {
            if (execution == null) {
                CoverageExecutionContext.clearCurrent();
            }
            else {
                CoverageExecutionContext.clear(execution.contextThreadId);
            }

            currentTest = null;
        }
    }

    static String buildTestId(URI scenarioUri, String scenarioName)
    {
        String featureName = resolveFeatureName(scenarioUri);

        if (featureName == null) {
            featureName = extractFeatureFileName(scenarioUri);
        }

        return featureName == null ? scenarioName : featureName + '#' + scenarioName;
    }

    static String resolveFeatureName(URI uri)
    {
        if (uri == null) {
            return null;
        }

        String cachedFeatureName = FEATURE_NAME_CACHE.get(uri);

        if (cachedFeatureName != null) {
            return cachedFeatureName;
        }

        String resolvedFeatureName = parseFeatureName(uri);

        if (resolvedFeatureName == null) {
            return null;
        }

        String existingFeatureName = FEATURE_NAME_CACHE.putIfAbsent(uri, resolvedFeatureName);

        return existingFeatureName == null ? resolvedFeatureName : existingFeatureName;
    }

    private static String parseFeatureName(URI uri)
    {
        try (InputStream featureSource = openFeatureSource(uri)) {
            GherkinParser parser = GherkinParser.builder()
                    .includeSource(false)
                    .includeGherkinDocument(true)
                    .includePickles(false)
                    .build();

            return parser.parse(uri.toString(), featureSource)
                    .flatMap(envelope -> envelope.getGherkinDocument().stream())
                    .flatMap(gherkinDocument -> gherkinDocument.getFeature().stream())
                    .map(ParasoftCucumberScenarioListener::formatFeatureName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        catch (IOException | RuntimeException e) {
            LOGGER.warn("Unable to resolve Cucumber feature name from {}; using the feature file name instead", uri, e);

            return null;
        }
    }

    private static String formatFeatureName(Feature feature)
    {
        String featureName = feature.getName();

        if (featureName == null || featureName.isBlank()) {
            return null;
        }

        String normalizedFeatureName = featureName.strip();
        String featureKeyword = feature.getKeyword();

        if (featureKeyword == null || featureKeyword.isBlank()) {
            return normalizedFeatureName;
        }

        String normalizedFeatureKeyword = featureKeyword.strip();

        return normalizedFeatureKeyword.endsWith(":")
                ? normalizedFeatureKeyword + ' ' + normalizedFeatureName
                : normalizedFeatureKeyword + ": " + normalizedFeatureName;
    }

    private static InputStream openFeatureSource(URI uri) throws IOException
    {
        String scheme = uri.getScheme();

        if (CLASSPATH_SCHEME.equalsIgnoreCase(scheme)) {
            String resourceName = uri.getSchemeSpecificPart();

            while (resourceName != null && resourceName.startsWith("/")) {
                resourceName = resourceName.substring(1);
            }

            if (resourceName == null || resourceName.isBlank()) {
                throw new IOException("Cucumber classpath feature URI has no resource name: " + uri);
            }

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            if (classLoader == null) {
                classLoader = ParasoftCucumberScenarioListener.class.getClassLoader();
            }

            InputStream featureSource = classLoader.getResourceAsStream(resourceName);

            if (featureSource == null) {
                throw new IOException("Unable to open Cucumber feature resource: " + uri);
            }

            return featureSource;
        }

        if (scheme == null || scheme.isBlank()) {
            String path = uri.getPath();

            if (path == null || path.isBlank()) {
                path = uri.toString();
            }

            return Files.newInputStream(Path.of(path));
        }

        return uri.toURL().openStream();
    }

    static String extractFeatureFileName(URI uri)
    {
        if (uri == null) {
            return null;
        }

        String path = uri.getPath();

        if (path == null || path.isBlank()) {
            path = uri.getSchemeSpecificPart();
        }

        if (path == null || path.isBlank()) {
            return null;
        }

        path = path.replace('\\', '/');

        int archiveSeparator = path.lastIndexOf("!/");
        if (archiveSeparator >= 0) {
            path = path.substring(archiveSeparator + 2);
        }

        int lastSeparator = path.lastIndexOf('/');
        String fileName = lastSeparator >= 0
                ? path.substring(lastSeparator + 1)
                : path;

        return fileName.isBlank() ? null : fileName;
    }

    static ResultEnum toCoverageResult(Status status)
    {
        if (status == Status.PASSED) {
            return ResultEnum.PASS;
        }

        if (status == Status.FAILED || status == Status.AMBIGUOUS) {
            return ResultEnum.FAIL;
        }

        return ResultEnum.INCOMPLETE;
    }

    private static String resolveScenarioName(Scenario scenario)
    {
        String scenarioName = scenario.getName();

        if (scenarioName != null && !scenarioName.isBlank()) {
            return scenarioName;
        }

        String scenarioId = scenario.getId();

        if (scenarioId != null && !scenarioId.isBlank()) {
            return scenarioId;
        }

        return "Cucumber scenario";
    }

    private static String buildResultMessage(Scenario scenario)
    {
        String scenarioName = resolveScenarioName(scenario);
        String status = scenario.getStatus() == null
                ? "UNKNOWN"
                : scenario.getStatus().name();

        String message = "Cucumber scenario '" + scenarioName
                + "' finished with status " + status;

        String sanitized = message
                .replace('"', '\'')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');

        return sanitized.length() <= MAX_RESULT_MESSAGE_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_RESULT_MESSAGE_LENGTH);
    }

    private static final class TestExecution
    {
        private final String testId;
        private final String testCaseId;

        private CoverageTestContext testContext;
        private long contextThreadId = -1L;
        private boolean stopped;

        private TestExecution(String testId, String testCaseId)
        {
            this.testId = testId;
            this.testCaseId = testCaseId;
        }
    }
}
