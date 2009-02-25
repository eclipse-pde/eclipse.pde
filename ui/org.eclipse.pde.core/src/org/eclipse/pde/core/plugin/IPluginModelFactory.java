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
 * instances of the plug-in model objects.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IPluginModelFactory extends IExtensionsModelFactory {
	/**
	 * Creates a new plug-in import
	 * @return a new plug-in import instance
	 */
	IPluginImport createImport();

	/**
	 * Creates a new library instance
	 *
	 *@return a new library instance
	 */
	IPluginLibrary createLibrary();
}
