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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoverageIntegrationSettings {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageIntegrationSettings.class);

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
        return Boolean.parseBoolean(getOptional("parasoft.coverage.integration.parallel.test.enabled"));
    }

    public String getSessionTag() {
        return getOptional("parasoft.coverage.integration.dtp.sessionTag");
    }

    private String getRequired(String key) {
        String value = getOptional(key);
        if (value == null || value.isBlank()) {
            LOGGER.error("Missing required coverage integration property: {}", key);
            throw new IllegalStateException("Missing required property: " + key);
        }
        LOGGER.debug("Resolved required coverage integration property: {}", key);
        return value;
    }

    private String getOptional(String key) {
        String value = System.getProperty(key);
        String source = "system property";
        if (value == null) {
            value = properties.getProperty(key);
            source = "classpath properties";
        }
        if (value == null) {
            LOGGER.debug("Coverage integration property is not configured: {}", key);
        } else {
            LOGGER.debug("Resolved coverage integration property {} from {}", key, source);
        }
        return resolveEnvironmentVariables(value);
    }

    private void loadClasspathProperties() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream input = classLoader.getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                LOGGER.info("Loaded coverage integration settings from {}", CONFIG_FILE);
            } else {
                LOGGER.warn("Coverage integration settings file {} was not found on the classpath", CONFIG_FILE);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load coverage integration settings from {}", CONFIG_FILE, e);
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
            if (envVarValue == null) {
                LOGGER.warn("Environment variable {} referenced by coverage integration settings is not set", envVarName);
            } else {
                LOGGER.debug("Resolved environment variable reference in coverage integration settings: {}", envVarName);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(envVarValue == null ? "" : envVarValue));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
