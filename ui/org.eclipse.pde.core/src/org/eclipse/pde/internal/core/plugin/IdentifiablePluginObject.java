/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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

	@Override
	public String getId() {
		return fID;
	}

	@Override
	public void setId(String id) throws CoreException {
		ensureModelEditable();
		String oldValue = fID;
		fID = id;
		firePropertyChanged(P_ID, oldValue, id);
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_ID)) {
			setId(newValue != null ? newValue.toString() : null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	@Override
	public void reconnect(ISharedPluginModel model, IPluginObject parent) {
		super.reconnect(model, parent);
		// No transient fields
	}

}
