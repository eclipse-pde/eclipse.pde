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

import org.eclipse.pde.internal.ds.core.IDSProperty;

public class DSProperty extends DSObject implements IDSProperty {

	private static final long serialVersionUID = 1L;

	public DSProperty(DSModel model) {
		super(model, ELEMENT_PROPERTY);
	}

	public boolean canAddChild(int objectType) {
		return false;
	}

	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_PROPERTY || objectType == TYPE_PROPERTIES || objectType == TYPE_REFERENCE || objectType == TYPE_SERVICE;
	}

	public boolean canBeParent() {
		return false;
	}

	public String getName() {
		return getPropertyName();
	}

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
	public String getPropertyElemBody(){ // TODO Confirm if this method is correct
		return getXMLContent(); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProperty#setPropertyElemBody(java.lang.String)
	 */
	public void setPropertyElemBody(String body){
		setXMLContent(body);
	}
	
}
