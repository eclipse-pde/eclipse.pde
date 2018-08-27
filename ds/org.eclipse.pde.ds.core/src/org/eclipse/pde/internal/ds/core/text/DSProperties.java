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
		return getEntry();
	}

	@Override
	public int getType() {
		return TYPE_PROPERTIES;
	}

	@Override
	public void setEntry(String entry){
		setXMLAttribute(ATTRIBUTE_PROPERTIES_ENTRY, entry);
	}

	@Override
	public String getEntry(){
		return getXMLAttributeValue(ATTRIBUTE_PROPERTIES_ENTRY);
	}

	@Override
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_PROPERTIES_ENTRY };
	}

	@Override
	public boolean isLeafNode() {
		return true;
	}

}
