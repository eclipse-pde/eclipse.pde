/*******************************************************************************
 *  Copyright (c) 2018, 2026 Andrey Loskutov <loskutov@gmx.de> and others.
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IDynamicReferenceProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

/**
 * Project references provider for JDT build to compute right build order with
 * indirect plug-in dependencies
 */
public class DynamicPluginProjectReferences implements IDynamicReferenceProvider {

	@Override
	public List<IProject> getDependentProjects(IBuildConfiguration buildConfiguration) throws CoreException {
		IProject input = buildConfiguration.getProject();
		IJavaProject javaProject = JavaCore.create(input);
		if (javaProject != null) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(javaProject.getProject());
			if (model != null) {
				BundleDescription bundle = model.getBundleDescription();
				if (bundle != null) {
					return ClasspathComputer.collectBuildRelevantDependencies(Set.of(bundle)).stream()
							.map(b -> (org.osgi.resource.Resource) b) //
							.filter(dependency -> dependency != bundle).map(PluginRegistry::findModel)
							.filter(Objects::nonNull)
							.map(IPluginModelBase::getUnderlyingResource).filter(Objects::nonNull)
							.map(IResource::getProject).distinct().toList();
				}
			}
		}
		return Collections.emptyList();
	}
}
