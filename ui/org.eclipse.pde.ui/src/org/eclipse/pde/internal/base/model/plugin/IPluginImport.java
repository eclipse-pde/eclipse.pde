package org.eclipse.pde.internal.base.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.IIdentifiable;
/**
 */
public interface IPluginImport extends IPluginObject, IIdentifiable {
/**
 * A name of the property that will be used to notify
 * about changes in the "reexported" field.
 */
	public static final String P_REEXPORTED = "reexported";
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
 * No rule.
 */
	int NONE = 0;
/**
 * An perfect match.
 */
	int PERFECT = 1;
/**
 * A match that is equivalent to the required version.
 */
	int EQUIVALENT = 2;
/**
 * A match that is compatible with the required version.
 */
	int COMPATIBLE = 3;
/**
 * A match requires that a version is greater or equal to the
 * specified version.
 */
	int GREATER_OR_EQUAL = 4;
/**
 * Returns the required match for the imported plug-in. The
 * choices are: PERFECT, EQUIVALENT, COMPATIBLE and 
 * GREATER_OR_EQUAL.
 *
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
 * Sets the match type for the require plug-in (EXACT or COMPATIBLE).
 * This method will throw a CoreException if the model
 * is not editable.
 *
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
 * Sets the desired version of the required plug-in.
 * This method will throw a CoreException if
 * the model is not editable.
 *
 * @param version the required import plug-in version
 */
public void setVersion(String version) throws CoreException;
}
