package org.eclipse.pde.internal.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.model.*;

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
