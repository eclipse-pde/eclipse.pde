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

/**
 * Handles the creation of document nodes representing the types of elements
 * that can exist in a declarative services xml file.
 * 
 * @since 3.4
 * @see IDSModel
 * @see IDSComponent
 * 
 */
public interface IDSDocumentFactory {

	/**
	 * creates a <code>provide</code> element
	 * 
	 * @return IDSProvide object, containing the new element
	 * @see IDSProvide
	 * @see IDSService
	 * @see IDSComponent
	 */
	public abstract IDSProvide createProvide();

	/**
	 * creates a <code>property</code> element
	 * 
	 * @return IDSProperty object, containing the new element
	 * @see IDSProperty
	 * @see IDSComponent
	 */
	public abstract IDSProperty createProperty();


	/**
	 * creates a <code>reference</code> element
	 * 
	 * @return IDSReference object, containing the new element
	 * @see IDSReference
	 * @see IDSComponent
	 */
	public abstract IDSReference createReference();

	/**
	 * creates a <code>service</code> element
	 * 
	 * @return IDSService object, containing the new element
	 * @see IDSService
	 * @see IDSComponent
	 */
	public abstract IDSService createService();

	/**
	 * creates a <code>properties</code> element
	 * 
	 * @return IDSProperties object, containing the new element
	 * @see IDSProperties
	 * @see IDSComponent
	 */
	public abstract IDSProperties createProperties();

	/**
	 * creates a <code>implementation</code> element
	 * 
	 * @return IDSImplementation object, containing the new element
	 * @see IDSImplementation
	 * @see IDSComponent
	 */
	public abstract IDSImplementation createImplementation();

	/**
	 * creates a <code>component</code> element
	 * 
	 * @return IDSComponent object, containing the new element
	 * @see IDSComponent
	 */
	public abstract IDSComponent createComponent();

}
