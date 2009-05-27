/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
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

public abstract class PluginParent extends IdentifiablePluginObject implements IPluginParent {
	private static final long serialVersionUID = 1L;
	protected ArrayList fChildren = null;

	public PluginParent() {
	}

	public void add(int index, IPluginObject child) throws CoreException {
		ensureModelEditable();
		getChildrenList().add(index, child);
		postAdd(child);
	}

	public void add(IPluginObject child) throws CoreException {
		ensureModelEditable();
		getChildrenList().add(child);
		postAdd(child);
	}

	void appendChild(IPluginElement child) {
		getChildrenList().add(child);
	}

	protected void postAdd(IPluginObject child) {
		((PluginObject) child).setInTheModel(true);
		((PluginObject) child).setParent(this);
		fireStructureChanged(child, IModelChangedEvent.INSERT);
	}

	public int getChildCount() {
		return getChildrenList().size();
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
				IPluginObject child = (IPluginObject) getChildrenList().get(i);
				if (child == null || child.equals(tchild) == false)
					return false;
			}
			return true;
		}
		return false;
	}

	public int getIndexOf(IPluginObject child) {
		return getChildrenList().indexOf(child);
	}

	public void swap(IPluginObject child1, IPluginObject child2) throws CoreException {
		ensureModelEditable();
		int index1 = getChildrenList().indexOf(child1);
		int index2 = getChildrenList().indexOf(child2);
		if (index1 == -1 || index2 == -1)
			throwCoreException(PDECoreMessages.PluginParent_siblingsNotFoundException);
		getChildrenList().set(index2, child1);
		getChildrenList().set(index1, child2);
		firePropertyChanged(this, P_SIBLING_ORDER, child1, child2);
	}

	public IPluginObject[] getChildren() {
		return (IPluginObject[]) getChildrenList().toArray(new IPluginObject[getChildrenList().size()]);
	}

	public void remove(IPluginObject child) throws CoreException {
		ensureModelEditable();
		getChildrenList().remove(child);
		((PluginObject) child).setInTheModel(false);
		fireStructureChanged(child, IModelChangedEvent.REMOVE);
	}

	protected ArrayList getChildrenList() {
		if (fChildren == null)
			fChildren = new ArrayList(1);
		return fChildren;
	}

}
