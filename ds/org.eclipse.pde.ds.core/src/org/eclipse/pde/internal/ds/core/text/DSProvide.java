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
import org.eclipse.pde.internal.ds.core.IDSProvide;

public class DSProvide extends DSObject implements IDSProvide {

	private static final long serialVersionUID = 1L;
	
	public DSProvide(DSModel model) {
		super(model, ELEMENT_PROVIDE);
		
		int prov_count = model.getDSComponent().getService()
				.getProvidedServices().length + 1;
		this.setInterface(IDSConstants.ATTRIBUTE_PROVIDE_INTERFACE
				+ prov_count);
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
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#canBeParent()
	 */
	public boolean canBeParent() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#getName()
	 */
	public String getName() {
		return getInterface();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#getType()
	 */
	public int getType() {
		return TYPE_PROVIDE;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProvide#setInterface(java.lang.String)
	 */
	public void setInterface(String interfaceName){
		setXMLAttribute(ATTRIBUTE_PROVIDE_INTERFACE, interfaceName);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSProvide#getInterface()
	 */
	public String getInterface(){
		return getXMLAttributeValue(ATTRIBUTE_PROVIDE_INTERFACE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.IDSObject#getAttributesNames()
	 */
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_PROVIDE_INTERFACE };
	}

	public boolean isLeafNode() {
		return true;
	}

}
