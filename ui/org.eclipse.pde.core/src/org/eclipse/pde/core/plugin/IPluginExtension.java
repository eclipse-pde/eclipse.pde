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

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;

/**
 * Classes that implement this interface model the extension
 * element found in the plug-in or fragment manifest.
 */
public interface IPluginExtension extends IPluginParent, IIdentifiable {
	/**
	 * A name of the property that will be used to
	 * notify about the "point" change
	 */
	String P_POINT = "point";
	/**
	 * Returns the full Id of the extension point that this extension
	 * is plugged into.
	 */
	String getPoint();
	/**
	 * Returns the schema for the extension point that this extension
	 * is plugged into or <samp>null</samp> if not found.
	 * <p>This method is an implementation detail - schema object
	 * is not needed for clients outside PDE and should not be used.
	 */
	Object getSchema();
	/**
	 * Sets the value of the extension point Id
	 * This method will throw a CoreException if
	 * this model is not editable.
	 * @param point the new extension point Id
	 */
	void setPoint(String point) throws CoreException;
}
