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
 * Objects that implement this interface represent references
 * to required plug-ins.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IPluginImport extends IPluginObject, IPluginReference {
	/**
	 * A name of the property that will be used to notify
	 * about changes in the "reexported" field.
	 */
	String P_REEXPORTED = "export"; //$NON-NLS-1$
	/**
	 * A name of the property that will be used to notify
	 * about changes in the "optional" field.
	 */
	String P_OPTIONAL = "optional"; //$NON-NLS-1$

	/**
	 * Tests whether the imported plug-in is reexported for
	 * plug-ins that will use this plug-in.
	 *
	 * @return true if the required plug-in libraries are reexported
	 */
	boolean isReexported();

	/**
	 * Tests whether this import is optional. Optional imports will
	 * not create an error condition when they cannot be resolved.
	 *
	 * @return true if this import is optional
	 */
	boolean isOptional();

	/**
	 * Sets whether the libraries of the required plug-in will
	 * be reexported.
	 * This method will throw a CoreException if the model
	 * is not editable.
	 *
	 * @param value true if reexporting is desired
	 * @throws CoreException if the model is not editable
	 */
	void setReexported(boolean value) throws CoreException;

	/**
	 * Sets whether this import is optional. Optional imports will
	 * not create an error condition when they cannot be resolved.
	 *
	 * @param value true if import is optional
	 * @throws CoreException if the model is not editable
	 */
	void setOptional(boolean value) throws CoreException;

}
