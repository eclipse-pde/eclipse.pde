/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.runtime;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllPDERuntimeTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite to test the pde.runtime plug-in."); //$NON-NLS-1$
		suite.addTest(LocalModelTest.suite());
		return suite;
	}

}
