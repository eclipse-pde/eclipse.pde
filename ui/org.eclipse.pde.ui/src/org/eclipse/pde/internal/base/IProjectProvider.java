package org.eclipse.pde.internal.base;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
/**
 * This interface is used to insulate
 * the client's wizards from the
 * master wizard that is responsible for
 * creating the new project.
 * Clients use this interface
 * to ask for the new project's name
 * (without forcing the project creation)
 * and the project handle itself.
 */
public interface IProjectProvider {
/**
 * Returns the new plug-in project handle.
 *
 * @return the handle of the new plug-in project
 */
IProject getProject();
/**
 * Returns the name of the plug-in project that
 * will be created. This method can be called
 * at any time without forcing project resource creation.
 *
 * @return new project name
 */
String getProjectName();

/**
 * Returns absolute path of the new plug-in 
 * project that will be created. 
 * 
 * @return absolute project path
 */
IPath getLocationPath();
}
