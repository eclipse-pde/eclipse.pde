/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	public void add(int index, IPluginObject child) throws CoreException {
		addChildNode((IDocumentElementNode) child, index);
		fireStructureChanged(child, IModelChangedEvent.INSERT);
	}

	@Override
	public void add(IPluginObject child) throws CoreException {
		add(getChildCount(), child);
		child.setInTheModel(true);
		((PluginObjectNode) child).setModel(getModel());
	}

	@Override
	public int getChildCount() {
		return getChildNodes().length;
	}

	@Override
	public int getIndexOf(IPluginObject child) {
		return indexOf((IDocumentElementNode) child);
	}

	@Override
	public void swap(IPluginObject child1, IPluginObject child2) throws CoreException {
		swap((IDocumentElementNode) child1, (IDocumentElementNode) child2);
		firePropertyChanged(this, P_SIBLING_ORDER, child1, child2);
	}

	@Override
	public IPluginObject[] getChildren() {
		ArrayList<IDocumentElementNode> result = new ArrayList<>();
		IDocumentElementNode[] nodes = getChildNodes();
		for (IDocumentElementNode childNode : nodes) {
			result.add(childNode);
		}

		return result.toArray(new IPluginObject[result.size()]);
	}

	@Override
	public void remove(IPluginObject child) throws CoreException {
		removeChildNode((IDocumentElementNode) child);
		child.setInTheModel(false);
		fireStructureChanged(child, IModelChangedEvent.REMOVE);
	}
}
