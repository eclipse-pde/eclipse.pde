/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.elements;

import org.eclipse.jface.viewers.ITreeContentProvider;

public class TreeContentProvider extends ListContentProvider implements ITreeContentProvider {

	public TreeContentProvider() {
		super();
	}

	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof IPDEElement) {
			return ((IPDEElement) element).getChildren();
		}
		return null;
	}

	@Override
	public Object[] getElements(Object element) {
		if (element instanceof IPDEElement) {
			return ((IPDEElement) element).getChildren();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IPDEElement) {
			return ((IPDEElement) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(java.lang.Object element) {
		if (element instanceof IPDEElement) {
			Object[] children = ((IPDEElement) element).getChildren();
			return children != null && children.length > 0;
		}
		return false;
	}

	public boolean isDeleted(Object element) {
		return false;
	}
}
