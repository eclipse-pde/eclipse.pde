/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
/**
 * Classes that implement this interface model an extension point
 * element specified in the plug-in manifest.
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
	 */
	void setSchema(String schema) throws CoreException;
}
