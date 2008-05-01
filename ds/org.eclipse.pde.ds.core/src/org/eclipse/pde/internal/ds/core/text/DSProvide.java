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

import org.eclipse.pde.internal.ds.core.IDSProvide;

public class DSProvide extends DSObject implements IDSProvide {

	private static final long serialVersionUID = 1L;

	public DSProvide(DSModel model) {
		super(model, ELEMENT_PROVIDE);
	}

	public boolean canAddChild(int objectType) {
		return false;
	}

	public boolean canAddSibling(int objectType) {
		return false;
	}

	public boolean canBeParent() {
		return false;
	}

	public String getName() {
		return getInterface();
	}

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
}
