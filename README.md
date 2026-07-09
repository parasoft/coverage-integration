# Coverage Integration

Coverage Integration reports JUnit test execution events to Parasoft CTP.

## API

User tests should compile against the `coverage-integration-api` module and import classes from `com.parasoft.coverage.integration.api`. The core module is internal.

Use `CoverageIntegration#getCurrentTestOperatorIdHeader()` to get the `Baggage` header value that contains the current `test-operator-id` returned by the CTP `/test/start` API.

For rare standalone use cases, such as tests launched from a `main` method, use `CoverageApiClient` from the API module to start and stop sessions and tests directly.

## Playwright

Add the Playwright integration dependency alongside the JUnit integration dependency used by the test project.

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

## Selenium

Add the Selenium integration dependency alongside the JUnit integration dependency used by the test project.

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
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.SeleniumBrowserCoverage;

try (SeleniumBrowserCoverage coverage = SeleniumCoverageIntegration.createChromeBrowserCoverage()) {
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
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.FirefoxBrowserCoverage;

try (FirefoxBrowserCoverage coverage = SeleniumCoverageIntegration.createFirefoxBrowserCoverage()) {
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
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration.SafariBrowserCoverage;

try (SafariBrowserCoverage coverage = SeleniumCoverageIntegration.createSafariBrowserCoverage()) {
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

## Coverage properties file

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
```
Place this file on your project's classpath, for example in src/test/resources.

## JUnit 5 and 6
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


## JUnit 4

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
