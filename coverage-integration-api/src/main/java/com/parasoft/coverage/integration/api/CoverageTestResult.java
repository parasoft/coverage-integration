/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.api;

/**
 * Final result for a test reported through {@link CoverageApiClient}.
 */
public enum CoverageTestResult
{
    /**
     * The test passed.
     */
    PASS,

    /**
     * The test failed.
     */
    FAIL,

    /**
     * The test did not complete.
     */
    INCOMPLETE
}
