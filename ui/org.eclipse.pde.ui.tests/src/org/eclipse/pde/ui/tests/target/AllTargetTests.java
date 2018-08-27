/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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

@RunWith(Suite.class)
@SuiteClasses({ TargetEnvironmentTestCase.class, TargetPlatformHelperTests.class, LocalTargetDefinitionTests.class,
	WorkspaceTargetDefinitionTests.class, TargetDefinitionPersistenceTests.class,
	TargetDefinitionResolutionTests.class, TargetDefinitionFeatureResolutionTests.class,
	IUBundleContainerTests.class })
public class AllTargetTests {

}
