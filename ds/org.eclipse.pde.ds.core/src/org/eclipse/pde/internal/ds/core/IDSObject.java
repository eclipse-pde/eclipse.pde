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
package org.eclipse.pde.internal.ds.core;

import org.eclipse.pde.internal.core.text.IDocumentObject;

public interface IDSObject extends IDocumentObject {

	/**
	 * @return the root model object that is an ancestor to this object.
	 */
	public abstract IDSModel getModel();

	/**
	 * @return the root element that is an ancestor to this object.
	 */
	public abstract IDSComponent getComponent();

	/**
	 * @return the identifier for this object to be used when displaying the element to the user
	 */
	public abstract String getName();

	/**
	 * Get the concrete type of this object, must be one of the TYPE constants
	 * defined in IDSConstants.
	 * 
	 * @return
	 * @see IDSConstants
	 */
	public abstract int getType();
	

	//	public abstract int getChildNodeCount(Class clazz);
	
	public abstract boolean canBeParent();
}
