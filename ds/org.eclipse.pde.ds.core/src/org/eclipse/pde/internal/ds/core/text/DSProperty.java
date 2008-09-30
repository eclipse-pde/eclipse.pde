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
import org.eclipse.pde.internal.ds.core.IDSProperty;

public class DSProperty extends DSObject implements IDSProperty {

	private static final long serialVersionUID = 1L;
	public DSProperty(DSModel model) {
		super(model, ELEMENT_PROPERTY);
		
		// set Default Values
		this.setPropertyType(IDSConstants.VALUE_PROPERTY_TYPE_STRING);
		int property_count = model.getDSComponent().getPropertyElements().length + 1;
		this.setPropertyName(IDSConstants.ELEMENT_PROPERTY + property_count);
		this.setPropertyValue(IDSConstants.ATTRIBUTE_PROPERTY_VALUE);
		
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
		return getPropertyName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getType()
	 */
	public int getType() {
		return TYPE_PROPERTY;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperty#getPropertyName()
	 */
	public String getPropertyName(){
		return getXMLAttributeValue(ATTRIBUTE_PROPERTY_NAME);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperty#setPropertyName(java.lang.String)
	 */
	public void setPropertyName(String name){
		setXMLAttribute(ATTRIBUTE_PROPERTY_NAME, name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperty#getPropertyValue()
	 */
	public String getPropertyValue(){
		return getXMLAttributeValue(ATTRIBUTE_PROPERTY_VALUE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperty#setPropertyValue(java.lang.String)
	 */
	public void setPropertyValue(String value){
		setXMLAttribute(ATTRIBUTE_PROPERTY_VALUE, value);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperty#getPropertyType()
	 */
	public String getPropertyType(){
		return getXMLAttributeValue(ATTRIBUTE_PROPERTY_TYPE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperty#setPropertyType(java.lang.String)
	 */
	public void setPropertyType(String type){
		setXMLAttribute(ATTRIBUTE_PROPERTY_TYPE, type);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperty#getPropertyElemBody()
	 */
	public String getPropertyElemBody() {
		return getXMLContent(); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperty#setPropertyElemBody(java.lang.String)
	 */
	public void setPropertyElemBody(String body){
		setXMLContent(body);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.IDSObject#getAttributesNames()
	 */
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_PROPERTY_NAME,
				IDSConstants.ATTRIBUTE_PROPERTY_TYPE,
				IDSConstants.ATTRIBUTE_PROPERTY_VALUE };
	}

	public boolean isLeafNode() {
		return true;
	}
	
}
