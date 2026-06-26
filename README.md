# Coverage Integration

Coverage Integration reports JUnit test execution events to Parasoft CTP.

## API

User tests should compile against the `coverage-integration-api` module and import classes from `com.parasoft.coverage.integration.api`. The core module is internal.

Use `CoverageIntegration#getCurrentTestOperatorIdHeader()` to get the `Baggage` header value that contains the current `test-operator-id` returned by the CTP `/test/start` API.

For rare standalone use cases, such as tests launched from a `main` method, use `CoverageApiClient` from the API module to start and stop sessions and tests directly.

## Javadoc

The API module generates Javadoc as part of the Maven `package` phase:

```shell
mvn -pl coverage-integration-api -am package
```

The generated documentation is written to `coverage-integration-api/target/reports/apidocs`. The GitHub Actions workflow in `.github/workflows/publish-javadoc.yml` publishes that Javadoc to GitHub Pages when changes are pushed to `master`, or when the workflow is run manually.

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
