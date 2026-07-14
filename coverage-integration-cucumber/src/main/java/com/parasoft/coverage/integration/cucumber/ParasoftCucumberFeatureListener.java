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

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;

/**
 * Cucumber global hooks that manage the Parasoft coverage session lifecycle.
 */
public class ParasoftCucumberFeatureListener
{
    private static final int COVERAGE_HOOK_ORDER = Integer.MIN_VALUE;

    private static final ParasoftCucumberLifecycle LIFECYCLE =
            new ParasoftCucumberLifecycle(
                    CoverageApiClientFactory.createFromSettings());

    /**
     * Starts the Parasoft coverage session before any Cucumber scenario or
     * user-defined global setup hook runs.
     */
    @BeforeAll(order = COVERAGE_HOOK_ORDER)
    public static void beforeAllScenarios()
    {
        LIFECYCLE.startSession();
    }

    /**
     * Stops and publishes the Parasoft coverage session after all Cucumber
     * scenarios and user-defined global cleanup hooks have completed.
     */
    @AfterAll(order = COVERAGE_HOOK_ORDER)
    public static void afterAllScenarios()
    {
        LIFECYCLE.stopSession();
    }

    static CoverageApiClient getCoverageApiClient()
    {
        return LIFECYCLE.getCoverageApiClient();
    }
}
