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

import java.util.List;

import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.eclipse.pde.internal.ds.core.text.DSObject;

public interface IDSObject {

	/**
	 * @return the children of the object or an empty List if none exist.
	 */
	public abstract List getChildren();

	/**
	 * @return the root model object that is an ancestor to this object.
	 */
	public abstract DSModel getModel();

	/**
	 * @return the root element that is an ancestor to this object.
	 */
	public abstract IDSRoot getRoot();

	/**
	 * @return the identifier for this object to be used when displaying the element to the user
	 */
	public abstract String getName();

	/**
	 * Get the concrete type of this object, must be one of the TYPE constants defined in IDSConstants.
	 * @see IDSConstants
	 */
	public abstract int getType();
	
	/**
	 * @return the parent of this object, or <code>null</code> if there is no
	 *         parent.
	 */
	public abstract DSObject getParent();
	
	

}
