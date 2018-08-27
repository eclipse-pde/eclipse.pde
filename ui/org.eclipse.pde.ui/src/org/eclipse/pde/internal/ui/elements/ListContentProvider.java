/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.elements;

import org.eclipse.jface.viewers.IStructuredContentProvider;

public class ListContentProvider implements IStructuredContentProvider {

	public ListContentProvider() {
		super();
	}

	@Override
	public Object[] getElements(Object element) {
		if (element instanceof IPDEElement) {
			return ((IPDEElement) element).getChildren();
		}
		return null;
	}
}
