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
 * Represents the service information to be used when a component configuration
 * is to be registered as a service.
 * 
 * @since 3.4
 * @see IDSComponent
 * @see IDSObject
 */
public interface IDSService extends IDSObject {

	/**
	 * Sets the attribute servicefactory
	 * 
	 * This attribute controls whether the service uses the ServiceFactory
	 * concept of the OSGi Framework.
	 * 
	 * The default value is false.
	 * 
	 * If servicefactory is set to true, a different component configuration is
	 * created, activated and its component instance returned as the service
	 * object for each distinct bundle that requests the service.Each of these
	 * component configurations has the same component properties.Otherwise, the
	 * same component instance from the single component configuration is
	 * returned as the service object for all bundles that request the service.
	 * 
	 * The servicefactory attribute must not be true if the component is a
	 * factory component or an immediate component. This is because SCR is not
	 * free to create component configurations as necessary to support
	 * servicefactory. A component description is ill-formed if it specifies
	 * that the component is a factory component or an immediate component and
	 * servicefactory is set to true.
	 * 
	 * @param bool
	 *            new boolean value of attribute servicefactory
	 */
	public void setServiceFactory(boolean bool);

	/**
	 * Returns the value of attribute servicefactory
	 * 
	 * @return boolean containing the value of attribute servicefactory
	 */
	public boolean getServiceFactory();

	/**
	 * Return all Service's interfaces
	 * 
	 * @return IDSProvide's array containing all provide elements
	 */
	public IDSProvide[] getProvidedServices();

	/**
	 * Adds a provide element with information about a service`s interface
	 * 
	 * @param provide IDSProvide object
	 */
	public void addProvidedService(IDSProvide provide);
	
	/**
	 * Removes a provide element
	 * 
	 * @param provide IDSProvide object
	 */
	public void removeProvidedService(IDSProvide provide);

}
