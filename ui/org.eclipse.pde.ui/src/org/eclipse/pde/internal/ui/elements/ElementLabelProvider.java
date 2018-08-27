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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class ElementLabelProvider extends LabelProvider {
	public static final ElementLabelProvider INSTANCE = new ElementLabelProvider();

	public ElementLabelProvider() {
		super();
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof IPDEElement) {
			return ((IPDEElement) element).getImage();
		}
		return super.getImage(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof IPDEElement) {
			return ((IPDEElement) element).getLabel();
		}
		return super.getText(element);
	}
}
