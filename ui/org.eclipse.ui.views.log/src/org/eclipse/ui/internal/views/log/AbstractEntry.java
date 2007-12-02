/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bug 207344
 *******************************************************************************/
package org.eclipse.ui.internal.views.log;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Everything that appears in LogView is Abstract Entry. It provides composite pattern.
 */
public abstract class AbstractEntry extends PlatformObject implements IWorkbenchAdapter {

	private List children;
	protected Object parent;
	
	public void addChild(AbstractEntry child) {
		if (children == null) {
			children = new ArrayList();
		}
		children.add(0, child);
		child.setParent(this);
	}
	
	public void addChild(AbstractEntry child, int limit) {
		addChild(child);
		if (children.size() > limit) {
			children.remove(children.size() - 1);
		}
	}
	
	/**
	 * @see IWorkbenchAdapter#getChildren(Object)
	 */
	public Object[] getChildren(Object parent) {
		if (children == null)
			return new Object[0];
		return children.toArray();
	}
	
	public boolean hasChildren() {
		return children != null && children.size() > 0;
	}
	
	public int size() {
		return children != null ? children.size() : 0;
	}

	/**
	 * @see IWorkbenchAdapter#getImageDescriptor(Object)
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	/**
	 * @see IWorkbenchAdapter#getLabel(Object)
	 */
	public String getLabel(Object o) {
		return null;
	}

	/**
	 * @see IWorkbenchAdapter#getParent(Object)
	 */
	public Object getParent(Object o) {
		return parent;
	}
	
	public void setParent(AbstractEntry parent) {
		this.parent = parent;
	}

	public void removeChildren(List list) {
		if (children != null) {
			children.removeAll(list);
		}
	}
	
	public void removeAllChildren() {
		children.clear();
	}
	
	public abstract void write(PrintWriter writer);
}
