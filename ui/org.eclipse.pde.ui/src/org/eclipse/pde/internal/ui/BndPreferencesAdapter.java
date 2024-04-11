/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.AdapterTypes;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.pde.bnd.ui.preferences.BndPreferences;
import org.eclipse.pde.internal.core.natures.BndProject;
import org.osgi.service.component.annotations.Component;

@Component(service = IAdapterFactory.class)
@AdapterTypes(adaptableClass = IProject.class, adapterNames = BndPreferences.class)
public class BndPreferencesAdapter implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof IProject project) {
			if (adapterType == BndPreferences.class) {
				if (BndProject.isBndProject(project)) {
					return adapterType.cast(new BndPreferences(project, PDEPlugin.getDefault().getPreferenceStore()));
				}
			}
		}
		return null;
	}

}
