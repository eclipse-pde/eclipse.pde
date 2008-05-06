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

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSService extends DSObject implements IDSService {

	private static final long serialVersionUID = 1L;

	public DSService(DSModel model) {
		super(model, ELEMENT_SERVICE);
	}

	public boolean canAddChild(int objectType) {
		return objectType == TYPE_PROVIDE;
	}

	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_REFERENCE; // TODO Should I consider any ordering? Or should I add here: Implementation and Properties too?
	}

	public boolean canBeParent() {
		return true;
	}

	public String getName() {
		return "Service";
	}

	public int getType() {
		return TYPE_SERVICE;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSService#setServiceFactory(boolean)
	 */
	public void setServiceFactory(boolean bool){
		setBooleanAttributeValue(ATTRIBUTE_SERVICE_FACTORY, bool);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSService#getServiceFactory()
	 */
	public boolean getServiceFactory(){
		return getBooleanAttributeValue(ATTRIBUTE_SERVICE_FACTORY, false);
	}

	public void removeChild(IDSProvide item) {
		removeChildNode((IDocumentElementNode) item, true);
	}

	public IDSProvide[] getProvidesElements() {
		return (IDSProvide[]) getChildNodesList(IDSProvide.class, true)
				.toArray();
	}
}
