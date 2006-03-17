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

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.PDECoreMessages;

public abstract class PluginParent extends IdentifiablePluginObject implements
		IPluginParent {
	protected ArrayList fChildren = new ArrayList(1);

	public PluginParent() {
	}

	public void add(int index, IPluginObject child) throws CoreException {
		ensureModelEditable();
		fChildren.add(index, child);
		postAdd(child);
	}

	public void add(IPluginObject child) throws CoreException {
		ensureModelEditable();
		fChildren.add(child);
		postAdd(child);
	}

	void appendChild(IPluginElement child) {
		fChildren.add(child);
	}

	protected void postAdd(IPluginObject child) {
		((PluginObject) child).setInTheModel(true);
		((PluginObject) child).setParent(this);
		fireStructureChanged(child, IModelChangedEvent.INSERT);
	}

	public int getChildCount() {
		return fChildren.size();
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IPluginParent) {
			IPluginParent target = (IPluginParent) obj;
			if (target.getChildCount() != getChildCount())
				return false;
			IPluginObject[] tchildren = target.getChildren();
			for (int i = 0; i < tchildren.length; i++) {
				IPluginObject tchild = tchildren[i];
				if (tchild.equals(fChildren.get(i)) == false)
					return false;
			}
			return true;
		}
		return false;
	}

	public int getIndexOf(IPluginObject child) {
		return fChildren.indexOf(child);
	}

	public void swap(IPluginObject child1, IPluginObject child2)
			throws CoreException {
		ensureModelEditable();
		int index1 = fChildren.indexOf(child1);
		int index2 = fChildren.indexOf(child2);
		if (index1 == -1 || index2 == -1)
			throwCoreException(PDECoreMessages.PluginParent_siblingsNotFoundException); 
		fChildren.set(index2, child1);
		fChildren.set(index1, child2);
		firePropertyChanged(this, P_SIBLING_ORDER, child1, child2);
	}

	public IPluginObject[] getChildren() {
		return (IPluginObject[])fChildren.toArray(new IPluginObject[fChildren.size()]);
	}

	public void remove(IPluginObject child) throws CoreException {
		ensureModelEditable();
		fChildren.remove(child);
		((PluginObject) child).setInTheModel(false);
		fireStructureChanged(child, IModelChangedEvent.REMOVE);
	}

	public void reconnect() {
		for (int i = 0; i < fChildren.size(); i++) {
			PluginObject child = (PluginObject) fChildren.get(i);
			child.setModel(getModel());
			child.setParent(this);
			if (child instanceof PluginParent) {
				((PluginParent) child).reconnect();
			}
		}
	}
}
