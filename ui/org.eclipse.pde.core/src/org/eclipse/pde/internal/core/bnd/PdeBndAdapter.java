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
package org.eclipse.pde.internal.core.bnd;

import java.io.InputStream;
import java.util.jar.Manifest;

import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.AdapterTypes;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.resource.ResourceBuilder;

@Component(service = IAdapterFactory.class)
@AdapterTypes(adaptableClass = IProject.class, adapterNames = { Project.class, VersionControlIgnoresManager.class,
		Resource.class })
public class PdeBndAdapter implements IAdapterFactory {

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile VersionControlIgnoresManager versionControlIgnoresManager;

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == Project.class) {
			if (adaptableObject instanceof IProject project) {
				try {
					return adapterType.cast(BndProjectManager.getBndProject(project).orElse(null));
				} catch (Exception e) {
					// can't adapt then...
					return null;
				}
			}
		}
		if (adapterType == VersionControlIgnoresManager.class) {
			return adapterType.cast(versionControlIgnoresManager);
		}
		if (adapterType == Resource.class) {
			if (adaptableObject instanceof IProject project) {
				if (PluginProject.isPluginProject(project)) {
					IFile manifestFile = PDEProject.getManifest(project);
					if (manifestFile != null && manifestFile.exists()) {
						Manifest manifest;
						try (InputStream stream = manifestFile.getContents()) {
							manifest = new Manifest(stream);
						} catch (Exception e) {
							return null;
						}
						ResourceBuilder builder = new ResourceBuilder();
						builder.addManifest(manifest);
						return adapterType.cast(builder.build());
					}
				}
			}
		}
		return null;
	}

}