package org.eclipse.pde.ui.tests;

import org.eclipse.pde.ui.tests.imports.*;

import junit.framework.*;


public class AllPDETests extends TestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for org.eclipse.pde.ui"); //$NON-NLS-1$
		suite.addTest(PluginImportTest.suite());
		return suite;
	}
}
