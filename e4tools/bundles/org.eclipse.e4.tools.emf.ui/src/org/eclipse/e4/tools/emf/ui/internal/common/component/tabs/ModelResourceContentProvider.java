/*******************************************************************************
 * Copyright (c) 2014, 2015 TwelveTone LLC and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Steven Spungin <steven@spungin.tv> - initial API and implementation
 *******************************************************************************/

package org.eclipse.e4.tools.emf.ui.internal.common.component.tabs;

import java.util.ArrayList;

import org.eclipse.e4.tools.emf.ui.common.IModelResource;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ModelResourceContentProvider implements IStructuredContentProvider {

	private Object[] items = new Object[0];

	@Override
	public Object[] getElements(Object object) {
		return items;
	}

	@Override
	public void inputChanged(final Viewer viewer, Object oldInput, Object newInput) {
		ArrayList<EObject> list = new ArrayList<>();
		IModelResource modelProvider = (IModelResource) newInput;
		if (newInput != null) {
			TreeIterator<Object> itTree = EcoreUtil.getAllContents(modelProvider.getRoot());
			while (itTree.hasNext()) {
				Object next = itTree.next();
				EObject eObject = (EObject) next;
				EAttribute att = EmfUtil.getAttribute(eObject, "elementId"); //$NON-NLS-1$
				if (att != null) {
					list.add(eObject);
				}
			}
		}

		items = list.toArray(new Object[0]);
	}

	@Override
	public void dispose() {
	}
}
