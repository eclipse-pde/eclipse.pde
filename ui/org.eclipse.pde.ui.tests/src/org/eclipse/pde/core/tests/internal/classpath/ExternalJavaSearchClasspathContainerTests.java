/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.classpath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportWizard;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

/**
 * Test that the External Plug-in Libraries project doesn't find duplicated
 * types, when a project is imported into the workspace.
 */
public class ExternalJavaSearchClasspathContainerTests {

	public static final String ADD_PLUGINS_TO_SEARCH_COMMAND_ID = "org.eclipse.pde.ui.addAllPluginsToJavaSearch";
	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;
	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;
	@Rule
	public final TestName name = new TestName();

	@Test
	public void testSearchWithImportedProject() throws Exception {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IHandlerService handlerService = window.getService(IHandlerService.class);
		handlerService.executeCommand(ADD_PLUGINS_TO_SEARCH_COMMAND_ID, null);
		TestUtils.waitForJobs(name.getMethodName(), 100, 10000);
		JavaModelManager.getIndexManager().waitForIndex(false, null);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject proxyProject = root.getProject(SearchablePluginsManager.PROXY_PROJECT_NAME);
		assertNotNull("Adding " + SearchablePluginsManager.PROXY_PROJECT_NAME + " failed", proxyProject);
		IJavaProject javaProject = JavaCore.create(proxyProject);

		String pluginId = "org.eclipse.core.expressions";
		String fqn = "org.eclipse.core.expressions.AndExpression";
		// expect a match like this:
		// .../plugins/org.eclipse.equinox.common_3.20.300.v20251111-0312.jar|org/eclipse/core/runtime/IProgressMonitor.class
		String expected = ".*.jar\\|" + fqn.replace('.', '/') + ".class";

		List<String> matches = performSearch(javaProject, fqn);
		assertSingleMatch(expected, matches);

		IPluginModelBase plugin = PluginRegistry.findModel(pluginId);
		IPluginModelBase[] plugins = { plugin };
		// import as binary so we don't have to compile, compiling will likely fail
		PluginImportWizard.doImportOperation(PluginImportOperation.IMPORT_BINARY, plugins, true, false, null, null);
		TestUtils.waitForJobs(name.getMethodName(), 100, 10000);
		JavaModelManager.getIndexManager().waitForIndex(false, null);
		IProject pluginProject = root.getProject(pluginId);
		assertNotNull("Importing " + pluginId + " failed", pluginProject);

		// expect a match like this:
		// /org.eclipse.core.expressions/org.eclipse.core.expressions_3.9.500.v20250608-0434.jar|org/eclipse/core/expressions/AndExpression.class
		expected = pluginProject.getFullPath() + ".*.jar\\|" + fqn.replace('.', '/') + ".class";
		matches = performSearch(javaProject, fqn);
		assertSingleMatch(expected, matches);
	}

	private static void assertSingleMatch(String regexp, List<String> matches) {
		assertEquals("Expected only one search match, but found: " + matches, 1, matches.size());
		Pattern pattern = Pattern.compile(regexp);
		Matcher matcher = pattern.matcher(matches.get(0));
		assertTrue("Unexpected search matches: " + matches + ", should match regexp: " + regexp, matcher.matches());
	}

	private static List<String> performSearch(IJavaProject javaProject, String fqn) throws JavaModelException {
		TestRequestor requestor = new TestRequestor();
		SearchEngine searchEngine = new SearchEngine();
		searchEngine.searchAllTypeNames(
				fqn.substring(0, fqn.lastIndexOf('.')).toCharArray(),
				SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE,
				fqn.substring(fqn.lastIndexOf('.') + 1).toCharArray(),
				SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE,
				IJavaSearchConstants.TYPE,
				SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject }),
				requestor,
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
				new NullProgressMonitor());
		List<String> matches = requestor.matches;
		return matches;
	}

	private static class TestRequestor extends TypeNameRequestor {

		private final List<String> matches = new ArrayList<>();

		@Override
		public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
			matches.add(path);
		}
	}
}
