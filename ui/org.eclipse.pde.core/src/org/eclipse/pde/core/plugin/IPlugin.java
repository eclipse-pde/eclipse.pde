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
/**
 * A model object that represents the content of the plugin.xml
 * file.
 */
public interface IPlugin extends IPluginBase {
	/**
	 * A property that will be used when "className"
	 * field is changed.
	 */
	String P_CLASS_NAME = "class"; //$NON-NLS-1$

	/**
	 * Returns a plug-in class name.
	 * @return plug-in class name or <samp>null</samp> if not specified.
	 */
	String getClassName();

	/**
	 * Sets the name of the plug-in class.
	 * This method will throw a CoreException
	 * if the model is not editable.
	 *
	 * @param className the new class name
	 */
	void setClassName(String className) throws CoreException;
}
