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
import org.eclipse.pde.core.*;
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
	String P_MATCH = "match"; //$NON-NLS-1$
	/**
	 * A name of the property that will be used to notify
	 * about changes in the "version" field.
	 */
	String P_VERSION = "version"; //$NON-NLS-1$
	/**
	 * Returns the required match for the imported plug-in. The
	 * choices are defined in IMatchRules interface.
	 * @see IMatchRules
	 * @return the desired type of the import plug-in match
	 */
	int getMatch();
	/**
	 * Returns the required version of the plug-in.
	 *
	 * @return required version or <samp>null</samp> if not set
	 */
	String getVersion();
	/**
	 * Sets the match type for the require plug-in.
	 * This method will throw a CoreException if the model
	 * is not editable.
	 * @see IMatchRules
	 * @param match the desired match type
	 */
	void setMatch(int match) throws CoreException;
	/**
	 * Sets the desired version of the required plug-in.
	 * This method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param version the required import plug-in version
	 */
	void setVersion(String version) throws CoreException;
}
