package com.parasoft.coverage.integration.testng;

import org.testng.annotations.Test;

// Keep fixtures outside the test class so TestNG does not discover them again
// as nested tests when Surefire runs the enclosing lifecycle test.
final class SuiteFixtures
{
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
}
