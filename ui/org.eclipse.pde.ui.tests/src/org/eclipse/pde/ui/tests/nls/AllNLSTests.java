/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 225047     
 *******************************************************************************/
package org.eclipse.pde.ui.tests.nls;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @since 3.4
 */
public class AllNLSTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for testing NLS"); //$NON-NLS-1$
		suite.addTest(StringHelperTestCase.suite());
		return suite;
	}

}
