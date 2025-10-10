/*******************************************************************************
 *  Copyright (c) 2021, 2023 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.core.builders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;

public class BundleErrorReporterTest {

	private IFile manifest;

	@Before
	public void setup() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(getClass().getName());
		manifest = project.getFile("META-INF/MANIFEST.MF");
	}

	@Test
	public void testErrorOnUnresolvedJrePackage() throws Exception {
		IProject project = ProjectUtils.createPluginProject(manifest.getProject().getName(),
				JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("JavaSE-1.8")).getProject();

		IFile manifest = project.getFile("META-INF/MANIFEST.MF");
		PDEModelUtility.modifyModel(new ModelModification(manifest) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
				IBundle bundle = modelBase.getBundleModel().getBundle();
				bundle.setHeader(Constants.IMPORT_PACKAGE, "java.lang.module");
			}
		}, null);

		assertThat(findUnresolvedImportsMarkers()).hasSize(1);

		ModelModification modification = new ModelModification(manifest) {
			@SuppressWarnings("deprecation")
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
				IBundle bundle = modelBase.getBundleModel().getBundle();
				bundle.setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "JavaSE-11");
			}
		};
		PDEModelUtility.modifyModel(modification, null);

		assertThat(findUnresolvedImportsMarkers()).isEmpty();
	}

	@Test
	public void testNoNPEWhenCreatingExtensionPoint() throws Exception {
		// Test for bug fix: NPE when creating extension point without directives
		IProject project = ProjectUtils.createPluginProject(manifest.getProject().getName()).getProject();

		// Create a plugin.xml to make it have extensions
		IFile pluginXml = project.getFile("plugin.xml");
		String pluginXmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<?eclipse version=\"3.4\"?>\n" +
				"<plugin>\n" +
				"   <extension-point id=\"testpoint\" name=\"Test Point\" schema=\"schema/testpoint.exsd\"/>\n" +
				"</plugin>\n";
		pluginXml.create(new java.io.ByteArrayInputStream(pluginXmlContent.getBytes()), true, null);

		IFile manifest = project.getFile("META-INF/MANIFEST.MF");
		PDEModelUtility.modifyModel(new ModelModification(manifest) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
				IBundle bundle = modelBase.getBundleModel().getBundle();
				// Set a bundle symbolic name without singleton directive or attribute
				// This triggers the code path where getDirectiveKeys() could return null
				bundle.setHeader(Constants.BUNDLE_SYMBOLICNAME, project.getName());
			}
		}, null);

		// This should not throw NPE
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);

		// Verify no unexpected errors (there should be an error about missing singleton)
		IMarker[] markers = manifest.findMarkers(PDEMarkerFactory.MARKER_ID, false, 0);
		assertThat(markers).isNotEmpty(); // There should be a singleton error
	}

	private List<IMarker> findUnresolvedImportsMarkers() throws CoreException {
		manifest.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		return Arrays.stream(manifest.findMarkers(PDEMarkerFactory.MARKER_ID, false, 0))
				.filter(m -> m.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR).toList();
	}

	@After
	public void tearDown() throws Exception {
		if (manifest.getProject().exists()) {
			manifest.getProject().delete(true, null);
		}
	}

}
