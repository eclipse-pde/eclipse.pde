package org.eclipse.pde.ui.tests.performance;

import org.eclipse.pde.ui.tests.performance.parts.*;

import junit.framework.*;

public class PDEPerformanceTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Performance Test Suite for org.eclipse.pde.ui"); //$NON-NLS-1$
		suite.addTest(PluginsViewPerfTest.suite());
		suite.addTest(OpenManifestEditorPerfTest.suite());
		suite.addTest(SchemaPerfTest.suite());
		suite.addTest(InitializeModelsPerfTest.suite());
		return suite;
	}

}
