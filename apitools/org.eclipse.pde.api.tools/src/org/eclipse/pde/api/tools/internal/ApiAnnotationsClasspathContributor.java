/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.core.IClasspathContributor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.osgi.resource.Resource;

public class ApiAnnotationsClasspathContributor implements IClasspathContributor {

	private static final Collection<String> API_TOOLS_ANNOTATIONS = List.of("org.eclipse.pde.api.tools.annotations"); //$NON-NLS-1$

	@Override
	public List<IClasspathEntry> getInitialEntries(BundleDescription project) {
		IPluginModelBase projectModel = PluginRegistry.findModel((Resource) project);
		if (hasApiNature(projectModel)) {
			return ClasspathUtilCore.classpathEntries(annotations().filter(model -> !model.equals(projectModel)))
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}



	private boolean hasApiNature(IPluginModelBase model) {
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource != null) {
				try {
					return resource.getProject().hasNature(ApiPlugin.NATURE_ID);
				} catch (CoreException e) {
					// assume not compatible project then...
				}
			}
		}
		return false;
	}



	/**
	 * @return s stream of all current available annotations in the current plugin
	 *         registry
	 */
	public static Stream<IPluginModelBase> annotations() {
		return API_TOOLS_ANNOTATIONS.stream().map(PluginRegistry::findModel).filter(Objects::nonNull)
				.filter(IPluginModelBase::isEnabled);
	}

	@Override
	public List<IClasspathEntry> getEntriesForDependency(BundleDescription project, BundleDescription addedDependency) {
		return Collections.emptyList();
	}

}
