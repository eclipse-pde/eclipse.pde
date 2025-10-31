/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
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

import org.eclipse.pde.core.tests.internal.AllPDECoreTests;
import org.eclipse.pde.core.tests.internal.classpath.ClasspathResolutionTest;
import org.eclipse.pde.core.tests.internal.core.builders.BundleErrorReporterTest;
import org.eclipse.pde.core.tests.internal.util.PDESchemaHelperTest;
import org.eclipse.pde.ui.tests.build.properties.AllValidatorTests;
import org.eclipse.pde.ui.tests.classpathcontributor.ClasspathContributorTest;
import org.eclipse.pde.ui.tests.classpathresolver.ClasspathResolverTest;
import org.eclipse.pde.ui.tests.classpathupdater.ClasspathUpdaterTest;
import org.eclipse.pde.ui.tests.ee.ExportBundleTests;
import org.eclipse.pde.ui.tests.imports.AllImportTests;
import org.eclipse.pde.ui.tests.launcher.AllLauncherTests;
import org.eclipse.pde.ui.tests.model.bundle.AllBundleModelTests;
import org.eclipse.pde.ui.tests.model.xml.AllXMLModelTests;
import org.eclipse.pde.ui.tests.nls.AllNLSTests;
import org.eclipse.pde.ui.tests.preferences.AllPreferenceTests;
import org.eclipse.pde.ui.tests.project.BundleRootTests;
import org.eclipse.pde.ui.tests.project.DynamicPluginProjectReferencesTest;
import org.eclipse.pde.ui.tests.project.PluginRegistryTests;
import org.eclipse.pde.ui.tests.project.ProjectCreationTests;
import org.eclipse.pde.ui.tests.runtime.AllPDERuntimeTests;
import org.eclipse.pde.ui.tests.target.AllTargetTests;
import org.eclipse.pde.ui.tests.views.log.AllLogViewTests;
import org.eclipse.pde.ui.tests.wizards.AllNewProjectTests;
import org.eclipse.ui.tests.smartimport.ProjectSmartImportTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ //
	AllTargetTests.class, //
	AllNewProjectTests.class, //
	AllPreferenceTests.class, //
	AllImportTests.class, //
	AllBundleModelTests.class, //
	AllXMLModelTests.class, //
	AllValidatorTests.class, //
	AllNLSTests.class, //
	AllPDERuntimeTests.class, //
	ExportBundleTests.class, //
	AllLauncherTests.class, //
	AllLogViewTests.class, //
	ProjectCreationTests.class, //
	BundleRootTests.class, //
	PluginRegistryTests.class, //
	ClasspathResolverTest.class, //
	ClasspathUpdaterTest.class, //
	PDESchemaHelperTest.class, //
	ClasspathContributorTest.class, //
	DynamicPluginProjectReferencesTest.class, //
	ClasspathResolutionTest.class, //
	BundleErrorReporterTest.class, //
	AllPDECoreTests.class, //
	ProjectSmartImportTest.class, //
})
public class AllPDETests {

}
