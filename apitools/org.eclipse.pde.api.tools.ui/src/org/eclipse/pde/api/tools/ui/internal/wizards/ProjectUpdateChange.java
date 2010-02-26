/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.pde.api.tools.internal.ApiDescriptionManager;
import org.eclipse.pde.api.tools.internal.builder.BuildState;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;

import com.ibm.icu.text.MessageFormat;

/**
 * Provides a {@link Change} for updating the nature, etc of a project
 * 
 * @since 1.0.0
 */
public class ProjectUpdateChange extends Change {

	private IProject fProject = null;
	
	/**
	 * Constructor
	 * 
	 * @param project the project to configure
	 */
	public ProjectUpdateChange(IProject project) {
		fProject = project;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#getModifiedElement()
	 */
	public Object getModifiedElement() {
		return fProject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#getName()
	 */
	public String getName() {
		return MessageFormat.format(WizardMessages.ProjectUpdateChange_add_nature_and_builder, new String[] {});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#initializeValidationData(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void initializeValidationData(IProgressMonitor pm) {}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#isValid(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if(fProject.isAccessible()) {
			return RefactoringStatus.create(Status.OK_STATUS);
		}
		return RefactoringStatus.createErrorStatus(MessageFormat.format(WizardMessages.ProjectUpdateChange_project_not_accessible, new String[] {fProject.getName()}));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		SubMonitor localmonitor = SubMonitor.convert(pm);
		localmonitor.beginTask(IApiToolsConstants.EMPTY_STRING, 1);
		localmonitor.setTaskName(WizardMessages.ProjectUpdateChange_adding_nature_and_builder);
		IProjectDescription description = this.fProject.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = ApiPlugin.NATURE_ID;
		description.setNatureIds(newNatures);
		this.fProject.setDescription(description, localmonitor);
		IJavaProject javaProject = JavaCore.create(this.fProject);
		// make sure we get rid of the previous api description file
		ApiDescriptionManager.getManager().clean(javaProject, true, true);
		// we want a full build of the converted project next time a build is triggered
		if (ResourcesPlugin.getWorkspace().isAutoBuilding()) {
			Util.getBuildJob(new IProject[] { this.fProject }).schedule();
		} else {
			/*
			 * If autobuild is off, clear the last build state to force a full build of
			 * this project on the next build.
			 */
			BuildState.setLastBuiltState(this.fProject, null);
		}
		Util.updateMonitor(localmonitor, 1);
		return null;
	}
}
