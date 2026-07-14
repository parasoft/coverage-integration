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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URI;

import io.cucumber.java.Status;

import org.junit.Test;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public class ParasoftCucumberScenarioListenerTest
{
    @Test
    public void buildsTestIdentifierFromFeatureFileAndScenarioName()
    {
        assertEquals(
                "petclinic.feature#Navigate to home page",
                ParasoftCucumberScenarioListener.buildTestId(
                        URI.create("classpath:features/petclinic.feature"),
                        "Navigate to home page"));
    }

    @Test
    public void extractsFeatureFileFromFileUri()
    {
        assertEquals(
                "parabank-demo.feature",
                ParasoftCucumberScenarioListener.extractFeatureFileName(
                        URI.create("file:///tmp/features/parabank-demo.feature")));
    }

    @Test
    public void mapsCucumberStatusesToCoverageResults()
    {
        assertEquals(
                ResultEnum.PASS,
                ParasoftCucumberScenarioListener.toCoverageResult(Status.PASSED));

        assertEquals(
                ResultEnum.FAIL,
                ParasoftCucumberScenarioListener.toCoverageResult(Status.FAILED));

        assertEquals(
                ResultEnum.FAIL,
                ParasoftCucumberScenarioListener.toCoverageResult(Status.AMBIGUOUS));

        assertEquals(
                ResultEnum.INCOMPLETE,
                ParasoftCucumberScenarioListener.toCoverageResult(Status.SKIPPED));

        assertEquals(
                ResultEnum.INCOMPLETE,
                ParasoftCucumberScenarioListener.toCoverageResult(Status.PENDING));

        assertEquals(
                ResultEnum.INCOMPLETE,
                ParasoftCucumberScenarioListener.toCoverageResult(Status.UNDEFINED));

        assertEquals(
                ResultEnum.INCOMPLETE,
                ParasoftCucumberScenarioListener.toCoverageResult(Status.UNUSED));
    }

    @Test
    public void startsAndStopsScenarioAndManagesExecutionContext()
    {
        RecordingCoverageApiClient client =
                new RecordingCoverageApiClient();
        ParasoftCucumberScenarioListener listener =
                new ParasoftCucumberScenarioListener(client);

        try {
            listener.startScenario(
                    "petclinic.feature#Navigate to home page",
                    "Navigate to home page");

            assertEquals(
                    "test-operator-id=automation-user+parallel-123",
                    CoverageExecutionContext.getCurrentBaggageHeader());

            listener.stopScenario(ResultEnum.PASS, null);

            assertEquals(
                    "petclinic.feature#Navigate to home page",
                    client.startedTest);
            assertEquals(
                    "Navigate to home page",
                    client.startedTestCase);
            assertEquals(
                    "petclinic.feature#Navigate to home page",
                    client.stoppedTest);
            assertEquals(
                    "Navigate to home page",
                    client.stoppedTestCase);
            assertEquals(ResultEnum.PASS, client.result);
            assertNull(client.message);
            assertNull(CoverageExecutionContext.getCurrentBaggageHeader());
        }
        finally {
            CoverageExecutionContext.clearCurrent();
        }
    }

    private static final class RecordingCoverageApiClient
            implements CoverageApiClient
    {
        private String startedTest;
        private String startedTestCase;
        private String stoppedTest;
        private String stoppedTestCase;
        private ResultEnum result;
        private String message;

        @Override
        public String startSession()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public CoverageTestContext startTest(String test, String testCase)
        {
            startedTest = test;
            startedTestCase = testCase;

            return new CoverageTestContext(
                    "parallel-123",
                    "test-operator-id=automation-user+parallel-123");
        }

        @Override
        public void stopTest(
                String test,
                String testCase,
                CoverageTestContext testContext,
                ResultEnum result,
                String message)
        {
            stoppedTest = test;
            stoppedTestCase = testCase;
            this.result = result;
            this.message = message;
        }

        @Override
        public void stopSession()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publishResults(
                String sessionId,
                String testConfig,
                String userId,
                String toolName)
        {
            throw new UnsupportedOperationException();
        }
    }
}
