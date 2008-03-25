/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

public class DSProperties extends DSObject {

	private static final long serialVersionUID = 1L;

	public DSProperties(DSModel model) {
		super(model, ELEMENT_PROPERTIES);
	}

	public boolean canAddChild(int objectType) {
		return false;
	}

	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_PROPERTY || objectType == TYPE_PROPERTIES ||objectType == TYPE_REFERENCE || objectType == TYPE_SERVICE;
	}

	public boolean canBeParent() {
		return false;
	}

	public String getName() {
		return getEntry();
	}

	public int getType() {
		return TYPE_PROPERTIES;
	}
	
	public void setEntry(String entry){
		setXMLAttribute(ATTRIBUTE_ENTRY, entry);
	}
	
	public String getEntry(){
		return getXMLAttributeValue(ATTRIBUTE_ENTRY);
	}
}
