package org.eclipse.pde.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IIdentifiable;
/**
 * Object that implement this interface represent references of
 * plug-ins. Plug-ins are referenced using their identifiers,
 * and optionally versions and match rules.
 */
public interface IPluginReference extends IIdentifiable, IMatchRules {
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
	 * Sets the match type for the require plug-in.
	 * This method will throw a CoreException if the model
	 * is not editable.
	 * @see IMatchRules
	 * @param match the desired match type
	 */
	public void setMatch(int match) throws CoreException;
	/**
	 * Sets the desired version of the required plug-in.
	 * This method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param version the required import plug-in version
	 */
	public void setVersion(String version) throws CoreException;
}