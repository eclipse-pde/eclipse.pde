/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 223738
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core;

/**
 * A component is a normal Java class contained within a bundle
 * 
 * <component> ::= <implementation> <properties> * <service> ? <reference> *
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
	 * Sets the value of the configuration policy
	 * 
	 * @param policy
	 *            String value of the configuration policy.
	 */
	public void setConfigurationPolicy(String policy);

	/**
	 * Returns the value of the configuration policy
	 * 
	 * @return String value of the configuration policy
	 */
	public String getConfigurationPolicy();

	/**
	 * Sets the value of the activate method signature name
	 * 
	 * @param name
	 *            String value of the activate method signature name
	 */
	public void setActivateMethod(String name);

	/**
	 * Returns the value of the activate method signature name
	 * 
	 * @return String value of the activate method signature name
	 */
	public String getActivateMethod();

	/**
	 * Sets the value of the deactivate method signature name
	 * 
	 * @param name
	 *            String value of the deactivate method signature name
	 */
	public void setDeactivateMethod(String name);

	/**
	 * Returns the value of the deactivate method signature name
	 * 
	 * @return String value of the deactivate method signature name
	 */
	public String getDeactivateMethod();

	/**
	 * Sets the value of the modified method signature name
	 * 
	 * @param name
	 *            String value of the modified method signature name
	 */
	public void setModifiedeMethod(String name);

	/**
	 * Returns the value of the modified method signature name
	 * 
	 * @return String value of the modified method signature name
	 */
	public String getModifiedMethod();

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
	 * Returns the service implementation class
	 * 
	 * @return IDSImplementation containing the Implementation element
	 */
	public IDSImplementation getImplementation();

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
	 * Returns the Service element
	 * 
	 * @return IDSService containing the Service element
	 */
	public IDSService getService();

	/**
	 * Returns all Reference elements
	 * 
	 * @return IDSReference's array containing all Reference elements
	 */
	public IDSReference[] getReferences();

	/**
	 * Sets the implementation element with information about the component
	 * implementation class
	 * 
	 * 
	 * @param implementation
	 *            IDSImplementation object
	 */
	public void setImplementation(IDSImplementation implementation);

	/**
	 * Sets the service element with information to be used when a component
	 * configuration is to be registered as a service.
	 * 
	 * @param service
	 *            IDSService object
	 */
	public void setService(IDSService service);

	/**
	 * Adds a property element with information about a single property file
	 * 
	 * @param property
	 *            IDSProperty object
	 */
	public void addPropertyElement(IDSProperty property);

	/**
	 * Adds a properties element with information about a set of properties from
	 * a bundle entry
	 * 
	 * @param properties
	 *            IDSProperties object
	 * 
	 */
	public void addPropertiesElement(IDSProperties properties);

	/**
	 * Adds a reference element with information about the reference of a bound
	 * service
	 * 
	 * @param reference
	 *            IDSReference object
	 */
	public void addReference(IDSReference reference);

	/**
	 * Removes a property element
	 * 
	 * @param property
	 *            IDSProperty object
	 */
	public void removePropertyElement(IDSProperty property);

	/**
	 * Removes a properties element
	 * 
	 * @param properties
	 *            IDSProperties object
	 * 
	 */
	public void removePropertiesElement(IDSProperties properties);

	/**
	 * Removes a reference element
	 * 
	 * @param reference
	 *            IDSReference object
	 */
	public void removeReference(IDSReference reference);

	/**
	 * Removes a service element
	 * 
	 * @param service
	 *            IDSService object
	 */
	public void removeService(IDSService service);

}
