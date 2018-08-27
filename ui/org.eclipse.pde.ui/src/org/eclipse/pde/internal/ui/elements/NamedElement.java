/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.elements;

import org.eclipse.swt.graphics.Image;

public class NamedElement extends DefaultElement {
	protected Image image;

	private String name;

	private IPDEElement parent;

	public NamedElement(String name) {
		this(name, null, null);
	}

	public NamedElement(String name, Image icon) {
		this(name, icon, null);
	}

	public NamedElement(String name, Image image, IPDEElement parent) {
		this.name = name;
		this.image = image;
		this.parent = parent;
	}

	@Override
	public Image getImage() {
		return image;
	}

	@Override
	public String getLabel() {
		return name;
	}

	@Override
	public Object getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
