/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.toc;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.internal.ua.core.toc.text.TocModel;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;

public class TocContentProvider implements ITreeContentProvider {

	public TocContentProvider() {
		// NO-OP
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TocModel) {
			return new Object[] {((TocModel) parentElement).getToc()};
		} else if (parentElement instanceof TocObject) {
			List<TocObject> list = ((TocObject) parentElement).getChildren();
			// List is never null
			if (list.size() > 0) {
				return list.toArray();
			}
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof TocObject) {
			return ((TocObject) element).getParent();
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
