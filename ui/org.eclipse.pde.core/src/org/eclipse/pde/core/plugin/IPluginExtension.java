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
import org.eclipse.pde.core.IIdentifiable;

/**
 * Classes that implement this interface model the extension
 * element found in the plug-in or fragment manifest.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IPluginExtension extends IPluginParent, IIdentifiable {
	/**
	 * A name of the property that will be used to
	 * notify about the "point" change
	 */
	String P_POINT = "point"; //$NON-NLS-1$

	/**
	 * Returns the full ID of the extension point that this extension
	 * is plugged into.
	 * 
	 * @return the full extension point ID  
	 */
	String getPoint();

	/**
	 * Returns the schema for the extension point that this extension
	 * is plugged into or <code>null</code> if not found.
	 * <p>This method is an implementation detail - schema object
	 * is not needed for clients outside PDE and should not be used.
	 * 
	 * @return The schema for the associated extension point or <code>null</code>
	 */
	Object getSchema();

	/**
	 * Sets the value of the extension point Id
	 * This method will throw a CoreException if
	 * this model is not editable.
	 * @param point the new extension point Id
	 * @throws CoreException if the model is not editable
	 */
	void setPoint(String point) throws CoreException;
}
