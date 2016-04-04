/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * They run on minimal plugin bundles and dont require the whole SDK ( for
 * hudson gerrit). This class is refactored out of AllTargetTests
 *
 */
public class AllTargetMinimalTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for testing targets"); //$NON-NLS-1$
		suite.addTest(TargetEnvironmentTestCase.suite());
		suite.addTest(TargetPlatformHelperTests.suite());
		// suite.addTest(LocalTargetDefinitionTests.suite());
		// suite.addTest(WorkspaceTargetDefinitionTests.suite());
		suite.addTest(MinimalTargetDefinitionPersistenceTests.suite());
		suite.addTest(MinimalTargetDefinitionResolutionTests.suite());
		suite.addTest(MinimalTargetDefinitionFeatureResolutionTests.suite());
		suite.addTest(IUBundleContainerTests.suite());
		return suite;
	}

}
