/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests;

import org.eclipse.pde.ui.tests.build.properties.AllValidatorTests;
import org.eclipse.pde.ui.tests.classpathresolver.ClasspathResolverTest;
import org.eclipse.pde.ui.tests.launcher.AllLauncherTests;
import org.eclipse.pde.ui.tests.model.bundle.AllBundleModelTests;
import org.eclipse.pde.ui.tests.model.xml.AllXMLModelTests;
import org.eclipse.pde.ui.tests.nls.AllNLSTests;
import org.eclipse.pde.ui.tests.preferences.AllPreferenceTests;
import org.eclipse.pde.ui.tests.project.*;
import org.eclipse.pde.ui.tests.runtime.AllPDERuntimeTests;
import org.eclipse.pde.ui.tests.target.AllTargetMinimalTests;
import org.eclipse.pde.ui.tests.views.log.AllLogViewTests;
import org.eclipse.pde.ui.tests.wizards.AllNewProjectMinimalTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * They run on minimal plugin bundles and dont require the whole SDK ( for
 * hudson gerrit). This class is refactored out of AllPDETests
 *
 */

@RunWith(Suite.class)
@SuiteClasses({ AllTargetMinimalTests.class, AllNewProjectMinimalTests.class, AllPreferenceTests.class,
	// AllImportTests.class,
	AllBundleModelTests.class, AllXMLModelTests.class, AllValidatorTests.class, AllNLSTests.class,
	AllPDERuntimeTests.class,
	// ExportBundleTests.class,
	AllLauncherTests.class, AllLogViewTests.class, ProjectCreationTests.class, BundleRootTests.class,
	PluginRegistryTestsMinimal.class, ClasspathResolverTest.class
	// ClasspathContributorTest.class
})
public class AllPDEMinimalTests {

}
