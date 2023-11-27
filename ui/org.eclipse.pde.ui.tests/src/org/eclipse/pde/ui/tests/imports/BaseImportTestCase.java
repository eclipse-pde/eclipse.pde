/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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
 *     Hannes Wellmann - Combine BaseImportTestCase subclasses into a parameterized test and make it succeed in verifiation builds
 *******************************************************************************/
package org.eclipse.pde.ui.tests.imports;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.BinaryRepositoryProvider;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.team.core.RepositoryProvider;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

@RunWith(Parameterized.class)
public class BaseImportTestCase extends PDETestCase {

	@Parameters(name = "{0}")
	public static Object[][] importTypes() {
		return new Object[][] { //
			{ "Import binary", PluginImportOperation.IMPORT_BINARY }, //
			{ "Import with source", PluginImportOperation.IMPORT_WITH_SOURCE }, //
			{ "Import with links", PluginImportOperation.IMPORT_BINARY_WITH_LINKS }, //
		};
	}

	@Parameter(0)
	public String importTypeName;
	@Parameter(1)
	public int importType;

	@Test
	public void testImportJAR() throws Exception {
		doSingleImport("org.eclipse.jsch.core", true);
	}

	@Test
	public void testImportFlat() throws Exception {
		doSingleImport("org.eclipse.jdt.debug", true);
	}

	@Test
	public void testImportNotJavaFlat() throws Exception {
		doSingleImport("org.junit.source", false);
	}

	@Test
	public void testImportNotJavaJARd() throws Exception {
		doSingleImport("org.eclipse.jdt.doc.user", false);
		doSingleImport("org.eclipse.pde.ui.source", false);
	}

	@Test
	public void testImportAnt() throws Exception {
		Assume.assumeFalse(importType == PluginImportOperation.IMPORT_WITH_SOURCE);
		// Note: Ant is exempt from importing as source
		doSingleImport("org.apache.ant", true);
	}

	@Test
	public void testImportJUnit4() throws Exception {
		doSingleImport("org.junit", 4, true);
	}

	@Test
	public void testImportLinksMultiple() throws Exception {
		List<IPluginModelBase> modelsToImport = Stream
				.of("org.eclipse.core.filebuffers", "org.eclipse.jdt.doc.user", "org.eclipse.pde.build").map(name -> {
					IPluginModelBase model = PluginRegistry.findModel(name);
					assertNotNull("No model found with name'" + name + "'", model);
					assertNull("Workspace resource already exists for: " + name, model.getUnderlyingResource());
					return model;
				}).toList();
		runOperation(modelsToImport, importType);
		for (int i = 0; i < modelsToImport.size(); i++) {
			verifyProject(modelsToImport.get(i), i != 1);
		}
	}

	protected void doSingleImport(String bundleSymbolicName, boolean isJava) throws Exception {
		IPluginModelBase modelToImport = PluginRegistry.findModel(bundleSymbolicName);
		assertNotNull("No model found with name'" + name + "'", modelToImport);
		assertNull(modelToImport.getUnderlyingResource());
		runOperation(List.of(modelToImport), importType);
		verifyProject(modelToImport, isJava);
	}

	/**
	 * Imports a bundle with the given symbolic name and a version with a major version matching
	 * the given int.  The result is checked for flags and natures.  This method was added to
	 * test org.junit which will have two versions of the same bundle in the SDK.
	 *
	 * @param bundleSymbolicName name of the plug-in to import
	 * @param majorVersion the major version that the imported plug-in must have
	 * @param isJava whether the imported plug-in should have a java nature
	 */
	protected void doSingleImport(String bundleSymbolicName, int majorVersion, boolean isJava) throws Exception {
		ModelEntry entry = PluginRegistry.findEntry(bundleSymbolicName);
		IPluginModelBase models[] = entry.getExternalModels();
		assertTrue("No external models for with name '" + bundleSymbolicName + "'", models.length > 0);
		IPluginModelBase modelToImport = Arrays.stream(models)
				.filter(m -> new Version(m.getPluginBase().getVersion()).getMajor() == majorVersion).findFirst()
				.orElse(null);

		assertNull("Model for " + bundleSymbolicName + " with major version " + majorVersion + " not be found",
				modelToImport.getUnderlyingResource());
		runOperation(List.of(modelToImport), importType);
		verifyProject(modelToImport, isJava);
	}

	protected void runOperation(List<IPluginModelBase> models, int type) throws InterruptedException {
		PluginImportOperation job = new PluginImportOperation(models.toArray(IPluginModelBase[]::new), type, false);
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.setSystem(true);
		job.schedule();
		job.join();
		IStatus status = job.getResult();
		assertTrue("Import Operation failed: " + status.toString(), status.isOK());
	}

	private void verifyProject(IPluginModelBase modelImported, boolean isJava) throws CoreException {
		String id = modelImported.getPluginBase().getId();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(id);
		assertTrue("Project " + id + " does not exist", project.exists());

		// When self hosting the tests, import tests may fail if you have the
		// imported project in the host
		boolean isFromSelfHostedTestRuntime = Platform.inDevelopmentMode()
				&& Path.of(modelImported.getInstallLocation()).getParent().getParent()
				.equals(FileLocator.getBundleFileLocation(FrameworkUtil.getBundle(BaseImportTestCase.class))
						.get().toPath().getParent().getParent());

		if (!isFromSelfHostedTestRuntime) {
			if (importType == PluginImportOperation.IMPORT_BINARY_WITH_LINKS) {
				assertThat(RepositoryProvider.getProvider(project), is(instanceOf(BinaryRepositoryProvider.class)));
			} else if (importType == PluginImportOperation.IMPORT_BINARY) {
				assertEquals(PDECore.BINARY_PROJECT_VALUE,
						project.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY));
			}
		}
		assertTrue(project.hasNature(PluginProject.NATURE));
		assertEquals(isJava, project.hasNature(JavaCore.NATURE_ID));
		if (isJava) {
			IJavaProject jProject = JavaCore.create(project);
			assertSourceAttached(jProject);
			if (importType == PluginImportOperation.IMPORT_WITH_SOURCE) {
				assertAnyClasspathEntryOfKind(jProject, IClasspathEntry.CPE_SOURCE);
			} else {
				if (!isFromSelfHostedTestRuntime) {
					assertAnyClasspathEntryOfKind(jProject, IClasspathEntry.CPE_LIBRARY);
				}
			}
		}
	}

	private void assertSourceAttached(IJavaProject jProject) throws CoreException {
		for (IPackageFragmentRoot root : jProject.getPackageFragmentRoots()) {
			IClasspathEntry entry = root.getRawClasspathEntry();
			if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY
					|| (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER)
					&& !entry.getPath().equals(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH)) {
				assertNotNull("Missing source attachement for entry " + entry, root.getSourceAttachmentPath());
			}
		}
	}

	private static void assertAnyClasspathEntryOfKind(IJavaProject project, int entryKind) throws JavaModelException {
		assertTrue(Arrays.stream(project.getRawClasspath()).anyMatch(e -> e.getEntryKind() == entryKind));
	}

}
