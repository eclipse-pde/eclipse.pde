package org.eclipse.pde.internal.editor.component;

import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.base.model.plugin.*;


public class PluginReference {
	private IComponentReference reference;
	private IPluginModelBase model;
	private boolean fragment;

public PluginReference(IComponentReference reference, IPluginModelBase model) {
	this.reference = reference;
	this.model = model;
}
public org.eclipse.pde.internal.base.model.plugin.IPluginModelBase getModel() {
	return model;
}
public org.eclipse.pde.internal.base.model.component.IComponentReference getReference() {
	return reference;
}
public boolean isFragment() {
	return fragment;
}
public boolean isInSync() {
	if (model == null)
		return false;
	if (reference==null) return true;
	if (!reference.getId().equals(model.getPluginBase().getId()))
		return false;
	if (!reference.getVersion().equals(model.getPluginBase().getVersion()))
		return false;
	return true;
}
public boolean isUnresolved() {
	return false;
}
public void setFragment(boolean newFragment) {
	fragment = newFragment;
}
public void setModel(org.eclipse.pde.internal.base.model.plugin.IPluginModelBase newModel) {
	model = newModel;
}
public void setReference(org.eclipse.pde.internal.base.model.component.IComponentReference newReference) {
	reference = newReference;
}
	public String toString() {
		return model.getPluginBase().getName();
	}
}
