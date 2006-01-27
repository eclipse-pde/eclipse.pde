/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;

public abstract class IdentifiablePluginObject extends PluginObject implements IIdentifiable {
	protected String id;

public IdentifiablePluginObject() {
}
public String getId() {
	return id;
}
public void setId(String id) throws CoreException {
	ensureModelEditable();
	String oldValue = this.id;
	this.id = id;
	firePropertyChanged(P_ID, oldValue, id);
}

public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
	if (name.equals(P_ID)) {
		setId(newValue!=null ? newValue.toString():null);
		return;
	}
	super.restoreProperty(name, oldValue, newValue);
}

}
