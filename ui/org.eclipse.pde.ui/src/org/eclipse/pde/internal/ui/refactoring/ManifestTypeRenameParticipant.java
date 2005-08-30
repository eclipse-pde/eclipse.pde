/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.pde.internal.core.WorkspaceModelManager;

public class ManifestTypeRenameParticipant extends RenameParticipant {
	
	private IProject fProject;
	private IType fType;

	protected boolean initialize(Object element) {
		if (element instanceof IType) {
			IType type = (IType)element;
			IJavaProject javaProject = (IJavaProject)type.getAncestor(IJavaElement.JAVA_PROJECT);
			IProject project = javaProject.getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				fProject = javaProject.getProject();
				fType = type;
				return true;
			}
		}
		return false;
	}

	public String getName() {
		return "Manifest Rename Type Participant";
	}

	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) 
			throws OperationCanceledException {
		return new RefactoringStatus();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange result = new CompositeChange("Rename classes referenced in plug-in manifest files");
		addBundleManifestChange(result, pm);
		addChange(result, "plugin.xml", pm); //$NON-NLS-1$
		addChange(result, "fragment.xml", pm); //$NON-NLS-1$
		return (result.getChildren().length == 0) ? null : result;
	}
	
	private void addChange(CompositeChange result, String filename, IProgressMonitor pm) throws CoreException {
		IFile file = fProject.getFile(filename);
		if (file.exists()) {
			Change change = PluginManifestChange.createChange(file, fType, getArguments().getNewName(), pm);
			if (change != null)
				result.add(change);
		}	
	}
	
	private void addBundleManifestChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		IFile file = fProject.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		if (file.exists()) {
			Change change = BundleManifestChange.createChange(file, fType, getArguments().getNewName(), pm);
			if (change != null)
				result.add(change);
		}	
	}

}
