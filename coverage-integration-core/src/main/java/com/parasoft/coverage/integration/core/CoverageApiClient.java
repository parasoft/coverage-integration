/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.core;

import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public interface CoverageApiClient
{
    String startSession();

    CoverageTestContext startTest(String test, String testCase);

    void stopTest(String test, String testCase, CoverageTestContext testContext, ResultEnum result, String message);

    void stopSession();

    void publishResults(String sessionId, String testConfig, String userId, String toolName);
}
