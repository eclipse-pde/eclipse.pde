package org.eclipse.pde.internal.base.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.IIdentifiable;
/**
 */
public interface IPluginImport extends IPluginObject, IIdentifiable, IMatchRules {
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
 * A name of the property that will be used to notify
 * about changes in the "match" field.
 */
	public static final String P_MATCH = "match";
/**
 * A name of the property that will be used to notify
 * about changes in the "version" field.
 */
	public static final String P_VERSION = "version";
/**
 * Returns the required match for the imported plug-in. The
 * choices are defined in IMatchRules interface.
 * @see IMatchRules
 * @return the desired type of the import plug-in match
 */
public int getMatch();
/**
 * Returns the required version of the plug-in.
 *
 * @return required version or <samp>null</samp> if not set
 */
public String getVersion();
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
 * Sets the match type for the require plug-in.
 * This method will throw a CoreException if the model
 * is not editable.
 * @see IMatchRules
 * @param match the desired match type
 */ 
public void setMatch(int match) throws CoreException;
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
/**
 * Sets the desired version of the required plug-in.
 * This method will throw a CoreException if
 * the model is not editable.
 *
 * @param version the required import plug-in version
 */
public void setVersion(String version) throws CoreException;
}
