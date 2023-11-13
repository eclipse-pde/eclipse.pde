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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Project;

@Component(service = IAdapterFactory.class, property = {
		IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS + "=org.eclipse.core.resources.IProject",
		IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=aQute.bnd.build.Project" })
public class PdeBndAdapter implements IAdapterFactory {

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
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { Project.class };
	}

}