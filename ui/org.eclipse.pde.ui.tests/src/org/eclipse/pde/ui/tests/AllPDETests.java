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
import org.eclipse.pde.ui.tests.classpathcontributor.ClasspathContributorTest;
import org.eclipse.pde.ui.tests.classpathresolver.ClasspathResolverTest;
import org.eclipse.pde.ui.tests.ee.ExportBundleTests;
import org.eclipse.pde.ui.tests.imports.AllImportTests;
import org.eclipse.pde.ui.tests.launcher.AllLauncherTests;
import org.eclipse.pde.ui.tests.model.bundle.AllBundleModelTests;
import org.eclipse.pde.ui.tests.model.xml.AllXMLModelTests;
import org.eclipse.pde.ui.tests.nls.AllNLSTests;
import org.eclipse.pde.ui.tests.preferences.AllPreferenceTests;
import org.eclipse.pde.ui.tests.project.*;
import org.eclipse.pde.ui.tests.runtime.AllPDERuntimeTests;
import org.eclipse.pde.ui.tests.target.AllTargetTests;
import org.eclipse.pde.ui.tests.views.log.AllLogViewTests;
import org.eclipse.pde.ui.tests.wizards.AllNewProjectTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	AllTargetTests.class,
	AllNewProjectTests.class,
	AllPreferenceTests.class,
	AllImportTests.class,
	AllBundleModelTests.class,
	AllXMLModelTests.class,
	AllValidatorTests.class,
	AllNLSTests.class,
	AllPDERuntimeTests.class,
	ExportBundleTests.class,
	AllLauncherTests.class,
	AllLogViewTests.class,
	ProjectCreationTests.class,
	BundleRootTests.class,
	PluginRegistryTests.class,
	ClasspathResolverTest.class,
	ClasspathContributorTest.class
})
public class AllPDETests {

}
