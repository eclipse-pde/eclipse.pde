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

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ TargetEnvironmentTestCase.class, //
		TargetPlatformHelperTests.class, //
		LocalTargetDefinitionTests.class, //
		WorkspaceTargetDefinitionTests.class, //
		TargetDefinitionPersistenceTests.class, //
		TargetDefinitionResolutionTests.class, //
		TargetDefinitionFeatureResolutionTests.class, //
		IUBundleContainerTests.class, //
		ProfileContainerTests.class })
public class AllTargetTests {

}
