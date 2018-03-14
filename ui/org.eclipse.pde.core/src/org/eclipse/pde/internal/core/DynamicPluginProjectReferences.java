/*******************************************************************************
 *  Copyright (c) 2018 Andrey Loskutov <loskutov@gmx.de> and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Collections;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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
				return new RequiredPluginsClasspathContainer(model).getAllProjectDependencies();
			}
		}
		return Collections.emptyList();
	}
}
