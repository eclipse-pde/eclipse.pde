package org.eclipse.pde.internal.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.*;

public abstract class IdentifiablePluginObject extends PluginObject implements IIdentifiable {
	protected String id;

public IdentifiablePluginObject() {
}
public String getId() {
	return id;
}
public void setId(String id) throws CoreException {
	ensureModelEditable();
	this.id = id;
	firePropertyChanged(P_ID);
}
}
