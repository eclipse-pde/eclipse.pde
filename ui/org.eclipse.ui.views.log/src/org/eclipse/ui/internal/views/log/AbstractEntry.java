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

	/**
	 * The collection of direct children of this entry
	 */
	private List children = new ArrayList();
	protected Object parent;

	/**
	 * Adds the specified child entry to the listing of children.
	 * If the specified child is <code>null</code>, no work is done
	 * 
	 * @param child
	 */
	public void addChild(AbstractEntry child) {
		if (child != null) {
			children.add(0, child);
			child.setParent(this);
		}
	}

	/**
	 * @see IWorkbenchAdapter#getChildren(Object)
	 */
	public Object[] getChildren(Object parent) {
		return children.toArray();
	}

	/**
	 * @return true if this entry has children, false otherwise
	 */
	public boolean hasChildren() {
		return children.size() > 0;
	}

	/**
	 * @return the size of the child array
	 * 
	 * TODO rename to getChildCount(), or something more meaningful
	 */
	public int size() {
		return children.size();
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

	/**
	 * Sets the parent of this entry
	 * @param parent
	 */
	public void setParent(AbstractEntry parent) {
		this.parent = parent;
	}

	/**
	 * removes all of the children specified in the given listing
	 * 
	 * @param list the list of children to remove
	 */
	public void removeChildren(List list) {
		children.removeAll(list);
	}

	/**
	 * Removes all of the children from this entry
	 */
	public void removeAllChildren() {
		children.clear();
	}

	/**
	 * Writes this entry information into the given {@link PrintWriter}
	 * 
	 * @param writer
	 */
	public abstract void write(PrintWriter writer);
}
