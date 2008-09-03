/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import java.util.List;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSObject;

public class DSContentProvider implements IContentProvider,
		ITreeContentProvider {

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IDSModel) {
			return new Object[] { ((IDSModel) parentElement).getDSComponent() };
		} else if (parentElement instanceof IDSObject) {
			List list = ((IDSObject) parentElement).getChildNodesList();
			// List is never null
			if (list.size() > 0) {
				return list.toArray();
			}
		}
		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof IDSObject) {
			return ((IDSObject) element).getParentNode();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		return (getChildren(element).length > 0);
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {

	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

}
