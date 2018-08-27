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

import java.util.ArrayList;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSService extends DSObject implements IDSService {

	private static final long serialVersionUID = 1L;

	public DSService(DSModel model) {
		super(model, ELEMENT_SERVICE);
	}


	@Override
	public boolean canAddChild(int objectType) {
		return objectType == TYPE_PROVIDE;
	}

	@Override
	public boolean canBeParent() {
		return true;
	}

	@Override
	public String getName() {
		return IDSConstants.ELEMENT_SERVICE;
	}

	@Override
	public int getType() {
		return TYPE_SERVICE;
	}

	@Override
	public void setServiceFactory(boolean bool) {
		setBooleanAttributeValue(ATTRIBUTE_SERVICE_FACTORY, bool);
	}

	@Override
	public boolean getServiceFactory() {
		return getBooleanAttributeValue(ATTRIBUTE_SERVICE_FACTORY, false);
	}

	@Override
	public IDSProvide[] getProvidedServices() {
		ArrayList<IDocumentElementNode> childNodesList = getChildNodesList(IDSProvide.class, true);
		IDSProvide[] providedServices = new IDSProvide[childNodesList.size()];
		for (int i = 0; i < childNodesList.size(); i++) {
			providedServices[i] = (IDSProvide) childNodesList.get(i);
		}
		return providedServices;
	}

	@Override
	public void addProvidedService(IDSProvide provide) {
		this.addChildNode(provide, true);
	}


	@Override
	public void removeProvidedService(IDSProvide provide) {
		this.removeChildNode(provide, true);
	}

	@Override
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_SERVICE_FACTORY };
	}
}
