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
		return getInterface();
	}

	@Override
	public int getType() {
		return TYPE_PROVIDE;
	}

	@Override
	public void setInterface(String interfaceName){
		setXMLAttribute(ATTRIBUTE_PROVIDE_INTERFACE, interfaceName);
	}

	@Override
	public String getInterface(){
		return getXMLAttributeValue(ATTRIBUTE_PROVIDE_INTERFACE);
	}

	@Override
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_PROVIDE_INTERFACE };
	}

	@Override
	public boolean isLeafNode() {
		return true;
	}

}
