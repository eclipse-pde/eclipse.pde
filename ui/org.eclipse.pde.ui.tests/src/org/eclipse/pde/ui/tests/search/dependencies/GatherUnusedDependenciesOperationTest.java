/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.ui.tests.search.dependencies;

import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.addExportedPackage;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.addReexportedBundle;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.addRequiredBundle;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.buildProjects;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.createJavaPluginProject;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.createJavaSource;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.createManifestOnlyPluginProject;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.gatherUnusedDependencies;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class GatherUnusedDependenciesOperationTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Test
	public void testDirectlyUsedDependencyReexportedByOtherDependencyIsNotFlaggedAsUnused() throws Exception {
		// Bundle C: exports a package that is used directly by bundle B
		String bundleC = "bundle.c";
		String packageC = bundleC + ".pkg";
		IProject projectC = createJavaPluginProject(bundleC);
		addExportedPackage(projectC, packageC);
		createJavaSource(projectC, packageC, "C", """
				public class C {
				}
				""");

		// Bundle A: requires and reexports C, but is otherwise unrelated to B
		String bundleA = "bundle.a";
		IProject projectA = createManifestOnlyPluginProject(bundleA);
		addReexportedBundle(projectA, bundleC);

		// Bundle B: directly requires both A and C, and directly uses C's API
		String bundleB = "bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		addRequiredBundle(projectB, bundleA);
		addRequiredBundle(projectB, bundleC);
		createJavaSource(projectB, bundleB, "UsesC", """
				public class UsesC {
					%s.C field;
				}
				""".formatted(packageC));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectB);
		assertFalse(
				"Direct, used dependency to bundle C must not be flagged as unused just because bundle A reexports it",
				unusedPlugins.contains(bundleC));
	}

}
