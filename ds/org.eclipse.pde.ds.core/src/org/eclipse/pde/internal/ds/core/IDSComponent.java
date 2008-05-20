/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira NÃ³brega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core;

/**
 * A component is a normal Java class contained within a bundle
 * 
 * <component> ::=
 * 	<implementation> 
 * 	<properties> * 
 * 	<service> ? 
 * 	<reference> *
 */
public interface IDSComponent extends IDSObject {

	/**
	 * Sets the value of the attribute name
	 * 
	 * @param name
	 *            New name
	 */
	public void setAttributeName(String name);

	/**
	 * Returns the value of the attribute name
	 * 
	 * @return String value of the attribute name
	 */
	public String getAttributeName();

	/**
	 * Sets the value of the attribute enabled
	 * 
	 * @param bool
	 *            boolean value of the attribute enabled.
	 */
	public void setEnabled(boolean bool);

	/**
	 * Returns the value of the attribute enabled
	 * 
	 * @return boolean value of the attribute enabled
	 */
	public boolean getEnabled();

	/**
	 * Sets the value of the attribute factory
	 * 
	 * @param factory
	 *            String value of the attribute factory.
	 */
	public void setFactory(String factory);

	/**
	 * Returns the value of the attribute factory
	 * 
	 * @return String value of the attribute factory
	 */
	public String getFactory();

	/**
	 * Sets the value of the attribute immediate
	 * 
	 * @param bool
	 *            boolean value of the attribute immediate.
	 */
	public void setImmediate(boolean bool);

	/**
	 * Returns the value of the attribute immediate
	 * 
	 * @return boolean value of the attribute immediate
	 */
	public boolean getImmediate();

	/**
	 * Returns all of the service implementation classes
	 * 
	 * @return IDSImplementation's array containing all Implementation elements
	 */
	public IDSImplementation[] getImplementations();

	/**
	 * Returns all Property elements
	 * 
	 * @return IDSProperty's array containing all Property elements
	 */
	public IDSProperty[] getPropertyElements();

	/**
	 * Returns all Properties elements
	 * 
	 * @return IDSProperties's array containing all Properties elements
	 */
	public IDSProperties[] getPropertiesElements();

	/**
	 * Returns all Service elements
	 * 
	 * @return IDSService's array containing all Service elements
	 */
	public IDSService[] getServices();

	/**
	 * Returns all Reference elements
	 * 
	 * @return IDSReference's array containing all Reference elements
	 */
	public IDSReference[] getReferences();

}
