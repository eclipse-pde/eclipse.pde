/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.build.properties;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @since 3.6
 */
public class AllValidatorTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for testing build.properties validations"); //$NON-NLS-1$
		suite.addTest(BuildPropertiesValidationTest.suite());
		return suite;
	}

}
