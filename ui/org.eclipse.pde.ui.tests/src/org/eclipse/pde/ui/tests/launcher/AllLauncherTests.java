/*******************************************************************************
 * Copyright (c) 2009, 2021 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import org.eclipse.pde.ui.tests.launcher.product.ProductEditorLaunchingTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @since 3.5
 */
@RunWith(Suite.class)
@SuiteClasses({ //
	FeatureBasedLaunchTest.class, //
	PluginBasedLaunchTest.class, //
	LaunchConfigurationHelperTestCase.class, //
	LaunchConfigurationMigrationTest.class, //
	ProductEditorLaunchingTest.class, //
})
public class AllLauncherTests {

}
