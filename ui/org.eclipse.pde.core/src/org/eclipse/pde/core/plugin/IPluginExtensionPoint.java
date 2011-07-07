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
 * Classes that implement this interface model an extension point
 * element specified in the plug-in manifest.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IPluginExtensionPoint extends IPluginObject, IIdentifiable {
	/**
	 * A property name that will be used to notify
	 * about changes to the schema value.
	 */
	String P_SCHEMA = "schema"; //$NON-NLS-1$

	/**
	 * Returns the full extension point Id that
	 * is composed as "pluginId.pointId". This full
	 * Id will be used by extensions to reference this
	 * extension point.
	 *
	 * @return a full extension point Id
	 */
	String getFullId();

	/**
	 * Returns the name of the extension point XML schema
	 * that defines this extension point.
	 *
	 * @return XML extension point schema file name
	 */
	String getSchema();

	/**
	 * Sets the plug-in relative name of
	 * the extension point schema file that
	 * describes this extension point.
	 * This method will throw a CoreException
	 * if the model is not editable.
	 * 
	 * @param schema the schema file name
	 * @throws CoreException if the model is not editable
	 */
	void setSchema(String schema) throws CoreException;
}
