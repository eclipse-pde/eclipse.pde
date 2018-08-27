/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.ui.*;

public class PersistablePluginObject extends PlatformObject implements IPersistableElement, IElementFactory {

	public static final String FACTORY_ID = "org.eclipse.pde.ui.elementFactory"; //$NON-NLS-1$
	public static final String KEY = "org.eclipse.pde.workingSetKey"; //$NON-NLS-1$
	private static PluginContainmentAdapter fgContainmentAdapter;

	private String fPluginID;

	public PersistablePluginObject() {
	}

	public PersistablePluginObject(String pluginID) {
		fPluginID = pluginID;
	}

	@Override
	public String getFactoryId() {
		return FACTORY_ID;
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString(KEY, fPluginID);
	}

	@Override
	public IAdaptable createElement(IMemento memento) {
		return new PersistablePluginObject(memento.getString(KEY));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(IPersistableElement.class))
			return (T) this;
		if (adapter.equals(IResource.class))
			return (T) getResource();
		if (adapter.equals(IContainmentAdapter.class))
			return (T) getPluginContainmentAdapter();
		if (adapter.equals(IJavaElement.class)) {
			IResource res = getResource();
			if (res instanceof IProject) {
				IProject project = (IProject) res;
				try {
					if (project.hasNature(JavaCore.NATURE_ID))
						return (T) JavaCore.create(project);
				} catch (CoreException e) {
				}
			}
		}
		return super.getAdapter(adapter);
	}

	public IResource getResource() {
		IPluginModelBase model = PluginRegistry.findModel(fPluginID);
		IResource resource = (model != null) ? model.getUnderlyingResource() : null;
		return resource == null ? null : resource.getProject();
	}

	public String getPluginID() {
		return fPluginID;
	}

	private static IContainmentAdapter getPluginContainmentAdapter() {
		if (fgContainmentAdapter == null)
			fgContainmentAdapter = new PluginContainmentAdapter();
		return fgContainmentAdapter;
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof PersistablePluginObject) {
			String id = ((PersistablePluginObject) arg0).fPluginID;
			return (fPluginID != null) ? fPluginID.equals(id) : id == null;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fPluginID.hashCode();
	}

}
