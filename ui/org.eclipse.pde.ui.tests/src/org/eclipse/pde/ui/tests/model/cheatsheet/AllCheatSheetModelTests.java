/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.cheatsheet;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllCheatSheetModelTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for testing the cheatsheet model"); //$NON-NLS-1$
		suite.addTestSuite(SimpleCSIntroTestCase.class);
		suite.addTestSuite(SimpleCSItemTestCase.class);
		suite.addTestSuite(SimpleCSSubItemTestCase.class);
		suite.addTestSuite(SimpleCSItemAPITestCase.class);
		suite.addTestSuite(SimpleCSSubItemAPITestCase.class);
		return suite;
	}

}
