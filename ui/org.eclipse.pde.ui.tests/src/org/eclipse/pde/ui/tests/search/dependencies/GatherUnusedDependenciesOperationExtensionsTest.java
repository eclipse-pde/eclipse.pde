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
 * of a plugin.xml or fragment.xml is not reported as unused, as far as the
 * schema of the extension point declares the referencing attributes to be of
 * the kind java.
 */
public class GatherUnusedDependenciesOperationExtensionsTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	private static final String EXTENSION_POINT_ID = "clients";

	private static final String SCHEMA_FILE = "schema/clients.exsd";

	@Test
	public void testDependencyOnlyReferencedByTypeAttributeInPluginXmlIsNotFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.attribute";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.attribute", bundleProvider, """
				<client class="%s.Sample"/>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertFalse("Dependency providing a type referenced by an attribute that the schema declares to be of the kind "
				+ "java must not be flagged as unused", unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testDependencyOnlyReferencedByTypeElementInPluginXmlIsNotFlaggedAsUnused() throws Exception {
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
		assertFalse("Dependency providing a type referenced by a type attribute given as nested element must not be "
				+ "flagged as unused", unusedPlugins.contains(bundleProvider));
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
	public void testDependencyOnlyReferencedByPlainAttributeInPluginXmlIsFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.plainattribute";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.plainattribute", bundleProvider, """
				<client label="%s.Sample"/>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertTrue("Dependency must be flagged as unused although a value names a type, because the schema declares "
				+ "the attribute holding it to be a plain string", unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testDependencyOnlyReferencedByElementTextInPluginXmlIsFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.text";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.text", bundleProvider, """
				<client>%s.Sample</client>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertTrue("Dependency must be flagged as unused although an element text names a type, because a schema "
				+ "cannot declare an element text to be of the kind java", unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testDependencyOnlyReferencedByTypeOfExtensionPointWithoutSchemaIsFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.noschema";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createJavaPluginProject("consumer.noschema", null, true);
		addRequiredBundle(projectConsumer, bundleProvider);
		createExtensionFile(projectConsumer, "plugin", "plugin.xml", "consumer.noschema", false, """
				<client class="%s.Sample"/>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertTrue("Dependency must be flagged as unused although a value names a type, because without a schema "
				+ "there is no declaration of which attributes hold a type", unusedPlugins.contains(bundleProvider));
	}

	@Test
	public void testDependencyOfFragmentOnlyReferencedByTypeInFragmentXmlIsNotFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.fragment";
		String packageProvider = createProviderBundle(bundleProvider);
		String bundleHost = "host.fragment";
		IProject projectHost = createJavaPluginProject(bundleHost, null, true);
		createExtensionFile(projectHost, "plugin", "plugin.xml", bundleHost, true, null);
		IProject projectFragment = createJavaPluginProject("consumer.fragment", bundleHost);
		addRequiredBundle(projectFragment, bundleProvider);
		createExtensionFile(projectFragment, "fragment", "fragment.xml", bundleHost, false, """
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
	public void testDependencyOnlyReferencedByUnresolvedTypeInPluginXmlIsNotFlaggedAsUnused() throws Exception {
		String bundleProvider = "provider.unresolvedtype";
		String packageProvider = createProviderBundle(bundleProvider);
		IProject projectConsumer = createConsumerBundle("consumer.unresolvedtype", bundleProvider, """
				<client class="%s.NoSuchType"/>
				""".formatted(packageProvider));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectConsumer);
		assertFalse(
				"Dependency must not be flagged as unused although the referenced type does not resolve, because the "
						+ "schema declares the value to be a type",
				unusedPlugins.contains(bundleProvider));
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
	 * contributes the given element to an extension point declared by itself
	 * with a schema. The project contains no Java source, so the required bundle
	 * can only be referenced from the contributed extension.
	 */
	private static IProject createConsumerBundle(String symbolicName, String providerSymbolicName,
			String extensionContent) throws Exception {
		IProject project = createJavaPluginProject(symbolicName, null, true);
		addRequiredBundle(project, providerSymbolicName);
		createExtensionFile(project, "plugin", "plugin.xml", symbolicName, true, extensionContent);
		return project;
	}

	/**
	 * Creates a plug-in project that imports the given package and contributes
	 * the given element to an extension point declared by itself with a schema.
	 * The project contains no Java source, so the imported package can only be
	 * referenced from the contributed extension.
	 */
	private static IProject createImportingConsumerBundle(String symbolicName, String importedPackage,
			String extensionContent) throws Exception {
		IProject project = createJavaPluginProject(symbolicName, null, true);
		addImportedPackage(project, importedPackage);
		createExtensionFile(project, "plugin", "plugin.xml", symbolicName, true, extensionContent);
		return project;
	}

	/**
	 * Creates the given extension file, which declares the extension point if
	 * requested and contributes the given element to it, if any. A declared
	 * extension point comes with a schema that declares the {@code class}
	 * attribute of the contributed element to be of the kind {@code java} and
	 * its {@code label} attribute to be a plain string. The declaring project
	 * has to be a singleton, as only then its extension point and thus the
	 * schema is resolvable.
	 *
	 * @param declaringSymbolicName the symbolic name of the bundle that declares
	 *                              the extension point
	 * @param declareExtensionPoint whether the file declares the extension point
	 *                              with its schema
	 * @param extensionContent      the element to contribute, or {@code null} to
	 *                              contribute none
	 */
	private static void createExtensionFile(IProject project, String rootElement, String fileName,
			String declaringSymbolicName, boolean declareExtensionPoint, String extensionContent)
			throws CoreException {
		String extensionPointDeclaration = "";
		if (declareExtensionPoint) {
			createSchema(project, declaringSymbolicName);
			extensionPointDeclaration = "<extension-point id=\"%s\" name=\"Clients\" schema=\"%s\"/>"
					.formatted(EXTENSION_POINT_ID, SCHEMA_FILE);
		}
		String extension = "";
		if (extensionContent != null) {
			extension = """
					<extension point="%s.%s">
					      %s
					   </extension>""".formatted(declaringSymbolicName, EXTENSION_POINT_ID, extensionContent.strip());
		}
		createFile(project, IPath.fromOSString(fileName), """
				<?xml version="1.0" encoding="UTF-8"?>
				<?eclipse version="3.4"?>
				<%1$s>
				   %2$s
				   %3$s
				</%1$s>
				""".formatted(rootElement, extensionPointDeclaration, extension));
	}

	private static void createSchema(IProject project, String declaringSymbolicName) throws CoreException {
		createFile(project, IPath.fromOSString(SCHEMA_FILE), """
				<?xml version='1.0' encoding='UTF-8'?>
				<schema targetNamespace="%1$s" xmlns="http://www.w3.org/2001/XMLSchema">
				   <annotation>
				      <appinfo>
				         <meta.schema plugin="%1$s" id="%2$s" name="Clients"/>
				      </appinfo>
				   </annotation>
				   <element name="extension">
				      <complexType>
				         <sequence>
				            <element ref="client" minOccurs="0" maxOccurs="unbounded"/>
				         </sequence>
				         <attribute name="point" type="string" use="required"/>
				      </complexType>
				   </element>
				   <element name="client">
				      <complexType>
				         <attribute name="class" type="string">
				            <annotation>
				               <appinfo>
				                  <meta.attribute kind="java"/>
				               </appinfo>
				            </annotation>
				         </attribute>
				         <attribute name="label" type="string"/>
				      </complexType>
				   </element>
				</schema>
				""".formatted(declaringSymbolicName, EXTENSION_POINT_ID));
	}

}
