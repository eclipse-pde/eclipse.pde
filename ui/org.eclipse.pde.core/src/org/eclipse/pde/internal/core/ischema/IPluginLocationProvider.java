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
package org.eclipse.pde.internal.core.ischema;

import org.eclipse.core.runtime.IPath;

/**
 * Classes that implement this interface are responsible for
 * providing absolute path of the plug-in given its identifier.
 */
public interface IPluginLocationProvider {
/**
 * Returns the absolute path of the plug-in with the provided id.
 * @param pluginId the identifier of the plug-in for which a location
 * is needed
 * @return the location of the plug-in or <code>null</code> if
 * plug-in with the required id cannot be found.
 */
	IPath getPluginLocation(String pluginId);
}
