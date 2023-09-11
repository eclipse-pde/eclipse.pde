/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.ui.target.ITargetLocationHandler;

public class TargetFeatureAdapter implements IAdapterFactory {

	private final ToggleIncludeHandler<TargetFeature> handler = new ToggleIncludeHandler<>(TargetFeature.class,
			TargetDefinition.MODE_FEATURE,
			ITargetDefinition::getAllFeatures, TargetFeatureAdapter::asNameVersionDescriptor);

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof TargetFeature) {
			if (adapterType == ITargetLocationHandler.class) {
				return adapterType.cast(handler);
			}
		}

		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { ITargetLocationHandler.class };
	}

	private static NameVersionDescriptor asNameVersionDescriptor(TargetFeature info) {
		return new NameVersionDescriptor(info.getId(), info.getVersion(), NameVersionDescriptor.TYPE_PLUGIN);
	}

}
