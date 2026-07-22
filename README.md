# Coverage Integration

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
- [Javadoc](#javadoc)

## API

User tests should compile against the `coverage-integration-api` module and import classes from `com.parasoft.coverage.integration.api`. The core module is internal.

Use `CoverageIntegration#getCurrentTestOperatorIdHeader()` to get the `Baggage` header value that contains the current `test-operator-id` returned by the CTP `/test/start` API.

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
## Browser Integrations

### Playwright

Add the Playwright integration dependency alongside the Cucumber, JUnit, or TestNG integration dependency used by the test project.

```xml
<dependency>
    <groupId>com.parasoft</groupId>
    <artifactId>coverage-integration-playwright</artifactId>
    <version>${coverage-integration.version}</version>
    <scope>test</scope>
</dependency>
```

Use `PlaywrightCoverageIntegration#createBrowserContextOptions()` when creating the browser context. Create the page from that configured context.

```java
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.parasoft.coverage.integration.playwright.PlaywrightCoverageIntegration;

Browser.NewContextOptions contextOptions = PlaywrightCoverageIntegration.createBrowserContextOptions();

BrowserContext context = browser.newContext(contextOptions);
Page page = context.newPage();
```

The returned options include the current test's `Baggage` header when CTP provides one. In single-user mode, or when no baggage value is available, the returned options contain no additional HTTP headers.

### Selenium

Add the Selenium integration dependency alongside the Cucumber, JUnit, or TestNG integration dependency used by the test project.

```xml
<dependency>
    <groupId>com.parasoft</groupId>
    <artifactId>coverage-integration-selenium</artifactId>
    <version>${coverage-integration.version}</version>
    <scope>test</scope>
</dependency>
```

Use `SeleniumCoverageIntegration#createChromeBrowserCoverage()`, `SeleniumCoverageIntegration#createEdgeBrowserCoverage()`, `SeleniumCoverageIntegration#createFirefoxBrowserCoverage()`, or `SeleniumCoverageIntegration#createSafariBrowserCoverage()` for each browser. The returned handle owns a dedicated proxy for that browser and closes the proxy when the browser session is done.

```java
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.ChromeCoverageConfig;

try (ChromeCoverageConfig coverage = SeleniumCoverageIntegration.createChromeBrowserCoverage()) {
    WebDriver driver = new ChromeDriver(coverage.getChromeOptions());

    try {
        // test code
    }
    finally {
        driver.quit();
    }
}
```

For Firefox, use the Firefox coverage handle and options:

```java
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.FirefoxCoverageConfig;

try (FirefoxCoverageConfig coverage = SeleniumCoverageIntegration.createFirefoxBrowserCoverage()) {
    WebDriver driver = new FirefoxDriver(coverage.getFirefoxOptions());

    try {
        // test code
    }
    finally {
        driver.quit();
    }
}
```

For Safari, use the Safari coverage handle and options:

```java
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.SafariCoverageConfig;

try (SafariCoverageConfig coverage = SeleniumCoverageIntegration.createSafariBrowserCoverage()) {
    WebDriver driver = new SafariDriver(coverage.getSafariOptions());

    try {
        // test code
    }
    finally {
        driver.quit();
    }
}
```

When Selenium tests run in parallel, create a separate browser coverage handle inside each test before creating that test's browser. Each handle starts a separate proxy and captures the current test's `Baggage` header for that browser.

Chrome and Edge can use Chrome DevTools Protocol instead of a proxy. Create the driver normally, then configure the current test's `Baggage` header before navigating to the application under test.

```java
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

ChromeDriver driver = new ChromeDriver();
SeleniumCoverageIntegration.configureCdpBaggageHeader(driver);
```

To set explicit headers instead, use `configureCdpHeaders(driver, headers)`.

Call `configureCdpBaggageHeader` separately for each Chrome or Edge browser session used by parallel tests. Firefox and Safari do not support this CDP path; use `createFirefoxBrowserCoverage()` or `createSafariBrowserCoverage()` for those browsers.

## Javadoc

The API module generates Javadoc as part of the Maven `package` phase:

```shell
mvn -pl coverage-integration-api -am package
```

The generated documentation is written to `coverage-integration-api/target/reports/apidocs`. The GitHub Actions workflow in `.github/workflows/publish-javadoc.yml` publishes that Javadoc to GitHub Pages when changes are pushed to `master`, or when the workflow is run manually.

## Cucumber

Use `coverage-integration-cucumber` to report Cucumber 7.x scenario results and coverage through CTP. After the integration is configured, each Cucumber scenario is reported to DTP using the scenario name as the test case name.

Before running the tests, configure a CTP environment with the coverage agents for the application under test.

### Required setup

#### 1. Add the Cucumber integration dependency

Add the following dependency to the Maven module that runs the Cucumber tests:

```xml
<dependency>
    <groupId>com.parasoft</groupId>
    <artifactId>coverage-integration-cucumber</artifactId>
    <version>${coverage-integration.version}</version>
    <scope>test</scope>
</dependency>
```

Keep the Cucumber dependencies already required by the project, including `cucumber-java` and the runner used by the test suite, such as `cucumber-junit`, `cucumber-junit-platform-engine`, or `cucumber-testng`.

Do not add `coverage-integration-junit4`, `coverage-integration-junit5`, `coverage-integration-junit6`, or `coverage-integration-testng` to the same Cucumber test run. The Cucumber integration manages the coverage lifecycle for that run.

#### 2. Configure the CTP connection

Create this file in the test resources:

```text
src/test/resources/coverage-integration.properties
```

At minimum, configure the CTP URL and the environment containing the coverage agents:

```properties
parasoft.coverage.integration.ctp.url=http://localhost:8080/em/
parasoft.coverage.integration.ctp.envId=1
```

When CTP uses basic authentication, add:

```properties
parasoft.coverage.integration.ctp.auth.username=admin
parasoft.coverage.integration.ctp.auth.password=${env_var:CTP_PASSWORD}
```

When CTP uses OIDC authentication, configure a bearer token instead:

```properties
parasoft.coverage.integration.ctp.auth.token=${env_var:CTP_TOKEN}
```

The properties can also be supplied as Java system properties. A system property overrides the value in `coverage-integration.properties`.

#### 3. Add the Parasoft package to the Cucumber glue

Add `com.parasoft.coverage.integration.cucumber` alongside the project's existing step-definition packages.

Replace `com.example.steps` and `features` in the following examples with the packages and feature location used by the project.

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

##### JUnit Platform with JUnit 5 or JUnit 6

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

#### 4. Run the Cucumber tests

Run the same Maven phase already used by the project, for example:

```shell
mvn test
```

or, when the Cucumber tests run through Maven Failsafe:

```shell
mvn verify
```

No explicit CTP REST API calls are required in the test code. The integration starts the coverage session, reports each scenario, and publishes the results when the Cucumber run finishes.

The Parasoft hooks start coverage before user-defined setup hooks and stop coverage after user-defined cleanup hooks. This keeps the scenario coverage context available throughout setup, scenario execution, and cleanup. Application requests must still propagate the current `Baggage` header using the Selenium, Playwright, or custom HTTP client approach described below.

For a scenario named `Add a new pet` in `petclinic.feature`, the integration reports these identifiers to CTP:

```text
test=petclinic.feature#Add a new pet
testCase=Add a new pet
```

The scenario name is therefore displayed as the test case name, while the feature file name remains part of the test identifier to help distinguish scenarios from different feature files.

### Headless Selenium execution

Headless mode is configured through Selenium browser options; it is not a `coverage-integration` property. Configure the browser options before passing them to the Selenium coverage integration.

The following example starts Chrome in headless mode while preserving the current Cucumber scenario's coverage baggage:

```java
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.ChromeCoverageConfig;

ChromeOptions options = new ChromeOptions();
options.addArguments("--headless=new");

try (ChromeCoverageConfig coverage =
        SeleniumCoverageIntegration.createChromeBrowserCoverage(options)) {
    WebDriver driver =
            new ChromeDriver(coverage.getChromeOptions());

    try {
        // scenario browser steps
    }
    finally {
        driver.quit();
    }
}
```

The application test project controls whether the browser runs headed or headless by configuring `ChromeOptions` before passing them to `SeleniumCoverageIntegration`. The coverage integration does not define or read a headless system property.

### Optional: DTP session tag

To associate the published results with a DTP session tag, add:

```properties
parasoft.coverage.integration.dtp.sessionTag=cucumber-tests
```

### Multi-user and parallel coverage

The basic Cucumber integration works without multi-user mode or parallel execution. Use the configuration in this section when CTP must distinguish concurrent scenarios for the same test operator and associate browser or HTTP traffic with the correct scenario.

#### Configure multi-user coverage

Configure the test operator ID expected by coverage agents running in multi-user mode:

```properties
parasoft.coverage.integration.ctp.userId=automation-user
```

Do not configure this property when the coverage agents run in single-user mode.

#### Configure parallel execution and coverage correlation

Parallel scenario execution and parallel coverage correlation are separate concerns:

- the Cucumber runner or Maven test plugin schedules tests concurrently;
- `parasoft.coverage.integration.parallel.test.enabled=true` generates a unique `parallelId` for each scenario and sends it to CTP.

After multi-user coverage is configured, a project that already runs Cucumber tests in parallel does not need an additional Cucumber setting; enable the Parasoft parallel-ID property below. Enabling Parasoft parallel IDs does not make sequential tests execute concurrently.

Add this property to `coverage-integration.properties`:

```properties
parasoft.coverage.integration.parallel.test.enabled=true
```

Parallel coverage IDs require multi-user coverage and a configured `parasoft.coverage.integration.ctp.userId`.

Enable concurrency through the configuration used by the project's Cucumber runner.

##### JUnit Platform with JUnit 5 or JUnit 6

Enable Cucumber parallel execution in `src/test/resources/junit-platform.properties`:

```properties
cucumber.execution.parallel.enabled=true
```

A fixed four-thread configuration can be used when a deterministic thread count is required:

```properties
cucumber.execution.parallel.enabled=true
cucumber.execution.parallel.config.strategy=fixed
cucumber.execution.parallel.config.fixed.parallelism=4
cucumber.execution.parallel.config.fixed.max-pool-size=4
```

The basic parallel settings can instead be supplied as Java system properties:

```shell
mvn test \
  -Dparasoft.coverage.integration.parallel.test.enabled=true \
  -Dcucumber.execution.parallel.enabled=true
```

##### JUnit 4

Configure parallel execution through Maven Surefire or Failsafe. With the JUnit 4 Cucumber runner, feature files are scheduled in parallel and the scenarios in one feature file remain on the same thread.

For example:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
        <perCoreThreadCount>false</perCoreThreadCount>
    </configuration>
</plugin>
```

Override the Cucumber data provider and enable its parallel mode:

```java
import org.testng.annotations.DataProvider;

@Override
@DataProvider(parallel = true)
public Object[][] scenarios()
{
    return super.scenarios();
}
```

Each concurrent scenario must use isolated test, browser, and application data. Serialize scenarios that modify the same external resource.

#### Propagate the coverage baggage header

When a test starts with a `userId` and `parallelId`, CTP returns a baggage value with this format:

```text
test-operator-id=<userId>+<parallelId>
```

Requests to the instrumented application must send that value in the HTTP `Baggage` header so the coverage agent can associate execution with the correct scenario.

##### Selenium

Add the Selenium integration dependency:

```xml
<dependency>
    <groupId>com.parasoft</groupId>
    <artifactId>coverage-integration-selenium</artifactId>
    <version>${coverage-integration.version}</version>
    <scope>test</scope>
</dependency>
```

Create a separate browser coverage handle inside each scenario before creating that scenario's browser:

```java
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.ChromeCoverageConfig;

try (ChromeCoverageConfig coverage =
        SeleniumCoverageIntegration.createChromeBrowserCoverage()) {
    WebDriver driver =
            new ChromeDriver(coverage.getChromeOptions());

    try {
        // scenario browser steps
    }
    finally {
        driver.quit();
    }
}
```

Each handle uses a dedicated proxy and captures the baggage for the current scenario. Equivalent handles are available for Edge, Firefox, and Safari. Chrome and Edge can alternatively use `SeleniumCoverageIntegration.configureCdpBaggageHeader(driver)` after driver creation and before navigation.

##### Playwright

Add the Playwright integration dependency:

```xml
<dependency>
    <groupId>com.parasoft</groupId>
    <artifactId>coverage-integration-playwright</artifactId>
    <version>${coverage-integration.version}</version>
    <scope>test</scope>
</dependency>
```

Create each browser context with the current scenario's baggage:

```java
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.parasoft.coverage.integration.playwright.PlaywrightCoverageIntegration;

Browser.NewContextOptions contextOptions =
        PlaywrightCoverageIntegration.createBrowserContextOptions();

BrowserContext context =
        browser.newContext(contextOptions);
```

Create a separate browser context for each concurrent scenario.

##### Custom HTTP clients

For a custom HTTP client, add the API dependency:

```xml
<dependency>
    <groupId>com.parasoft</groupId>
    <artifactId>coverage-integration-api</artifactId>
    <version>${coverage-integration.version}</version>
    <scope>test</scope>
</dependency>
```

Then retrieve the current scenario's baggage value:

```java
import com.parasoft.coverage.integration.api.CoverageIntegration;

String baggageHeader =
        CoverageIntegration.getBaggageHeader();
```

When the returned value is not `null`, send it as the HTTP `Baggage` header on requests to the application under test.

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
