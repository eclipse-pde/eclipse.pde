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

import java.util.ArrayList;

import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSService extends DSObject implements IDSService {

	private static final long serialVersionUID = 1L;

	public DSService(DSModel model) {
		super(model, ELEMENT_SERVICE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#canAddChild(int)
	 */

	public boolean canAddChild(int objectType) {
		return objectType == TYPE_PROVIDE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#canBeParent()
	 */
	public boolean canBeParent() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#getName()
	 */
	public String getName() {
		return IDSConstants.ELEMENT_SERVICE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#getType()
	 */
	public int getType() {
		return TYPE_SERVICE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSService#setServiceFactory(boolean)
	 */
	public void setServiceFactory(boolean bool) {
		setBooleanAttributeValue(ATTRIBUTE_SERVICE_FACTORY, bool);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSService#getServiceFactory()
	 */
	public boolean getServiceFactory() {
		return getBooleanAttributeValue(ATTRIBUTE_SERVICE_FACTORY, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSService#getProvidedServices()
	 */
	public IDSProvide[] getProvidedServices() {
		ArrayList childNodesList = getChildNodesList(IDSProvide.class, true);
		IDSProvide[] providedServices = new IDSProvide[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			providedServices[i] = (IDSProvide) childNodesList.get(i);
		}
		return providedServices;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSService#addProvidedService(org.eclipse.pde.internal.ds.core.IDSProvide)
	 */
	public void addProvidedService(IDSProvide provide) {
		this.addChildNode(provide, true);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.IDSService#removeProvidedService(org.eclipse.pde.internal.ds.core.IDSProvide)
	 */
	public void removeProvidedService(IDSProvide provide) {
		this.removeChildNode(provide, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.IDSObject#getAttributesNames()
	 */
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_SERVICE_FACTORY };
	}
}
