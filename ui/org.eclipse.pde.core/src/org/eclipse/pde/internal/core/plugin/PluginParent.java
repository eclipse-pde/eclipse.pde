package org.eclipse.pde.internal.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;

public abstract class PluginParent extends IdentifiablePluginObject implements IPluginParent {
	protected Vector children = new Vector();

public PluginParent() {
}
public void add(int index, IPluginObject child) throws CoreException {
	ensureModelEditable();
	children.add(index, child);
	postAdd(child);
}

public void add(IPluginObject child) throws CoreException {
	ensureModelEditable();
	children.add(child);
	postAdd(child);
}

protected void postAdd(IPluginObject child) {
	((PluginObject)child).setInTheModel(true);
	((PluginObject)child).setParent(this);
	fireStructureChanged(child, IModelChangedEvent.INSERT);
}


public int getChildCount() {
	return children.size();
}

public int getIndexOf(IPluginObject child) {
	return children.indexOf(child);
}

public void swap(IPluginObject child1, IPluginObject child2) throws CoreException {
	ensureModelEditable();
	int index1 = children.indexOf(child1);
	int index2 = children.indexOf(child2);
	if (index1 == -1 || index2 == -1)
		throwCoreException("Siblings not in the model");
	children.setElementAt(child1, index2);
	children.setElementAt(child2, index1);
	firePropertyChanged(this, P_SIBLING_ORDER, child1, child2);
}

public IPluginObject[] getChildren() {
	IPluginObject [] result = new IPluginObject[children.size()];
	children.copyInto(result);
	return result;
}
public void remove(IPluginObject child) throws CoreException {
	ensureModelEditable();
	children.removeElement(child);
	((PluginObject)child).setInTheModel(false);
	fireStructureChanged(child, ModelChangedEvent.REMOVE);
}
public void reconnect() {
	for (int i=0; i<children.size(); i++) {
		PluginObject child = (PluginObject)children.get(i);
		child.setModel(getModel());
		child.setParent(this);
		if (child instanceof PluginParent) {
			((PluginParent)child).reconnect();
		}
	}
}
}
