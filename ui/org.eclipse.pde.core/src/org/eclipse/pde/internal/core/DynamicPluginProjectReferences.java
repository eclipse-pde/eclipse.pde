/*******************************************************************************
 *  Copyright (c) 2018 Andrey Loskutov <loskutov@gmx.de> and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IDynamicReferenceProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * Project references provider for JDT build to compute right build order with
 * indirect plug-in dependencies
 */
public class DynamicPluginProjectReferences implements IDynamicReferenceProvider {

	public static State state;

	public DynamicPluginProjectReferences() {
		super();
	}

	@Override
	public List<IProject> getDependentProjects(IBuildConfiguration buildConfiguration) throws CoreException {
		IProject input = buildConfiguration.getProject();
		IJavaProject javaProject = JavaCore.create(input);
		if (javaProject != null) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(javaProject.getProject());
			if (model != null) {
				List<IProject> allProj = new ArrayList<>();
				BundleDescription currentBundle = model.getBundleDescription();
				if (state == null) {
					state = StateObjectFactory.defaultFactory.createState(true);
				}
				if (state != null && currentBundle != null) {
					BundleDescription[] bundlePrereq = state.getStateHelper()
							.getPrerequisites(new BundleDescription[] { currentBundle });
					for (BundleDescription bundle : bundlePrereq) {
						if (bundle.equals(currentBundle)) {
							continue;
						}
						IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
						IProject project = root.getProject(bundle.getName());
						if (project.exists() && !allProj.contains(project)) {
							allProj.add(project);
						}
					}
				}
				return allProj;
			}
		}
		return Collections.emptyList();
	}
}
