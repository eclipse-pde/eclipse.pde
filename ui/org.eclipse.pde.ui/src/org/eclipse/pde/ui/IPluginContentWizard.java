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
 * Clients should implement this interface if they
 * want to provide a wizard that will define the
 * initial content of a new plug-in project.
 * These wizards set up folder structure, create
 * the manifest file and the top-level Java class.
 * They can also create any number of additional
 * files and folders if the purpose of
 * the plug-in warrants it.
	 * <p>
	 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
 */
public interface IPluginContentWizard extends IBasePluginWizard {
	/**
	 * Initializes the wizard with the parent wizard instance,
	 * the project provider and the structure data.
	 * These objects should provide enough context for the
	 * wizard to set up required Java build path (for all
	 * the plug-ins that may be required), generate
	 * required plug-in folder structure and create
	 * initial files.
	 *
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	void init(String id);
	
	String getPluginId();
	
	ITemplateSection[] getTemplateSections();
}
