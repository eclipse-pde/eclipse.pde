/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.xml;

import org.eclipse.pde.core.tests.internal.feature.FeatureDataTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ BasicXMLTestCase.class,
	/*
	 * Commented out till XMLTextChangeListener#getTextOperations() gets fixed /
	 * updated (see what InputContext does)
	 */
	// suite.addTest(SwapXMLModelTestCase.suite());
	StructureXMLModelTestCase.class, ExtensionAttributeTestCase.class, ExtensionElementTestCase.class,
	ManifestEditorSpellCheckTestCase.class, FeatureDataTestCase.class })
public class AllXMLModelTests {
}
