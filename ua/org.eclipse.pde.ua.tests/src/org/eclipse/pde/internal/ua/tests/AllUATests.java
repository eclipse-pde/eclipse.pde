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
package org.eclipse.pde.internal.ua.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.internal.ua.tests.cheatsheet.AllCheatSheetModelTests;
import org.eclipse.pde.internal.ua.tests.ctxhelp.AllCtxHelpModelTests;
import org.eclipse.pde.internal.ua.tests.toc.AllTocModelTests;

public class AllUATests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for testing all ua related models"); //$NON-NLS-1$
		suite.addTest(AllCheatSheetModelTests.suite());
		suite.addTest(AllTocModelTests.suite());
		suite.addTest(AllCtxHelpModelTests.suite());
		return suite;
	}

}
