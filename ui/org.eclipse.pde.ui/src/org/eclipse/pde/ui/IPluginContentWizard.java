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
package org.eclipse.pde.ui;
import org.eclipse.pde.ui.templates.*;
/**
 * Clients should implement this interface if they want to provide a wizard that
 * will define the initial content of a new plug-in project. These wizards set
 * up folder structure, create the manifest file and the top-level Java class.
 * They can also create any number of additional files and folders if the
 * purpose of the plug-in warrants it.
 */
public interface IPluginContentWizard extends IBasePluginWizard {
	/**
	 * Initializes the wizard with the parent wizard instance, the project
	 * provider and the structure data. These objects should provide enough
	 * context for the wizard to set up required Java build path (for all the
	 * plug-ins that may be required), generate required plug-in folder
	 * structure and create initial files.
	 *  
	 */
	void init(String id);
/**
 * TODO add javadoc
 * @return
 */
	String getPluginId();
/**
 * TODO add javadoc
 * @return
 */
	ITemplateSection[] getTemplateSections();
}