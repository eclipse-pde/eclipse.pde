/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import org.eclipse.core.runtime.CoreException;

/**
 * A model object that contains the portion of the plug-in model
 * responsible for extensions and extension points. If
 * the plug-in contains OSGi manifest file, plugin.xml is
 * reduced to extensions and extension points only.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.0
 */
public interface IExtensions extends IPluginObject {
	/**
	 * A model property that will be used when order of extensions
	 * changes in this object.
	 */
	String P_EXTENSION_ORDER = "extension_order"; //$NON-NLS-1$

	/**
	 * Adds a new extension to this object. This
	 * method will throw a CoreException if
	 * model is not editable.
	 *
	 * @param extension the extension object
	 * @throws CoreException if the model is not editable
	 */
	void add(IPluginExtension extension) throws CoreException;

	/**
	 * Adds a new extension point to this object.
	 * This method will throw a CoreException if the model is not editable.
	 * 
	 * @param extensionPoint the extension point
	 * @throws CoreException if the model is not editable
	 */
	void add(IPluginExtensionPoint extensionPoint) throws CoreException;

	/**
	 * Returns extension points defined in this object.
	 * @return an array of extension point objects
	 */
	IPluginExtensionPoint[] getExtensionPoints();

	/**
	 * Returns extensions defined in this object.
	 *
	 * @return an array of extension objects
	 */
	IPluginExtension[] getExtensions();

	/**
	 * Removes an extension from this object. This
	 * method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param extension the extension object
	 * @throws CoreException if the model is not editable
	 */
	void remove(IPluginExtension extension) throws CoreException;

	/**
	 * Removes an extension point from this object. This
	 * method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param extensionPoint the extension point object
	 * @throws CoreException if the model is not editable
	 */
	void remove(IPluginExtensionPoint extensionPoint) throws CoreException;

	/**
	 * Swaps the positions of the provided extensions
	 * in the list of extensions.
	 *
	 * @param e1 the first extension object
	 * @param e2 the second extension object
	 * @throws CoreException if the model is not editable
	 */
	void swap(IPluginExtension e1, IPluginExtension e2) throws CoreException;

	/**
	 * Returns the position of the extension in the receiver.
	 * @param e the extension
	 * @return the 0-based index of the extension in the receiver.
	 */
	int getIndexOf(IPluginExtension e);
}
