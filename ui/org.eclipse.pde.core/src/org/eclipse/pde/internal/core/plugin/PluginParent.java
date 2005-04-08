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

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

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

void appendChild(IPluginElement child) {
	children.add(child);
}


protected void postAdd(IPluginObject child) {
	((PluginObject)child).setInTheModel(true);
	((PluginObject)child).setParent(this);
	fireStructureChanged(child, IModelChangedEvent.INSERT);
}


public int getChildCount() {
	return children.size();
}

public boolean equals(Object obj) {
	if (this==obj) return true;
	if (obj==null) return false;
	if (obj instanceof IPluginParent) {
		IPluginParent target = (IPluginParent)obj;
		if (target.getChildCount()!=getChildCount())
			return false;
		IPluginObject [] tchildren = target.getChildren();
		for (int i=0; i<tchildren.length; i++) {
			IPluginObject tchild = tchildren[i];
			if (tchild.equals(children.get(i))==false)
				return false;
		}
		return true;
	}
	return false;
}

public int getIndexOf(IPluginObject child) {
	return children.indexOf(child);
}

public void swap(IPluginObject child1, IPluginObject child2) throws CoreException {
	ensureModelEditable();
	int index1 = children.indexOf(child1);
	int index2 = children.indexOf(child2);
	if (index1 == -1 || index2 == -1)
		throwCoreException(PDECoreMessages.PluginParent_siblingsNotFoundException); //$NON-NLS-1$
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
