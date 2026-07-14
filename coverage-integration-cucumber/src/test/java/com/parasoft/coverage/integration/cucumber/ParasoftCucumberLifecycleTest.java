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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public class ParasoftCucumberLifecycleTest
{
    @Test
    public void startsStopsAndPublishesCoverageSession()
    {
        RecordingCoverageApiClient client =
                new RecordingCoverageApiClient("cucumber-session-123");
        ParasoftCucumberLifecycle lifecycle =
                new ParasoftCucumberLifecycle(client);

        lifecycle.startSession();
        lifecycle.stopSession();

        assertEquals(
                List.of(
                        "startSession",
                        "stopSession",
                        "publishResults:cucumber-session-123"),
                client.events);
    }

    @Test
    public void skipsPublishingWhenSessionIdentifierIsUnavailable()
    {
        RecordingCoverageApiClient client =
                new RecordingCoverageApiClient(null);
        ParasoftCucumberLifecycle lifecycle =
                new ParasoftCucumberLifecycle(client);

        lifecycle.startSession();
        lifecycle.stopSession();

        assertEquals(
                List.of(
                        "startSession",
                        "stopSession"),
                client.events);
    }

    @Test
    public void ignoresDuplicateLifecycleCalls()
    {
        RecordingCoverageApiClient client =
                new RecordingCoverageApiClient("cucumber-session-123");
        ParasoftCucumberLifecycle lifecycle =
                new ParasoftCucumberLifecycle(client);

        lifecycle.startSession();
        lifecycle.startSession();
        lifecycle.stopSession();
        lifecycle.stopSession();

        assertEquals(
                List.of(
                        "startSession",
                        "stopSession",
                        "publishResults:cucumber-session-123"),
                client.events);
    }

    private static final class RecordingCoverageApiClient
            implements CoverageApiClient
    {
        private final String sessionId;
        private final List<String> events = new ArrayList<>();

        private RecordingCoverageApiClient(String sessionId)
        {
            this.sessionId = sessionId;
        }

        @Override
        public String startSession()
        {
            events.add("startSession");
            return sessionId;
        }

        @Override
        public CoverageTestContext startTest(String test, String testCase)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void stopTest(
                String test,
                String testCase,
                CoverageTestContext testContext,
                ResultEnum result,
                String message)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void stopSession()
        {
            events.add("stopSession");
        }

        @Override
        public void publishResults(
                String sessionId,
                String testConfig,
                String userId,
                String toolName)
        {
            events.add("publishResults:" + sessionId);
        }
    }
}
