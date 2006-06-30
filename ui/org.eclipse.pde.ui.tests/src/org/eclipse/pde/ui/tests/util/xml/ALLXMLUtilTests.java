package org.eclipse.pde.ui.tests.util.xml;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ALLXMLUtilTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("XML Utilities Test Suite"); //$NON-NLS-1$
		suite.addTest(ParserWrapperTestCase.suite());
		return suite;
	}	
	
}
