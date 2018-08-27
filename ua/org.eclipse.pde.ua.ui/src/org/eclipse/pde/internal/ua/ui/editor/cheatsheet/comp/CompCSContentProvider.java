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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;

public class CompCSContentProvider implements ITreeContentProvider {

	public CompCSContentProvider() {
		// NO-OP
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ICompCSModel) {
			return new Object[] { ((ICompCSModel) parentElement).getCompCS() };
		} else if (parentElement instanceof ICompCSObject) {
			List<? extends ICompCSObject> list = ((ICompCSObject) parentElement).getChildren();
			// List is never null
			if (list.size() > 0) {
				return list.toArray();
			}
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof ICompCSObject) {
			return ((ICompCSObject) element).getParent();
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
