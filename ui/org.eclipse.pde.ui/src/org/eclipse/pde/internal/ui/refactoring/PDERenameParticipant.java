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
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

public abstract class PDERenameParticipant extends RenameParticipant {
	
	protected IProject fProject;
	protected IJavaElement fElement;

	public RefactoringStatus checkConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		if (!getArguments().getUpdateReferences())
			return null;
		CompositeChange result = new CompositeChange(getName());
		addBundleManifestChange(result, pm);
		addChange(result, "plugin.xml", pm); //$NON-NLS-1$
		addChange(result, "fragment.xml", pm); //$NON-NLS-1$
		return (result.getChildren().length == 0) ? null : result;
	}
	
	private void addChange(CompositeChange result, String filename, IProgressMonitor pm)
			throws CoreException {
		IFile file = fProject.getFile(filename);
		if (file.exists()) {
			Change change = PluginManifestChange.createRenameChange(
					file, getOldName(), getNewName(), pm);
			if (change != null)
				result.add(change);
		}
	}
	
	protected String getOldName() {
		return fElement.getElementName();
	}
	
	protected String getNewName() {
		return getArguments().getNewName();
	}

	private void addBundleManifestChange(CompositeChange result, IProgressMonitor pm)
			throws CoreException {
		IFile file = fProject.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		if (file.exists()) {
			Change change = BundleManifestChange.createRenameChange(file,
								fElement, getNewName(), pm);
			if (change != null)
				result.add(change);
		}
	}

}
