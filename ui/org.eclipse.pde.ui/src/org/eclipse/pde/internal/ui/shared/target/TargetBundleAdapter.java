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
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.ui.target.ITargetLocationHandler;

public class TargetBundleAdapter implements IAdapterFactory {

	private ToggleIncludeHandler<TargetBundle> handler = new ToggleIncludeHandler<>(TargetBundle.class,
			TargetDefinition.MODE_PLUGIN, ITargetDefinition::getAllBundles,
			TargetBundleAdapter::asNameVersionDescriptor);

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof TargetBundle) {
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

	private static NameVersionDescriptor asNameVersionDescriptor(TargetBundle bundle) {
		BundleInfo info = bundle.getBundleInfo();
		if (info == null) {
			return null;
		}
		return new NameVersionDescriptor(info.getSymbolicName(), info.getVersion(), NameVersionDescriptor.TYPE_PLUGIN);
	}

}
