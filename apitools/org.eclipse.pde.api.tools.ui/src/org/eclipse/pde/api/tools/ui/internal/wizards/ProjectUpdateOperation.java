/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.scanner.ApiDescriptionProcessor;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import com.ibm.icu.text.MessageFormat;

/**
 * Update operation to add source tags to sources in the workspace
 * 
 * @since 1.0.0
 */
public class ProjectUpdateOperation extends WorkspaceModifyOperation {

	private IProject[] projects = null;
	private boolean removecxml = false;
	
	/**
	 * Constructor
	 * @param projects
	 */
	public ProjectUpdateOperation(final IProject[] projects, boolean removecxml) {
		this.projects = projects;
		this.removecxml = removecxml;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		if(monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(WizardMessages.UpdateJavadocTagsWizardPage_1, projects.length);
		try {
			for(int i = 0; i < projects.length; i++) {
				try {
					monitor.subTask(MessageFormat.format(WizardMessages.UpdateJavadocTagsWizardPage_2, new String[] {projects[i].getName()}));
					monitor.subTask(MessageFormat.format(WizardMessages.ProjectUpdateOperation_0, new String[] {projects[i].getName()}));
					convertProject(projects[i], monitor);
					monitor.subTask(MessageFormat.format(WizardMessages.ProjectUpdateOperation_1, new String[] {projects[i].getName()}));
					IResource cxml = projects[i].findMember(ApiDescriptionProcessor.COMPONENT_XML_NAME);
					if(cxml != null) {
						ApiDescriptionProcessor.updateJavadocTags(JavaCore.create(projects[i]), new File(cxml.getLocationURI()));
						if(removecxml) {
							monitor.subTask(MessageFormat.format(WizardMessages.ProjectUpdateOperation_2, new String[] {projects[i].getName()}));
							cxml.delete(true, monitor);
						}
					}
				}
				catch (CoreException e) {
					ApiUIPlugin.log(e);
				} 
				catch (IOException e) {
					ApiUIPlugin.log(e);
				}
				if(monitor.isCanceled()) {
					return;
				}
				monitor.worked(1);
			}
		}
		finally {
			if(monitor.isCanceled()) {
				return;
			}
			monitor.done();
		}
	}

	/**
	 * Converts a single {@link IProject} to have an Api nature
	 * @param projectToConvert
	 * @param monitor
	 * @throws CoreException
	 */
	private void convertProject(IProject projectToConvert, IProgressMonitor monitor) throws CoreException {
		// Do early checks to make sure we can get out fast if we're not setup
		// properly
		if (projectToConvert == null || !projectToConvert.exists()) {
			return;
		}
		// Nature check - do we need to do anything at all?
		if (projectToConvert.hasNature(ApiPlugin.NATURE_ID)) {
			return;
		}
		addNatureToProject(projectToConvert, ApiPlugin.NATURE_ID, monitor);
	}
	
	/**
	 * Adds the Api project nature to the given {@link IProject}
	 * @param proj
	 * @param natureId
	 * @param monitor
	 * @throws CoreException
	 */
	public static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}
	
}
