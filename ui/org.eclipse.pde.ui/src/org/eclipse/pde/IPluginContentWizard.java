package org.eclipse.pde;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jface.wizard.*;
/**
 * Clients should implement this interface if they
 * want to provide a wizard that will define the
 * initial content of a new plug-in project.
 * These wizards set up folder structure, create
 * the manifest file and the top-level Java class.
 * They can also create any number of additional
 * files and folders if the purpose of
 * the plug-in warrants it.
 * <p>The 
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
	 * @param provider the object to ask for the project
	 * @param structureData provides data about initial plug-in structure
	 * @param fragment true if the new project will host a fragment
	 */
	void init(
		IProjectProvider provider,
		IPluginStructureData structureData,
		boolean fragment);
}