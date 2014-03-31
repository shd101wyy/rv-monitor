package com.runtimeverification.rvmonitor.examples.llvmmop.safety_critical;

import com.runtimeverification.rvmonitor.examples.llvmmop.TestHelper;

import org.junit.Before;

/**
 * Test class for the llvmmop/safety_critical/auto_headlight example
 * @author TraianSF
 */
public class AutoHeadlightsTest extends TestHelper {
    @Override
    @Before
    public void setUp() throws Exception {
        specName = "AutoHeadlights";
        specPath = "examples/llvmmop/safety_critical/auto_headlights/auto_headlights.rvm";
        super.setUp();
    }


}
