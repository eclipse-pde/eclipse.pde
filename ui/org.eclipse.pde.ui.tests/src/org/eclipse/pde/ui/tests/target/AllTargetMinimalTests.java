/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * They run on minimal plugin bundles and dont require the whole SDK ( for
 * hudson gerrit). This class is refactored out of AllTargetTests
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
	TargetEnvironmentTestCase.class, TargetPlatformHelperTests.class,
	// LocalTargetDefinitionTests.class,
	// WorkspaceTargetDefinitionTests.class
	MinimalTargetDefinitionPersistenceTests.class, MinimalTargetDefinitionResolutionTests.class,
	MinimalTargetDefinitionFeatureResolutionTests.class, IUBundleContainerTests.class
})
public class AllTargetMinimalTests {
}
