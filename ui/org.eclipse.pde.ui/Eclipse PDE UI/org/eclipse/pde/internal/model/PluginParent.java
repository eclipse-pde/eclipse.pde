package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;

public abstract class PluginParent extends IdentifiablePluginObject implements IPluginParent {
	protected Vector children = new Vector();

public PluginParent() {
}
public void add(int index, IPluginObject child) throws CoreException {
	ensureModelEditable();
	children.add(index, child);
	((PluginObject)child).setParent(this);
	fireStructureChanged(child, IModelChangedEvent.INSERT);
}
public void add(IPluginObject child) throws CoreException {
	ensureModelEditable();
	children.add(child);
	fireStructureChanged(child, IModelChangedEvent.INSERT);
}
public int getChildCount() {
	return children.size();
}
public IPluginObject[] getChildren() {
	IPluginObject [] result = new IPluginObject[children.size()];
	children.copyInto(result);
	return result;
}
public void remove(IPluginObject child) throws CoreException {
	ensureModelEditable();
	children.removeElement(child);
	fireStructureChanged(child, ModelChangedEvent.REMOVE);
}
}
