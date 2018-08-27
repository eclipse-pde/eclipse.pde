/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSObject;

public class SimpleCSContentProvider implements ITreeContentProvider {

	public SimpleCSContentProvider() {
		// intentionally left empty
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ISimpleCSModel) {
			return new Object[] { ((ISimpleCSModel) parentElement)
					.getSimpleCS() };
		} else if (parentElement instanceof ISimpleCSObject) {
			List<IDocumentElementNode> list = ((ISimpleCSObject) parentElement).getChildren();
			// List is never null
			if (list.size() > 0) {
				return list.toArray();
			}
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof ISimpleCSObject) {
			return ((ISimpleCSObject) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (getChildren(element).length > 0);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

}
