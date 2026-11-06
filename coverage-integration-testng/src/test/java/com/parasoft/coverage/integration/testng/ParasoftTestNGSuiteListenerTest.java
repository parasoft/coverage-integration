package com.parasoft.coverage.integration.testng;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import com.parasoft.coverage.integration.core.CoverageApiClient;
import com.parasoft.coverage.integration.core.CoverageTestContext;
import com.parasoft.coverage.integration.core.model.AgentTestStopModelV3.ResultEnum;
import com.parasoft.coverage.integration.core.model.CoverageUploadRequestModelV3.AnalysisTypeEnum;

public class ParasoftTestNGSuiteListenerTest
{
    private static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    @Test
    public void startsOneSessionForParallelClassesAcrossSuites()
    {
        EVENTS.clear();
        XmlSuite firstSuite = suite("first", FirstClass.class, SecondClass.class);
        XmlSuite secondSuite = suite("second", ThirdClass.class);

        TestNG runner = new TestNG();
        runner.setUseDefaultListeners(false);
        runner.setVerbose(0);
        runner.setXmlSuites(List.of(firstSuite, secondSuite));
        runner.run();

        assertFalse(runner.hasFailure());
        assertEquals(EVENTS, List.of("start", "stop", "publish:session-1"));
    }

    private static XmlSuite suite(String name, Class<?>... classes)
    {
        XmlSuite suite = new XmlSuite();
        suite.setName(name);
        suite.setParallel(XmlSuite.ParallelMode.CLASSES);
        suite.setThreadCount(3);
        suite.addListener(RecordingListener.class.getName());
        XmlTest test = new XmlTest(suite);
        test.setName(name + "Test");
        test.setXmlClasses(java.util.Arrays.stream(classes).map(XmlClass::new).toList());
        return suite;
    }

    public static class RecordingListener extends ParasoftTestNGSuiteListener
    {
        public RecordingListener()
        {
            super(new RecordingClient());
        }
    }

    public static class FirstClass
    {
        @Test
        public void runs()
        {
        }
    }

    public static class SecondClass
    {
        @Test
        public void runs()
        {
        }
    }

    public static class ThirdClass
    {
        @Test
        public void runs()
        {
        }
    }

    private static class RecordingClient implements CoverageApiClient
    {
        @Override
        public String startSession()
        {
            EVENTS.add("start");
            return "session-1";
        }

        @Override
        public CoverageTestContext startTest(String test, String testCase)
        {
            return null;
        }

        @Override
        public void stopTest(String test, String testCase, CoverageTestContext context, ResultEnum result, String message)
        {
        }

        @Override
        public void stopSession()
        {
            EVENTS.add("stop");
        }

        @Override
        public void publishResults(String sessionId, String testConfig, String userId, String toolName,
                AnalysisTypeEnum analysisType)
        {
            EVENTS.add("publish:" + sessionId);
        }
    }
}