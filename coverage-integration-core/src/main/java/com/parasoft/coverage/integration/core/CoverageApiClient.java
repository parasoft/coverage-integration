/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */
package com.parasoft.coverage.integration.core;

import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;

public interface CoverageApiClient {
    void startSession();

    void startTest(String test, String testCas);

    void stopTest(String test, String testCase, ResultEnum result, String message);

    void stopSession();
}