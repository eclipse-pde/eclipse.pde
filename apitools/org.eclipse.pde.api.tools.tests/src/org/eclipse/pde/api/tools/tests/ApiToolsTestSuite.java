/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.tests;


import org.eclipse.pde.api.tools.builder.tests.OSGiLessAnalysisTests;
import org.eclipse.pde.api.tools.comparator.tests.AllDeltaTests;
import org.eclipse.pde.api.tools.model.tests.ApiBaselineTests;
import org.eclipse.pde.api.tools.model.tests.ApiDescriptionTests;
import org.eclipse.pde.api.tools.model.tests.ApiModelCacheTests;
import org.eclipse.pde.api.tools.model.tests.ApiTypeContainerTests;
import org.eclipse.pde.api.tools.model.tests.BadClassfileTests;
import org.eclipse.pde.api.tools.model.tests.CRCTests;
import org.eclipse.pde.api.tools.model.tests.ClassFileScannerTests;
import org.eclipse.pde.api.tools.model.tests.ComponentManifestTests;
import org.eclipse.pde.api.tools.model.tests.ElementDescriptorTests;
import org.eclipse.pde.api.tools.model.tests.Java8ClassfileScannerTests;
import org.eclipse.pde.api.tools.model.tests.TagScannerTests;
import org.eclipse.pde.api.tools.problems.tests.ApiFilterTests;
import org.eclipse.pde.api.tools.problems.tests.ApiProblemFactoryTests;
import org.eclipse.pde.api.tools.problems.tests.ApiProblemTests;
import org.eclipse.pde.api.tools.reference.tests.SearchScopeTests;
import org.eclipse.pde.api.tools.search.tests.SearchEngineTests;
import org.eclipse.pde.api.tools.search.tests.SkippedComponentTests;
import org.eclipse.pde.api.tools.search.tests.UseSearchTests;
import org.eclipse.pde.api.tools.util.tests.HeadlessApiBaselineManagerTests;
import org.eclipse.pde.api.tools.util.tests.SignaturesTests;
import org.eclipse.pde.api.tools.util.tests.TarEntryTests;
import org.eclipse.pde.api.tools.util.tests.TarExceptionTests;
import org.eclipse.pde.api.tools.util.tests.UtilTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for all of the API tools test
 *
 * The API Baseline Tests should be run as JUnit tests, not JUnit Plug-in Tests.
 * This means that there is no OSGi environment available. The vm argument
 * requiredBundles must be set to a valid baseline. In addition, rather than use
 * the EE profiles provided by OSGi, the baseline will resolve using EEs found
 * in the org.eclipse.pde.api.tools.internal.util.profiles inside the
 * org.eclipse.pde.api.tools bundle. "-DrequiredBundles=${eclipse_home}/plugins"
 *
 * @since 1.0.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		ApiDescriptionTests.class, SearchEngineTests.class, SkippedComponentTests.class, UseSearchTests.class,
		HeadlessApiBaselineManagerTests.class, TagScannerTests.class, ComponentManifestTests.class, UtilTests.class,
		SignaturesTests.class, ApiBaselineTests.class, ApiTypeContainerTests.class, ClassFileScannerTests.class,
		Java8ClassfileScannerTests.class, ElementDescriptorTests.class, SearchScopeTests.class, ApiProblemTests.class,
		ApiProblemFactoryTests.class, ApiFilterTests.class, TarEntryTests.class, TarExceptionTests.class,
		OSGiLessAnalysisTests.class, ApiModelCacheTests.class, BadClassfileTests.class,
	CRCTests.class,
	AllDeltaTests.class
})
public class ApiToolsTestSuite {
}
