/*******************************************************************************
 *  Copyright (c) 2007, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.util.HashSet;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetElementAdapter;

public class PluginAdapter implements IWorkingSetElementAdapter {

	@Override
	public IAdaptable[] adaptElements(IWorkingSet ws, IAdaptable[] elements) {
		HashSet<PersistablePluginObject> set = new HashSet<>();
		for (IAdaptable element : elements) {
			IResource res = element.getAdapter(IResource.class);
			if (res == null) {
				continue;
			}
			IProject proj = res.getProject();
			IPluginModelBase base = PluginRegistry.findModel(proj);
			// if project is a plug-in project
			if (base == null) {
				continue;
			}
			BundleDescription desc = base.getBundleDescription();
			String id = (desc != null) ? desc.getSymbolicName() : base.getPluginBase().getId();
			set.add(new PersistablePluginObject(id));
		}
		return set.toArray(new IAdaptable[set.size()]);
	}

	@Override
	public void dispose() {
	}

}
