/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

/**
 * This interface is used to insulate the client's wizards from the master
 * wizard that is responsible for creating the new project. Clients use this
 * interface to ask for the new project's name (without forcing the project
 * creation) and the project handle itself. Content wizards can use the project
 * name to construct default values for other name properties before the project
 * resource is being created.
 */
public interface IProjectProvider {
	/**
	 * Returns the new plug-in project handle. This method will cause project
	 * creation if not created already.
	 * 
	 * @return the handle of the new plug-in project
	 */
	IProject getProject();

	/**
	 * Returns the name of the plug-in project that will be created. This method
	 * can be called at any time without forcing the project resource creation.
	 * 
	 * @return new project name
	 */
	String getProjectName();

	/**
	 * Returns an absolute path of the new plug-in project that will be created.
	 * This method can be called at any time without forcing the project
	 * resource creation.
	 * 
	 * @return absolute project location path
	 */
	IPath getLocationPath();
}
