/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;

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
		return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(WizardMessages.ProjectUpdateChange_project_not_accessible, new String[] {fProject.getName()}));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		IProjectDescription description = fProject.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = ApiPlugin.NATURE_ID;
		description.setNatureIds(newNatures);
		fProject.setDescription(description, pm);
		if(!pm.isCanceled()) {
			pm.worked(1);
		}
		return null;
	}
}
