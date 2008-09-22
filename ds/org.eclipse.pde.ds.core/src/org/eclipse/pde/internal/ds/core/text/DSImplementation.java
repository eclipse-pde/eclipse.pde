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

import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSObject;

/**
 * Represents the component implementation class
 * 
 * @since 3.4
 * @see IDSComponent
 * @see IDSObject
 * 
 */
public class DSImplementation extends DSObject implements IDSImplementation {

	private static final long serialVersionUID = 1L;

	public DSImplementation(DSModel model) {
		super(model, ELEMENT_IMPLEMENTATION);
		this.setClassName(IDSConstants.ELEMENT_IMPLEMENTATION);
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
		return getClassName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getType()
	 */
	public int getType() {
		return TYPE_IMPLEMENTATION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSImplementation#setClassName(java.lang.String)
	 */
	public void setClassName(String className) {
		setXMLAttribute(ATTRIBUTE_IMPLEMENTATION_CLASS, className);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSImplementation#getClassName()
	 */
	public String getClassName() {
		return getXMLAttributeValue(ATTRIBUTE_IMPLEMENTATION_CLASS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSObject#getClassName()
	 */
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_IMPLEMENTATION_CLASS };
	}

	public boolean isLeafNode() {
		return true;
	}

}
