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
 * A model object that represents the content of the plugin.xml
 * file.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IPlugin extends IPluginBase {
	/**
	 * A property that will be used when the plug-in activator
	 * field is changed.
	 */
	String P_CLASS_NAME = "class"; //$NON-NLS-1$

	/**
	 * Returns a plug-in activator class name
	 * @return plug-in activator class name or <samp>null</samp> if not specified.
	 */
	String getClassName();

	/**
	 * Sets the name of the plug-in activator class.
	 * This method will throw a CoreException
	 * if the model is not editable.
	 *
	 * @param className the new class name
	 * @throws CoreException if the model is not editable
	 */
	void setClassName(String className) throws CoreException;

}
