/*******************************************************************************
 *  Copyright (c) 2005, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.xml;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllXMLModelTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for testing the xml model"); //$NON-NLS-1$
		suite.addTest(BasicXMLTestCase.suite());
		/* 
		 * Commented out till XMLTextChangeListener#getTextOperations() 
		 * gets fixed / updated (see what InputContext does)
		 */
		//		suite.addTest(SwapXMLModelTestCase.suite());
		suite.addTest(StructureXMLModelTestCase.suite());
		suite.addTest(ExtensionAttributeTestCase.suite());
		suite.addTest(ExtensionElementTestCase.suite());
		suite.addTest(ManifestEditorSpellCheckTestCase.suite());
		return suite;
	}

}
