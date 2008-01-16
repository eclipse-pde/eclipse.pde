/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.comp;

import java.util.List;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;

/**
 * CompCSContentProvider
 *
 */
public class CompCSContentProvider extends DefaultContentProvider implements ITreeContentProvider {

	/**
	 * 
	 */
	public CompCSContentProvider() {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ICompCSModel) {
			return new Object[] {((ICompCSModel) parentElement).getCompCS()};
		} else if (parentElement instanceof ICompCSObject) {
			List list = ((ICompCSObject) parentElement).getChildren();
			// List is never null
			if (list.size() > 0) {
				return list.toArray();
			}
		}
		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof ICompCSObject) {
			return ((ICompCSObject) element).getParent();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		return (getChildren(element).length > 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

}
