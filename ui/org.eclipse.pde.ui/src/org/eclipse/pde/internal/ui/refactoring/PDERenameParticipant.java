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

import java.util.HashMap;
import java.util.Iterator;

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
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

public abstract class PDERenameParticipant extends RenameParticipant implements ISharableParticipant {
	
	protected IProject fProject;
	protected HashMap fElements;

	public RefactoringStatus checkConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}
	
	public void addElement(Object element, RefactoringArguments arguments) {
		fElements.put(element, ((RenameArguments)arguments).getNewName());
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
					file, getOldNames(), getNewNames(), pm);
			if (change != null)
				result.add(change);
		}
	}
	
	protected String[] getOldNames() {
		String[] result = new String[fElements.size()];
		Iterator iter = fElements.keySet().iterator();
		for (int i = 0; i < fElements.size(); i++)
			result[i] = ((IJavaElement)iter.next()).getElementName();
		return result;
	}
	
	protected String[] getNewNames() {
		String[] result = new String[fElements.size()];
		Iterator iter = fElements.values().iterator();
		for (int i = 0; i < fElements.size(); i++)
			result[i] = iter.next().toString();
		return result;
	}

	protected void addBundleManifestChange(CompositeChange result, IProgressMonitor pm)
			throws CoreException {
		addBundleManifestChange(fProject.getFile("META-INF/MANIFEST.MF"), result, pm); //$NON-NLS-1$
	}
	
	protected void addBundleManifestChange(IFile file, CompositeChange result, 
			IProgressMonitor pm) throws CoreException {
		if (file.exists()) {
			Change change = BundleManifestChange.createRenameChange(
							file, 
							(IJavaElement[])fElements.keySet().toArray(new IJavaElement[fElements.size()]),
							getNewNames(), 
							pm);
			if (change != null)
				result.add(change);
		}
	}

}
