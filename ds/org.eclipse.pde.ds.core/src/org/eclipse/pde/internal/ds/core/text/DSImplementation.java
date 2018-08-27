/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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

	@Override
	public boolean canAddChild(int objectType) {
		return false;
	}

	@Override
	public boolean canBeParent() {
		return false;
	}

	@Override
	public String getName() {
		return getClassName();
	}

	@Override
	public int getType() {
		return TYPE_IMPLEMENTATION;
	}

	@Override
	public void setClassName(String className) {
		setXMLAttribute(ATTRIBUTE_IMPLEMENTATION_CLASS, className);
	}

	@Override
	public String getClassName() {
		return getXMLAttributeValue(ATTRIBUTE_IMPLEMENTATION_CLASS);
	}

	@Override
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_IMPLEMENTATION_CLASS };
	}

	@Override
	public boolean isLeafNode() {
		return true;
	}

}
