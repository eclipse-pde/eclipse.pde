package org.eclipse.pde.internal.base.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.IIdentifiable;
/**
 */
public interface IPluginImport extends IPluginObject, IPluginReference {
/**
 * A name of the property that will be used to notify
 * about changes in the "reexported" field.
 */
	public static final String P_REEXPORTED = "reexported";
/**
 * A name of the property that will be used to notify
 * about changes in the "optional" field.
 */
	public static final String P_OPTIONAL = "optional";
/**
 * Tests whether the imported plug-in is reexported for
 * plug-ins that will use this plug-in.
 *
 * @return true if the required plug-in libraries are reexported
 */
public boolean isReexported();
/**
 * Tests whether this import is optional. Optional imports will
 * not create an error condition when they cannot be resolved.
 *
 * @return true if this import is optional
 */
public boolean isOptional();
/**
 * Sets whether the libraries of the required plug-in will
 * be reexported.
 * This method will throw a CoreException if the model
 * is not editable.
 *
 * @param value true if reexporting is desired
 */ 
public void setReexported(boolean value) throws CoreException;
/**
 * Sets whether this import is optional. Optional imports will
 * not create an error condition when they cannot be resolved.
 *
 * @param value true if import is optional
 */ 
public void setOptional(boolean value) throws CoreException;
}
