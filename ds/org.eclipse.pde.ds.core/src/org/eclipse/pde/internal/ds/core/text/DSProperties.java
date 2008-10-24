/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSProperties;

/**
 * Represents a set of properties from a bundle entry
 * 
 * @since 3.4
 * @see DSObject
 * @see DSComponent
 * @see DSModel
 */
public class DSProperties extends DSObject implements IDSProperties {

	private static final long serialVersionUID = 1L;
	
	public DSProperties(DSModel model) {
		super(model, ELEMENT_PROPERTIES);
		int number = model.getDSComponent().getPropertiesElements().length + 1;
		this.setEntry(IDSConstants.ATTRIBUTE_PROPERTIES_ENTRY
				+ number);
		setInTheModel(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#canAddChild(int)
	 */
	public boolean canAddChild(int objectType) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#canBeParent()
	 */
	public boolean canBeParent() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getName()
	 */
	public String getName() {
		return getEntry();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getType()
	 */
	public int getType() {
		return TYPE_PROPERTIES;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperties#setEntry(java.lang.String)
	 */
	public void setEntry(String entry){
		setXMLAttribute(ATTRIBUTE_PROPERTIES_ENTRY, entry);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperties#getEntry()
	 */
	public String getEntry(){
		return getXMLAttributeValue(ATTRIBUTE_PROPERTIES_ENTRY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.IDSObject#getAttributesNames()
	 */
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_PROPERTIES_ENTRY };
	}

	public boolean isLeafNode() {
		return true;
	}

}
