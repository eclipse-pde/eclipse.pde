/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkingSetElementAdapter#adaptElements(org.eclipse.ui.IWorkingSet, org.eclipse.core.runtime.IAdaptable[])
	 */
	public IAdaptable[] adaptElements(IWorkingSet ws, IAdaptable[] elements) {
		HashSet set = new HashSet();
		for (int i = 0; i < elements.length; i++) {
			IResource res = (IResource) elements[i].getAdapter(IResource.class);
			if (res == null)
				continue;
			IProject proj = res.getProject();
			IPluginModelBase base = PluginRegistry.findModel(proj);
			// if project is a plug-in project
			if (base == null)
				continue;
			BundleDescription desc = base.getBundleDescription();
			String id = (desc != null) ? desc.getSymbolicName() : base.getPluginBase().getId();
			set.add(new PersistablePluginObject(id));
		}
		return (IAdaptable[]) set.toArray(new IAdaptable[set.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkingSetElementAdapter#dispose()
	 */
	public void dispose() {
	}

}
