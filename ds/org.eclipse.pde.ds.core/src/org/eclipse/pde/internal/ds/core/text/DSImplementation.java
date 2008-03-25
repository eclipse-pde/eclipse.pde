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

public class DSImplementation extends DSObject {

	private static final long serialVersionUID = 1L;

	
	public DSImplementation(DSModel model) {
		super(model, ELEMENT_IMPLEMENTATION);
	}

	public boolean canAddChild(int objectType) {
		return false;
	}

	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_PROPERTY ||objectType == TYPE_PROPERTIES || objectType == TYPE_SERVICE || objectType == TYPE_REFERENCE;
	}

	public boolean canBeParent() {
		return false;
	}

	public String getName() {
		return getClassName(); 
	}

	public int getType() {
		return TYPE_IMPLEMENTATION;
	}

	public void setClassName(String className){
		setXMLAttribute(ATTRIBUTE_CLASS, className);
	}
	
	public String getClassName(){
		return getXMLAttributeValue(ATTRIBUTE_CLASS);
	}
	

}
