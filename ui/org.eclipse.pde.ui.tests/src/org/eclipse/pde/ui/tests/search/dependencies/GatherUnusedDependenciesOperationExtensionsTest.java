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
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.addImportedPackage;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.addRequiredBundle;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.buildProjects;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.createFile;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.createJavaPluginProject;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.createJavaSource;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.createManifestOnlyPluginProject;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.gatherUnusedDependencies;
import static org.eclipse.pde.ui.tests.search.dependencies.GatherUnusedDependenciesTestUtils.gatherUnusedPackageImports;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

/**
 * Tests that a dependency whose types are only referenced from the extensions
 * of a plugin.xml or fragment.xml is not reported as unused.
 */
public class GatherUnusedDependenciesOperationExtensionsTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Test
	public void testDependencyOnlyReferencedByTypeInAttributeInPluginXmlIsNotFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.attribute";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.attribute", bundleProvider, """
				<client class="%s.Sample"/>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertFalse("Dependency providing a type referenced by an attribute must not be flagged as unused",
				unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testDependencyOnlyReferencedByTypeInNestedElementInPluginXmlIsNotFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.element";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.element", bundleProvider, """
				<client>
				         <class class="%s.Sample">
				            <parameter name="key" value="value"/>
				         </class>
				      </client>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertFalse("Dependency providing a type referenced by a nested element must not be flagged as unused",
				unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testDependencyNotReferencedByAnyTypeInPluginXmlIsFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.unreferenced";
		createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.unreferenced", bundleProvider, """
				<client label="Some plain label"/>
				""");

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertTrue("Dependency whose types are not referenced at all must be flagged as unused",
				unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testDependencyOnlyReferencedByTypeInElementTextInPluginXmlIsNotFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.text";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.text", bundleProvider, """
				<client>%s.Sample</client>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertFalse("Dependency providing a type referenced as element text must not be flagged as unused",
				unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testDependencyOfFragmentOnlyReferencedByTypeInFragmentXmlIsNotFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.fragment";
		String packageProvider = createProviderBundle(bundleProvider);
		String bundleHost = "host.fragment";
		createManifestOnlyPluginProject(bundleHost);
		IProject projectFragment = createConsumerFragment("consumer.fragment", bundleHost, bundleProvider, """
				<client class="%s.Sample"/>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectFragment);
		assertFalse("Dependency providing a type referenced in a fragment.xml must not be flagged as unused",
				unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testImportedPackageOnlyReferencedByTypeInPluginXmlIsNotFlaggedAsUnused() throws Exception {
		String packageProvider = createProviderBundle("provider.importedused");
		IProject projectConsumer = createImportingConsumerBundle("consumer.importedused", packageProvider, """
				<client class="%s.Sample"/>
				""".formatted(packageProvider));

		buildProjects();
		assertFalse("Imported package providing a referenced type must not be flagged as unused",
				gatherUnusedPackageImports(projectConsumer).contains(packageProvider));
	}

	@Test
	public void testImportedPackageNotReferencedByAnyTypeInPluginXmlIsFlaggedAsUnused() throws Exception {
		String packageProvider = createProviderBundle("provider.importedunused");
		IProject projectConsumer = createImportingConsumerBundle("consumer.importedunused", packageProvider, """
				<client label="Some plain label"/>
				""");

		buildProjects();
		assertTrue("Imported package whose types are not referenced at all must be flagged as unused",
				gatherUnusedPackageImports(projectConsumer).contains(packageProvider));
	}

	@Test
	public void testDependencyOnlyReferencedByNestedTypeInPluginXmlIsNotFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.nestedtype";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.nestedtype", bundleProvider, """
				<client class="%s.Sample$Nested"/>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertFalse("Dependency providing a referenced nested type must not be flagged as unused",
				unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testDependencyOnlyReferencedByTypeWithInitializationDataInPluginXmlIsNotFlaggedAsUnused()
			throws Exception {
		String bundleProvider = "provider.initializationdata";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.initializationdata", bundleProvider, """
				<client class="%s.Sample:some initialization data"/>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertFalse(
				"Dependency providing a referenced type must not be flagged as unused although the reference carries "
						+ "initialization data",
				unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testDependencyOnlyReferencedByValueNamingNoTypeInPluginXmlIsFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.notype";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.notype", bundleProvider, """
				<client class="%s.NoSuchType"/>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertTrue("Dependency must be flagged as unused although a value has the shape of a type name, because it "
				+ "does not name a type of the class path", unusedPlugins.contains(bundleProvider));
	}

	/**
	 * Creates a plug-in project exporting a package that contains a type
	 * {@code Sample} and returns the name of that package.
	 */
	private static String createProviderBundle(String symbolicName) throws Exception {
		String packageName = symbolicName + ".pkg";
		IProject project = createJavaPluginProject(symbolicName);
		addExportedPackage(project, packageName);
		createJavaSource(project, packageName, "Sample", """
				public class Sample {
					public static class Nested {
					}
				}
				""");
		return packageName;
	}

	/**
	 * Creates a plug-in project that requires the given provider bundle and
	 * contributes the given element to an extension point declared by itself.
	 * The project contains no Java source, so the required bundle can only be
	 * referenced from the contributed extension.
	 */
	private static IProject createConsumerBundle(String symbolicName, String providerSymbolicName,
			String extensionContent) throws Exception {
		IProject project = createJavaPluginProject(symbolicName);
		addRequiredBundle(project, providerSymbolicName);
		createExtensionFile(project, "plugin", "plugin.xml", symbolicName, extensionContent);
		return project;
	}

	/**
	 * Creates a plug-in project that imports the given package and contributes
	 * the given element to an extension point declared by itself. The project
	 * contains no Java source, so the imported package can only be referenced
	 * from the contributed extension.
	 */
	private static IProject createImportingConsumerBundle(String symbolicName, String importedPackage,
			String extensionContent) throws Exception {
		IProject project = createJavaPluginProject(symbolicName);
		addImportedPackage(project, importedPackage);
		createExtensionFile(project, "plugin", "plugin.xml", symbolicName, extensionContent);
		return project;
	}

	/**
	 * Creates a fragment project for the given host that requires the given
	 * provider bundle and contributes the given element to an extension point
	 * declared by its host. The project contains no Java source, so the
	 * required bundle can only be referenced from the contributed extension.
	 */
	private static IProject createConsumerFragment(String symbolicName, String hostSymbolicName,
			String providerSymbolicName, String extensionContent) throws Exception {
		IProject project = createJavaPluginProject(symbolicName, hostSymbolicName);
		addRequiredBundle(project, providerSymbolicName);
		createExtensionFile(project, "fragment", "fragment.xml", hostSymbolicName, extensionContent);
		return project;
	}

	private static void createExtensionFile(IProject project, String rootElement, String fileName,
			String declaringSymbolicName, String extensionContent) throws CoreException {
		String extensionPointId = "clients";
		createFile(project, IPath.fromOSString(fileName), """
				<?xml version="1.0" encoding="UTF-8"?>
				<?eclipse version="3.4"?>
				<%1$s>
				   <extension-point id="%3$s" name="Clients"/>
				   <extension point="%2$s.%3$s">
				      %4$s
				   </extension>
				</%1$s>
				""".formatted(rootElement, declaringSymbolicName, extensionPointId, extensionContent.strip()));
	}

}
