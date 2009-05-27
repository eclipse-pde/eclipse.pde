/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;

public abstract class IdentifiablePluginObject extends PluginObject implements IIdentifiable {

	private static final long serialVersionUID = 1L;
	protected String fID;

	public IdentifiablePluginObject() {
	}

	public String getId() {
		return fID;
	}

	public void setId(String id) throws CoreException {
		ensureModelEditable();
		String oldValue = fID;
		fID = id;
		firePropertyChanged(P_ID, oldValue, id);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_ID)) {
			setId(newValue != null ? newValue.toString() : null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.plugin.PluginObject#reconnect(org.eclipse.pde.core.plugin.ISharedPluginModel, org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public void reconnect(ISharedPluginModel model, IPluginObject parent) {
		super.reconnect(model, parent);
		// No transient fields
	}

}
