/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpModel;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject;

/**
 * Content provider for the tree section of the context help editor.  Gets the children of
 * each element.
 * @since 3.4
 * @see CtxHelpTreeSection
 */
public class CtxHelpContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof CtxHelpModel) {
			CtxHelpObject root = ((CtxHelpModel) parentElement).getCtxHelpRoot();
			return new Object[] {root};
		} else if (parentElement instanceof CtxHelpObject) {
			List<CtxHelpObject> list = ((CtxHelpObject) parentElement).getChildren();
			return list.toArray();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof CtxHelpObject) {
			return ((CtxHelpObject) element).getParent();
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
