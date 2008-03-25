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

public class DSService extends DSObject {

	private static final long serialVersionUID = 1L;

	public DSService(DSModel model) {
		super(model, ELEMENT_SERVICE);
	}

	public boolean canAddChild(int objectType) {
		return objectType == TYPE_PROVIDE;
	}

	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_REFERENCE; // TODO Should I consider any ordering? Or should I add here: Implementation and Properties too?
	}

	public boolean canBeParent() {
		return true;
	}

	public String getName() {
		return getServiceFactory();
	}

	public int getType() {
		return TYPE_SERVICE;
	}
	
	public void setServiceFactory(String factory){
		setXMLAttribute(ATTRIBUTE_SERVICE_FACTORY, factory);
	}
	
	public String getServiceFactory(){
		return getXMLAttributeValue(ATTRIBUTE_SERVICE_FACTORY);
	}

}
