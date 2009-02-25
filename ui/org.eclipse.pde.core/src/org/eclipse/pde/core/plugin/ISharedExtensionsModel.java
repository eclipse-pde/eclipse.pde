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
 * This type of model is created by parsing the plug-in 
 * manifest file but only takes the extensions and extension
 * points into account.  
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISharedExtensionsModel extends ISharedPluginModel {
	/**
	 * Returns a top-level model object. Equivalent to
	 * calling <pre>getPluginBase(true)</pre>.
	 * @return a top-level model object representing a plug-in or a fragment.
	 */
	IExtensions getExtensions();

	/**
	 * Returns a top-level model object.
	 * @param createIfMissing if true, root model object will
	 * be created if not defined.
	 * @return a top-level model object
	 */
	IExtensions getExtensions(boolean createIfMissing);
}
