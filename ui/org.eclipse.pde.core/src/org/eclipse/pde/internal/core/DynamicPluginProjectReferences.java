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

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

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
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * Project references provider for JDT build to compute right build order with
 * indirect plug-in dependencies
 */
public class DynamicPluginProjectReferences implements IDynamicReferenceProvider {

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
				BundleDescription currentBundle = model.getBundleDescription();
				if (currentBundle != null) {
					IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
					return BuildDependencyCollector.collectBuildRelevantDependencies(singleton(currentBundle)).stream()
							.filter(dependency -> dependency != currentBundle)
							.map(dependency -> root.getProject(dependency.getName())).filter(IProject::exists)
							.distinct().collect(toList());
				}
			}
		}
		return Collections.emptyList();
	}
}
