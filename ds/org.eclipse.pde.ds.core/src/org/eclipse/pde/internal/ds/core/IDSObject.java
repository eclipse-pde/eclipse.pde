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

/**
 * Represents a general DS element of a DS xml file.
 * 
 */
public interface IDSObject extends IDocumentObject {

	/**
	 * Returns the model object of this object.
	 * 
	 * @return IDSModel object containing the model.
	 */
	public abstract IDSModel getModel();

	/**
	 * Returns the root element that is an ancestor to this object.
	 * 
	 * @return IDSComponent object containing the root element.
	 */
	public abstract IDSComponent getComponent();

	/**
	 * Returns the identifier for this object to be used when displaying the
	 * element to the user
	 * 
	 * @return String containing the identifier
	 */
	public abstract String getName();

	/**
	 * Returns the concrete type of this object, must be one of the TYPE constants
	 * defined in IDSConstants.
	 * 
	 * @return int value containing one of the TYPE constants defined in
	 *         IDSConstants.
	 * @see IDSConstants
	 */
	public abstract int getType();
	
	/**
	 * Returns a boolean which represents if this object is capable of
	 * containing children
	 * 
	 * @return true if this object can have children.
	 */
	public abstract boolean canBeParent();
	
	
	/**
	 * Returns all attributes` names of this element
	 * 
	 * @return an array of String containing all attributes` names
	 */
	public abstract String[] getAttributesNames();
	
	/**
	 * Returns the XML Tag Name of this element
	 * 
	 * @return a String containing the XML Tag Name
	 */
	public abstract String getXMLTagName();
	
	
}
