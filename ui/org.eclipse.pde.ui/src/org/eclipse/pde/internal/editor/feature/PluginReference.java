package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.base.model.plugin.*;


public class PluginReference {
	private IFeaturePlugin reference;
	private IPluginModelBase model;
	private boolean fragment;

public PluginReference(IFeaturePlugin reference, IPluginModelBase model) {
	this.reference = reference;
	this.model = model;
}
public IPluginModelBase getModel() {
	return model;
}
public IFeaturePlugin getReference() {
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
public void setModel(IPluginModelBase newModel) {
	model = newModel;
}
public void setReference(IFeaturePlugin newReference) {
	reference = newReference;
}
	public String toString() {
		String name = model.getPluginBase().getName();
		return model.getResourceString(name);
	}
}
