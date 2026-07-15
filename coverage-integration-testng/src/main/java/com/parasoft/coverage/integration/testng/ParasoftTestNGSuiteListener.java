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

package com.parasoft.coverage.integration.testng;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageApiClientFactory;

public class ParasoftTestNGSuiteListener implements ISuiteListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftTestNGSuiteListener.class);

    private final CoverageApiClient coverageApiClient;
    private String sessionId;

    public ParasoftTestNGSuiteListener()
    {
        this(CoverageApiClientFactory.createFromSettings());
    }

    public ParasoftTestNGSuiteListener(CoverageApiClient coverageApiClient)
    {
        this.coverageApiClient = coverageApiClient;
    }

    @Override
    public void onStart(ISuite suite)
    {
        LOGGER.info("TestNG suite starting; starting Parasoft coverage session");
        sessionId = coverageApiClient.startSession();
    }

    @Override
    public void onFinish(ISuite suite)
    {
        LOGGER.info("TestNG suite finished; stopping Parasoft coverage session");
        coverageApiClient.stopSession();
        if (sessionId != null) {
            coverageApiClient.publishResults(sessionId, null, null, null);
        }
    }
}
