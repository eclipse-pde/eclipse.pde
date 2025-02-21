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

import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.AdapterTypes;
import org.eclipse.core.runtime.IAdapterFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import aQute.bnd.build.Project;
import biz.aQute.resolve.Bndrun;

@Component(service = IAdapterFactory.class)
@AdapterTypes(adaptableClass = IProject.class, adapterNames = { Project.class, VersionControlIgnoresManager.class,
		Bndrun.class })
public class PdeBndAdapter implements IAdapterFactory {

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile VersionControlIgnoresManager versionControlIgnoresManager;

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof IProject project) {
			if (adapterType == Project.class) {
				try {
					return adapterType.cast(BndProjectManager.getBndProject(project).orElse(null));
				} catch (Exception e) {
					// can't adapt then...
					return null;
				}
			}
			if (adapterType == VersionControlIgnoresManager.class) {
				return adapterType.cast(versionControlIgnoresManager);
			}
			if (adapterType == Bndrun.class) {
				try {
					return adapterType.cast(BndProjectManager.createBndrun(project).orElse(null));
				} catch (Exception e) {
					// can't adapt then...
					return null;
				}
			}
		}
		return null;
	}

}