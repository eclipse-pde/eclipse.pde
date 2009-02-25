/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
 * A model object that represents the content of the fragment.xml
 * file.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IFragment extends IPluginBase {
	/**
	 * A property that will be used to notify
	 * that a plugin id has changed.
	 */
	String P_PLUGIN_ID = "plugin-id"; //$NON-NLS-1$
	/**
	 * A property that will be used to notify
	 * that a plugin version has changed.
	 */
	String P_PLUGIN_VERSION = "plugin-version"; //$NON-NLS-1$
	/**
	 * A property that will be used to notify
	 * that a plugin version match rule has changed.
	 */
	String P_RULE = "match"; //$NON-NLS-1$

	/**
	 * Returns the id of the fragment host.
	 * 
	 * @return the host id
	 */
	String getPluginId();

	/**
	 * Returns the version of the fragment host.
	 * 
	 * @return the host version
	 */
	String getPluginVersion();

	/**
	 * Returns an optional version match rule as defined in
	 * IMatchRule interface.
	 * 
	 * @return the match rule
	 */
	int getRule();

	/**
	 * Sets the id of the plug-in that will be the target of this fragment.
	 * @param id the id of the referenced plug-in.
	 * @exception org.eclipse.core.runtime.CoreException attempts to modify a read-only fragment will result in an exception
	 */
	void setPluginId(String id) throws CoreException;

	/**
	 * Sets the version of the plug-in that will be the target of this fragment.'
	 * @param version the version of the referenced version.
	 * @exception org.eclipse.core.runtime.CoreException attempts to modify a read-only fragment will result in an exception
	 */
	void setPluginVersion(String version) throws CoreException;

	/**
	 * Sets the optional version match rule as defined in IMatchRules. This
	 * rule will be used when attempting to match the referenced plug-in
	 * version.
	 * @param rule the match rule to be used when locating the referenced the plug-in.
	 * @exception org.eclipse.core.runtime.CoreException attempts to modify a read-only fragment will result in an exception
	 */
	void setRule(int rule) throws CoreException;
}
