# [Parasoft](https://www.parasoft.com) Coverage Integration

[![javadoc](https://img.shields.io/badge/javadoc-1.0.0-brightgreen.svg)](https://parasoft.github.io/coverage-integration/index.html)

Coverage Integration reports test execution and coverage to Parasoft Continuous Testing Platform (CTP)

Supported integrations include:

- JUnit 4
- JUnit 5
- JUnit 6
- TestNG
- Cucumber
- Selenium
- Playwright

Most users only need:

1. Add the framework dependency.
2. Configure `coverage-integration.properties`.
3. Run tests.

## Table of Contents

- [API](#api)
- [Coverage Configuration](#coverage-configuration)
- [Framework Integrations](#framework-integrations)
  - [JUnit 4](#junit-4)
  - [JUnit 5/6](#junit-56)
  - [TestNG](#testng)
  - [Cucumber](#cucumber)
- [Browser Integrations](#browser-integrations)
  - [Selenium](#selenium)
  - [Playwright](#playwright)
- [Logging](#logging)

## API

User tests should compile against the `coverage-integration-api` module and import classes from `com.parasoft.coverage.integration.api`. The `coverage-integration-core` module is for internal use only.

Add the following dependency to your Maven project:
```xml
<dependencies>
    <dependency>
        <groupId>com.parasoft</groupId>
        <artifactId>coverage-integration-api</artifactId>
        <version>${coverage-integration.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

When coverage agents are running in multi-user mode or tests execute in parallel, browser requests must include the current test's `Baggage` header so that coverage can be correctly associated with the executing test.

The following example retrieves the current `Baggage` header, creates a proxy that injects the header into browser requests, and configures the Chrome driver to use that proxy.

```java
String baggageHeader = CoverageIntegration.getBaggageHeader();
if (baggageHeader == null || baggageHeader.isBlank()) {
    return;
}

ChromeOptions options = new ChromeOptions();
ParasoftHeaderInjectingProxy coverageProxy =
        new ParasoftHeaderInjectingProxy(PROXY_BIND_HOST, 0, baggageHeader);
SeleniumCoverageIntegration.configureChromeOptions(options, coverageProxy);
```

For rare standalone use cases, such as tests launched from a `main` method, use `CoverageApiClient` from the API module to start and stop sessions and tests directly.

## Coverage Configuration

Create a coverage-integration.properties file to configure communication with CTP during the testing workflow. This file also provides the information needed to publish test results and coverage after all tests have completed.

```properties
# URL that points your CTP instance
parasoft.coverage.integration.ctp.url=http://localhost:8080/em/

# The ID of CTP environment where your coverage agents are configured
parasoft.coverage.integration.ctp.envId=1

# Session tag used when publishing test and coverage results
parasoft.coverage.integration.dtp.sessionTag=unit-testing-session

# Authentication username for CTP
parasoft.coverage.integration.ctp.auth.username=admin

# Password for CTP with support for variable resolution
parasoft.coverage.integration.ctp.auth.password=${env_var:PASSWORD}

# OAuth bearer token in the case where CTP is setup with OIDC authentication
parasoft.coverage.integration.ctp.auth.token=<bearer token>

# Enables support for parallel test execution. When enabled, the
# coverage-integration library isolates coverage data for each test.
parasoft.coverage.integration.parallel.test.enabled=true

# Identifies the user associated with the coverage session. When running
# tests in parallel, this value is used to isolate coverage data between
# concurrent test executions.
parasoft.coverage.intergration.ctp.userId=tester
```
Place this file on your project's classpath, for example in src/test/resources.

## Framework Integrations

### JUnit 4

JUnit 4 users should add the JUnit 4 integration dependency and register the run listener in Maven Surefire. The listener starts the coverage session when the JUnit run starts and stops/publishes the session when the run finishes.

```xml
<dependencies>
    <dependency>
        <groupId>com.parasoft</groupId>
        <artifactId>coverage-integration-junit4</artifactId>
        <version>${coverage-integration.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <properties>
                    <property>
                        <name>listener</name>
                        <value>com.parasoft.coverage.integration.junit4.ParasoftJUnit4RunListener</value>
                    </property>
                </properties>
            </configuration>
        </plugin>
    </plugins>
</build>
```

If JUnit 4 tests run through Maven Failsafe, configure the same listener there:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <properties>
            <property>
                <name>listener</name>
                <value>com.parasoft.coverage.integration.junit4.ParasoftJUnit4RunListener</value>
            </property>
        </properties>
    </configuration>
</plugin>
```

The JUnit 4 TestWatcher must be added to each test class that should publish test results and coverage.
```java
@Rule
public ParasoftJUnit4Watcher parasoftJUnit4Watcher = new ParasoftJUnit4Watcher();
```

## JUnit 5/6
Add the Maven dependency that matches the version of JUnit used by your project. The example below demonstrates the dependency for JUnit 5.

```xml
<dependencies>
    <dependency>
        <groupId>com.parasoft</groupId>
        <artifactId>coverage-integration-junit5</artifactId>
        <version>${coverage-integration.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Enable JUnit extension auto-detection by setting the following system property when running your unit tests. This allows the extension to be loaded automatically without modifying your existing test code.

-Djunit.jupiter.extensions.autodetection.enabled=true

### TestNG

Add the Maven dependency for the TestNG coverage integration:

```xml
<dependency>
    <groupId>com.parasoft</groupId>
    <artifactId>coverage-integration-testng</artifactId>
    <version>${coverage-integration.version}</version>
    <scope>test</scope>
</dependency>
```

After adding the Maven dependency, modify your `testng.xml` file, which is used to organize, configure, and execute TestNG test suites without modifying Java code.

The integration provides two custom listeners:

- [`ParasoftTestNGSuiteListener`](coverage-integration-testng/src/main/java/com/parasoft/coverage/integration/testng/ParasoftTestNGSuiteListener.java)
- [`ParasoftTestNGTestListener`](coverage-integration-testng/src/main/java/com/parasoft/coverage/integration/testng/ParasoftTestNGTestListener.java)

Add both listeners to your `testng.xml` file as shown below:

```xml
<suite name="PetClinic Selenium TestNG Suite">
    <listeners>
        <listener class-name="com.parasoft.coverage.integration.testng.ParasoftTestNGSuiteListener"/>
        <listener class-name="com.parasoft.coverage.integration.testng.ParasoftTestNGTestListener"/>
    </listeners>

    <test name="NavigateIT">
        <classes>
            <class name="org.springframework.samples.petclinic.selenium.testng.NavigateIT"/>
        </classes>
    </test>

    <test name="PetIT">
        <classes>
            <class name="org.springframework.samples.petclinic.selenium.testng.PetIT"/>
        </classes>
    </test>
</suite>
```

### Cucumber

The `coverage-integration-cucumber` module automatically reports Cucumber 7.x scenario execution and coverage to CTP. It manages the coverage session for the duration of the Cucumber test run and reports each scenario as an individual test case.

Add the following dependency to the Maven module that runs the Cucumber tests:

```xml
<dependency>
    <groupId>com.parasoft</groupId>
    <artifactId>coverage-integration-cucumber</artifactId>
    <version>${coverage-integration.version}</version>
    <scope>test</scope>
</dependency>
```

> **Note**
>
> Do not add `coverage-integration-junit4`, `coverage-integration-junit5`, `coverage-integration-junit6`, or `coverage-integration-testng` to the same Cucumber test run. The Cucumber integration manages the coverage lifecycle for the entire test run.

Add `com.parasoft.coverage.integration.cucumber` to the Cucumber `glue` configuration alongside your project's existing step-definition packages.

Replace `com.example.steps` and `features` in the following examples with the step-definition package and feature location used by your project.

##### JUnit 4

```java
import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "classpath:features",
        glue = {
                "com.example.steps",
                "com.parasoft.coverage.integration.cucumber"
        })
public class RunCucumberTest
{
}
```

##### JUnit 5/6

```java
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
        key = GLUE_PROPERTY_NAME,
        value = "com.example.steps,"
                + "com.parasoft.coverage.integration.cucumber")
public class RunCucumberTest
{
}
```

##### TestNG

```java
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "classpath:features",
        glue = {
                "com.example.steps",
                "com.parasoft.coverage.integration.cucumber"
        })
public class RunCucumberTest
        extends AbstractTestNGCucumberTests
{
}
```

Each Cucumber scenario is reported as an individual test case.

For example:

```text
Feature: PetClinic browser actions
Scenario: Add a new pet
```

is reported to CTP as:

```text
test=Feature: PetClinic browser actions#Add a new pet
testCase=Add a new pet
```
## Browser Integrations

Browser integrations are required when coverage agents are running in multi-user mode or when tests execute in parallel. They propagate the coverage `Baggage` header so that coverage agents can correctly correlate browser activity with the current test using the configured `userId` and, when applicable, the generated `parallelId`

### Playwright

If your tests use Playwright, add the Playwright integration dependency alongside the Cucumber, JUnit, or TestNG integration dependency

```xml
<dependency>
    <groupId>com.parasoft</groupId>
    <artifactId>coverage-integration-playwright</artifactId>
    <version>${coverage-integration.version}</version>
    <scope>test</scope>
</dependency>
```

When creating a new browser context, use `PlaywrightCoverageIntegration#createBrowserContextOptions()`. If you already have an existing `Browser.NewContextOptions` instance, use `PlaywrightCoverageIntegration#updateBrowserContextOptions()` to add the coverage configuration before creating the browser context. Create the browser context using the configured options, then create pages from that context.

```java
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.parasoft.coverage.integration.playwright.PlaywrightCoverageIntegration;

Browser.NewContextOptions contextOptions = PlaywrightCoverageIntegration.createBrowserContextOptions();

BrowserContext context = browser.newContext(contextOptions);
Page page = context.newPage();
```

The configured browser context automatically includes the current test's `Baggage` header when CTP provides one. This allows coverage agents to correlate browser activity with the correct test execution. In single-user mode, or when no baggage value is available, no additional HTTP headers are added.

### Selenium

If your tests use Selenium, add the Selenium integration dependency alongside the Cucumber, JUnit, or TestNG integration dependency.

```xml
<dependency>
    <groupId>com.parasoft</groupId>
    <artifactId>coverage-integration-selenium</artifactId>
    <version>${coverage-integration.version}</version>
    <scope>test</scope>
</dependency>
```

Chrome, Edge, and Firefox are supported. Use the browser-specific factory method that matches the Selenium driver being created:

- `createChromeProxyConfig()`
- `createEdgeProxyConfig()`
- `createFirefoxProxyConfig()`

Create a browser coverage configuration before creating the Selenium driver. The coverage configuration propagates the current test's `Baggage` header to the browser and manages the proxy used for coverage correlation. The proxy is automatically shut down when the coverage configuration is closed.

Firefox example using the Firefox coverage handle and options:

```java
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.FirefoxCoverageConfig;

try (FirefoxCoverageConfig coverage = SeleniumCoverageIntegration.createFirefoxProxyConfig()) {
    WebDriver driver = new FirefoxDriver(coverage.getFirefoxOptions());

    try {
        // test code
    }
    finally {
        driver.quit();
    }
}
```

When Selenium tests execute in parallel, create a separate browser coverage configuration for each browser instance. Each configuration starts its own proxy and captures the current test's `Baggage` header.

For Chrome and Edge, the `Baggage` header can be configured using Chrome DevTools Protocol (CDP) instead of the built-in proxy.

```java
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

ChromeDriver driver = new ChromeDriver();
SeleniumCoverageIntegration.configureCdpBaggageHeader(driver);
```

To set explicit headers instead, use `configureCdpHeaders(driver, headers)`.

Call `configureCdpBaggageHeader` separately for each Chrome or Edge browser session used by parallel tests. The proxy-based approach works with all supported browsers. CDP is an alternative available only for Chrome and Edge.

## Logging

This project uses SLF4J and includes only the `slf4j-api` dependency. It does not provide or configure a logging backend, so debug logging is not shown by default. Applications that use this library control logging through their own SLF4J backend, such as Logback, Log4j 2, JUL, or `slf4j-simple`.

The project logs under the `com.parasoft.coverage.integration` package.

## Enable Debug Logging

Configure your application's logging backend to set `com.parasoft.coverage.integration` to `DEBUG`.

### Logback

Add or update `logback.xml` on the application classpath:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.parasoft.coverage.integration" level="DEBUG" />

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

### Log4j 2

Add or update `log4j2.xml` on the application classpath:

```xml
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n" />
        </Console>
    </Appenders>

    <Loggers>
        <Logger name="com.parasoft.coverage.integration" level="debug" />
        <Root level="info">
            <AppenderRef ref="Console" />
        </Root>
    </Loggers>
</Configuration>
```

### slf4j-simple

Pass this system property when running tests:

```shell
-Dorg.slf4j.simpleLogger.log.com.parasoft.coverage.integration=debug
```

For Maven Surefire:

```shell
mvn test -Dorg.slf4j.simpleLogger.log.com.parasoft.coverage.integration=debug
```
