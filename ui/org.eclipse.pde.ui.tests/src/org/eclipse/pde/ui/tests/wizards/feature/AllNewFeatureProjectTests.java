/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.wizards.feature;

import junit.framework.*;

public class AllNewFeatureProjectTests {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite to test new feature project creation wizards."); //$NON-NLS-1$
		suite.addTest(SimpleFeatureTestCase.suite());
		suite.addTest(FeatureWithPluginTestCase.suite());
		return suite;
	}

}
