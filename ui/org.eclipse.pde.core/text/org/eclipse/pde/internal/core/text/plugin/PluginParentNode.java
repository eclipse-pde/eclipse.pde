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
package org.eclipse.pde.internal.core.text.plugin;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

public class PluginParentNode extends PluginObjectNode implements IPluginParent {

	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#add(int, org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public void add(int index, IPluginObject child) throws CoreException {
		addChildNode((IDocumentElementNode) child, index);
		fireStructureChanged(child, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#add(org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public void add(IPluginObject child) throws CoreException {
		add(getChildCount(), child);
		child.setInTheModel(true);
		((PluginObjectNode) child).setModel(getModel());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#getChildCount()
	 */
	public int getChildCount() {
		return getChildNodes().length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#getIndexOf(org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public int getIndexOf(IPluginObject child) {
		return indexOf((IDocumentElementNode) child);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#swap(org.eclipse.pde.core.plugin.IPluginObject, org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public void swap(IPluginObject child1, IPluginObject child2) throws CoreException {
		swap((IDocumentElementNode) child1, (IDocumentElementNode) child2);
		firePropertyChanged(this, P_SIBLING_ORDER, child1, child2);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#getChildren()
	 */
	public IPluginObject[] getChildren() {
		ArrayList result = new ArrayList();
		IDocumentElementNode[] nodes = getChildNodes();
		for (int i = 0; i < nodes.length; i++)
			result.add(nodes[i]);

		return (IPluginObject[]) result.toArray(new IPluginObject[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#remove(org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public void remove(IPluginObject child) throws CoreException {
		removeChildNode((IDocumentElementNode) child);
		child.setInTheModel(false);
		fireStructureChanged(child, IModelChangedEvent.REMOVE);
	}
}
