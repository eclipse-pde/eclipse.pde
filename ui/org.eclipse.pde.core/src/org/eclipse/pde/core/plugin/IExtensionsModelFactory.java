/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

/**
 * This factory should be used to create
 * instances of the plug-in model objects.
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
