/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

/**
 * This factory should be used to create
 * instances of the extensions model objects.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.0
 */
public interface IExtensionsModelFactory {
	/**
	 * Creates a new attribute instance for the
	 * provided element.
	 *
	 * @param element the parent element
	 * @return the new attribute instance
	 */
	IPluginAttribute createAttribute(IPluginElement element);

	/**
	 * Creates a new element instance for the
	 * provided parent.
	 *
	 * @param parent the parent element
	 * @return the new element instance
	 */
	IPluginElement createElement(IPluginObject parent);

	/**
	 * Creates a new extension instance.
	 * @return the new extension instance
	 */
	IPluginExtension createExtension();

	/**
	 * Creates a new extension point instance
	 *
	 * @return a new extension point 
	 */
	IPluginExtensionPoint createExtensionPoint();
}
