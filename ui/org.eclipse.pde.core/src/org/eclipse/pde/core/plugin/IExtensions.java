/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import org.eclipse.core.runtime.*;

/**
 * A model object that contains the portion of the plug-in model
 * responsible for extensions and extension points. If
 * the plug-in contains OSGi manifest file, plugin.xml is
 * reduced to extensions and extension points only.
 * 
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
	 */
	void add(IPluginExtension extension) throws CoreException;
	/**
	 * @param extension org.eclipse.pde.ui.model.plugin.IPluginExtension
	 */
	void add(IPluginExtensionPoint extension) throws CoreException;
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
	 */
	void remove(IPluginExtension extension) throws CoreException;
	/**
	 * Removes an extension point from this object. This
	 * method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param extensionPoint the extension point object
	 */
	void remove(IPluginExtensionPoint extensionPoint) throws CoreException;
	/**
	 * Swaps the positions of the provided extensions
	 * in the list of extensions.
	 *
	 * @param e1 the first extension object
	 * @param e2 the second extension object
	 */
	void swap(IPluginExtension e1, IPluginExtension e2) throws CoreException;
	/**
	 * Returns the position of the extension in the receiver.
	 * @param e the extension
	 * @return the 0-based index of the extension in the receiver.
	 */
	int getIndexOf(IPluginExtension e);	
}
