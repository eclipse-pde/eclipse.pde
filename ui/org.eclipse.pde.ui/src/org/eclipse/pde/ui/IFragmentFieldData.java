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
package org.eclipse.pde.ui;

/**
 * In addition to field data from the 'New Project' wizard pages, this interface
 * provides choices made by the user that are unique to creating a new fragment
 * project.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.0
 */
public interface IFragmentFieldData extends IFieldData {
	/**
	 * Referenced plug-in id field
	 * 
	 * @return the id of the fragment's plug-in
	 */
	String getPluginId();

	/**
	 * Referenced plug-in version field
	 * 
	 * @return the version of the fragment's plug-in
	 */
	String getPluginVersion();

	/**
	 * Referenced plug-in version match choice
	 * 
	 * @return the rule for matching the version of the referenced plug-in that
	 *         can be one of the values defined in <code>IMatchRules</code>
	 * 
	 */
	int getMatch();
}
