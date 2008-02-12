package org.eclipse.pde.api.tools.builder.tests;

import java.util.Enumeration;

import junit.extensions.TestDecorator;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

public class BuilderTestSetup extends TestDecorator {

	long complianceLevel;

	public BuilderTestSetup(Test test, long complianceLevel) {
		super(test);
		this.complianceLevel = complianceLevel;
	}

	protected void initTest(Object test) {
		if (test instanceof AbstractBuilderTest) {
			AbstractBuilderTest compilerTest = (AbstractBuilderTest)test;
			compilerTest.initialize(this);
			return;
		}
		if (test instanceof TestSuite) {
			TestSuite testSuite = (TestSuite)test;
			Enumeration evaluationTestClassTests = testSuite.tests();
			while (evaluationTestClassTests.hasMoreElements()) {
				initTest(evaluationTestClassTests.nextElement());
			}
			return;
		}
	}

	public void run(TestResult result) {
		try {
			setUp();
			super.run(result);
		} finally {
			tearDown();
		}
	}

	protected void setUp() {
		// Init wrapped suite
		initTest(fTest);
	}

	protected void tearDown() {
	}
}
