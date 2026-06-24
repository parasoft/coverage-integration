/*
 * (C) Copyright Parasoft Corporation 2026.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.coverage.integration.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CoverageIntegrationSettings {
    private static final String CONFIG_FILE = "coverage-integration.properties";
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{env_var:([^}]+)}");

    private final Properties properties = new Properties();

    public CoverageIntegrationSettings() {
        loadClasspathProperties();
    }

    public String getCtpUrl() {
        return getRequired("parasoft.coverage.integration.ctp.url");
    }

    public String getUsername() {
        return getOptional("parasoft.coverage.integration.ctp.auth.username");
    }

    public String getPassword() {
        return getOptional("parasoft.coverage.integration.ctp.auth.password");
    }

    public String getToken() {
        return getOptional("parasoft.coverage.integration.ctp.auth.token");
    }

    public Long getEnvironmentId() {
        return Long.parseLong(getRequired("parasoft.coverage.integration.ctp.envId"));
    }

    public String getUserId() {
        return getOptional("parasoft.coverage.integration.ctp.userId");
    }

    public boolean isParallelIdEnabled() {
        return Boolean.parseBoolean(getOptional("parasoft.coverage.integration.ctp.parallelId"));
    }

    public String getSessionTag() {
        return getOptional("parasoft.coverage.integration.dtp.sessionTag");
    }

    private String getRequired(String key) {
        String value = getOptional(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }

    private String getOptional(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        return resolveEnvironmentVariables(value);
    }

    private void loadClasspathProperties() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream input = classLoader.getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + CONFIG_FILE, e);
        }
    }

    private static String resolveEnvironmentVariables(String value) {
        if (value == null) {
            return null;
        }

        Matcher matcher = ENV_VAR_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String envVarName = matcher.group(1);
            String envVarValue = System.getenv(envVarName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(envVarValue == null ? "" : envVarValue));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}