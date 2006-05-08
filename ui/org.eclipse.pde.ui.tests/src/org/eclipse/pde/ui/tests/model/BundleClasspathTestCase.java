package org.eclipse.pde.ui.tests.model;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.osgi.framework.Constants;

public class BundleClasspathTestCase extends MultiLineHeaderTestCase {
	
	public static Test suite() {
		return new TestSuite(BundleClasspathTestCase.class);
	}
	
	public BundleClasspathTestCase() {
		super(Constants.BUNDLE_CLASSPATH);
	}
}
