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
package org.eclipse.pde.ui.tests.refactoring;

import static org.eclipse.pde.ui.tests.util.ExtensionSchemaTestUtils.createClientsSchema;
import static org.eclipse.pde.ui.tests.util.ProjectUtils.createFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.refactoring.PluginManifestChange;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

/**
 * Tests that renaming a type or resource updates the attributes of extensions
 * that the schema of the extension point declares to reference it.
 */
public class PluginManifestChangeTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	private static final String SCHEMA_FILE = "schema/clients.exsd";

	@Test
	public void testRenameTypeUpdatesTypeAttributes() throws Exception {
		IProject project = createExtensionPointProject("rename.type", """
				<client class="rename.type.pkg.Sample" label="rename.type.pkg.Sample"/>
				<client>
				   <class class="rename.type.pkg.Sample">
				      <parameter name="key" value="value"/>
				   </class>
				</client>
				""");
		IType type = createType(project, "rename.type.pkg", "Sample", "public class Sample {}");

		String renamed = renamePluginXml(project, type, "rename.type.pkg.Renamed");
		assertEquals("The type must be renamed in the type attribute and in the declared element giving it, but not "
				+ "in the plain string attribute", createPluginXml("rename.type", """
						<client class="rename.type.pkg.Renamed" label="rename.type.pkg.Sample"/>
						<client>
						   <class class="rename.type.pkg.Renamed">
						      <parameter name="key" value="value"/>
						   </class>
						</client>
						"""), renamed);
	}

	@Test
	public void testRenameTypeUpdatesTypeAttributeGivenAsUndeclaredElement() throws Exception {
		IProject project = createExtensionPointProject("rename.element", """
				<client>
				   <factory class="rename.element.pkg.Sample"/>
				</client>
				""");
		IType type = createType(project, "rename.element.pkg", "Sample", "public class Sample {}");

		String renamed = renamePluginXml(project, type, "rename.element.pkg.Renamed");
		assertEquals("The type must be renamed in the element giving a type attribute of its parent",
				createPluginXml("rename.element", """
						<client>
						   <factory class="rename.element.pkg.Renamed"/>
						</client>
						"""), renamed);
	}

	@Test
	public void testRenameTypeUpdatesReferencesToItsMemberTypes() throws Exception {
		IProject project = createExtensionPointProject("rename.member", """
				<client class="rename.member.pkg.Sample$Inner"/>
				""");
		IType type = createType(project, "rename.member.pkg", "Sample", """
				public class Sample {
					public static class Inner {
					}
				}
				""");

		String renamed = renamePluginXml(project, type, "rename.member.pkg.Renamed");
		assertEquals("The enclosing type must be renamed in the reference to its member type",
				createPluginXml("rename.member", """
						<client class="rename.member.pkg.Renamed$Inner"/>
						"""), renamed);
	}

	@Test
	public void testRenamePackageUpdatesTypeAttributes() throws Exception {
		IProject project = createExtensionPointProject("rename.pkg", """
				<client class="rename.pkg.old.Sample" label="rename.pkg.old.Sample"/>
				""");
		IType type = createType(project, "rename.pkg.old", "Sample", "public class Sample {}");

		String renamed = renamePluginXml(project, type.getPackageFragment(), "rename.pkg.renamed");
		assertEquals("The package must be renamed in the type attribute, but not in the plain string attribute",
				createPluginXml("rename.pkg", """
						<client class="rename.pkg.renamed.Sample" label="rename.pkg.old.Sample"/>
						"""), renamed);
	}

	@Test
	public void testRenameResourceUpdatesResourceAttributes() throws Exception {
		IProject project = createExtensionPointProject("rename.resource", """
				<client icon="icons/sample.svg" label="icons/sample.svg"/>
				""");
		IFile icon = createFile(project, IPath.fromOSString("icons/sample.svg"),
				"<svg xmlns=\"http://www.w3.org/2000/svg\"/>");
		waitForWorkspaceUpdate(project);

		String renamed = renamePluginXml(project, icon, "icons/renamed.svg");
		assertEquals("The resource must be renamed in the resource attribute, but not in the plain string attribute",
				createPluginXml("rename.resource", """
						<client icon="icons/renamed.svg" label="icons/sample.svg"/>
						"""), renamed);
	}

	private static String renamePluginXml(IProject project, Object element, String newName) throws CoreException {
		Change change = PluginManifestChange.createRenameChange(project.getFile("plugin.xml"),
				new Object[] { element }, new String[] { newName }, null, new NullProgressMonitor());
		assertNotNull("A change of the plugin.xml must be created", change);
		return ((TextChange) change).getPreviewContent(new NullProgressMonitor());
	}

	/**
	 * Creates a plug-in project that declares an extension point with a schema
	 * and contributes the given elements to it. The project is a singleton so
	 * that its extension point, and thus its schema, can be resolved.
	 */
	private static IProject createExtensionPointProject(String symbolicName, String extensionContent)
			throws CoreException {
		IProject project = ProjectUtils.createPluginProject(symbolicName, symbolicName, null,
				(description, service) -> {
					description.setSingleton(true);
					description.setNatureIds(
							new String[] { IBundleProjectDescription.PLUGIN_NATURE, JavaCore.NATURE_ID });
					IBundleClasspathEntry classpathEntry = service.newBundleClasspathEntry(IPath.fromOSString("src"),
							null, IPath.fromOSString("."));
					description.setBundleClasspath(new IBundleClasspathEntry[] { classpathEntry });
				});
		createFile(project, IPath.fromOSString(SCHEMA_FILE), createClientsSchema(symbolicName));
		createFile(project, IPath.fromOSString("plugin.xml"), createPluginXml(symbolicName, extensionContent));
		return project;
	}

	/**
	 * Creates the given type in the source folder of the given project and
	 * returns it once the workspace reflects it.
	 */
	private static IType createType(IProject project, String packageName, String typeName, String body)
			throws CoreException {
		IPath path = IPath.fromOSString("src").append(packageName.replace('.', '/')).append(typeName + ".java");
		createFile(project, path, "package %s;\n\n%s".formatted(packageName, body));
		waitForWorkspaceUpdate(project);
		IType type = JavaCore.create(project).findType(packageName + "." + typeName);
		assertNotNull("The type to rename must be found", type);
		return type;
	}

	/**
	 * Waits until the plug-in model and the Java model reflect the created
	 * project, and checks that the schema of its extension point is found, as
	 * otherwise no attribute of its extensions would be renamed.
	 */
	private static void waitForWorkspaceUpdate(IProject project) {
		TestUtils.waitForJobs(PluginManifestChangeTest.class.getName(), 100, 10000);
		assertNotNull("The schema of the extension point must be found",
				PDECore.getDefault().getSchemaRegistry().getSchema(project.getName() + ".clients"));
	}

	private static String createPluginXml(String symbolicName, String extensionContent) {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<?eclipse version="3.4"?>
				<plugin>
				   <extension-point id="clients" name="Clients" schema="%s"/>
				   <extension point="%s.clients">
				%s   </extension>
				</plugin>
				""".formatted(SCHEMA_FILE, symbolicName, extensionContent.indent(6));
	}

}
