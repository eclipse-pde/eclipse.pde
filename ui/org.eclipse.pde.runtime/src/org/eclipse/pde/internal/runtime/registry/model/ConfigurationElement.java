/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.pde.internal.runtime.registry.model;

public class ConfigurationElement extends Attribute {

	private Attribute[] elements = new Attribute[0];

	public void setElements(Attribute[] elements) {
		if (elements == null)
			throw new IllegalArgumentException();

		this.elements = elements;
	}

	public Attribute[] getElements() {
		return elements;
	}
}
