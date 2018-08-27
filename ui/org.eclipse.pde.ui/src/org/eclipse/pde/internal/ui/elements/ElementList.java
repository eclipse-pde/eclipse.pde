/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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

import java.util.Vector;
import org.eclipse.swt.graphics.Image;

public class ElementList extends NamedElement {
	private Vector<IPDEElement> children = new Vector<>();

	public ElementList(String name) {
		super(name);
	}

	public ElementList(String name, Image icon) {
		super(name, icon);
	}

	public ElementList(String name, Image icon, IPDEElement parent) {
		super(name, icon, parent);
	}

	public void add(IPDEElement child) {
		children.addElement(child);
	}

	@Override
	public Object[] getChildren() {
		if (children.isEmpty())
			return new Object[0];
		Object[] result = new Object[children.size()];
		children.copyInto(result);
		return result;
	}

	public void remove(IPDEElement child) {
		children.remove(child);
	}

	public int size() {
		return children.size();
	}

	@Override
	public String toString() {
		return children.toString();
	}
}
