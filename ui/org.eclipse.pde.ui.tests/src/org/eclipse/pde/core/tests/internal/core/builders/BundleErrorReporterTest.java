/*******************************************************************************
 *  Copyright (c) 2021 Julian Honnen
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
import org.eclipse.pde.internal.core.builders.CompilerFlags;
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
				JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("JavaSE-11")).getProject();

		IFile manifest = project.getFile("META-INF/MANIFEST.MF");
		PDEModelUtility.modifyModel(new ModelModification(manifest) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
				IBundle bundle = modelBase.getBundleModel().getBundle();
				bundle.setHeader(Constants.IMPORT_PACKAGE, "javax.xml.ws");
			}
		}, null);

		assertThat(findUnresolvedImportsMarkers()).hasSize(1);

		ModelModification modification = new ModelModification(manifest) {
			@SuppressWarnings("deprecation")
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
				IBundle bundle = modelBase.getBundleModel().getBundle();
				bundle.setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "JavaSE-1.8");
			}
		};
		PDEModelUtility.modifyModel(modification, null);

		assertThat(findUnresolvedImportsMarkers()).isEmpty();
	}

	private List<IMarker> findUnresolvedImportsMarkers() throws CoreException {
		manifest.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		return Arrays.stream(manifest.findMarkers(PDEMarkerFactory.MARKER_ID, false, 0)).filter(
				m -> m.getAttribute(PDEMarkerFactory.compilerKey, "").equals(CompilerFlags.P_UNRESOLVED_IMPORTS))
				.toList();
	}

	@After
	public void tearDown() throws Exception {
		if (manifest.getProject().exists()) {
			manifest.getProject().delete(true, null);
		}
	}

}
