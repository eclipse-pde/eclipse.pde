/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.ltk.core.refactoring.participants.*;
import org.eclipse.pde.internal.core.project.PDEProject;

public abstract class PDERenameParticipant extends RenameParticipant implements ISharableParticipant {

	protected IProject fProject;
	protected HashMap fElements;

	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}

	public void addElement(Object element, RefactoringArguments arguments) {
		String newName = ((RenameArguments) arguments).getNewName();
		if (element instanceof IResource) {
			IPath projectPath = ((IResource) element).getProjectRelativePath();
			newName = projectPath.removeLastSegments(1).append(newName).toString();
		}
		fElements.put(element, newName);
	}

	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (!getArguments().getUpdateReferences())
			return null;
		CompositeChange result = new CompositeChange(getName());
		addBundleManifestChange(result, pm);
		if (updateBuildProperties())
			addBuildPropertiesChange(result, pm);
		addChange(result, PDEProject.getPluginXml(fProject), pm);
		addChange(result, PDEProject.getFragmentXml(fProject), pm);
		return (result.getChildren().length == 0) ? null : result;
	}

	private void addChange(CompositeChange result, IFile file, IProgressMonitor pm) throws CoreException {
		if (file.exists()) {
			Change change = PluginManifestChange.createRenameChange(file, fElements.keySet().toArray(), getNewNames(), getTextChange(file), pm);
			if (change != null)
				result.add(change);
		}
	}

	protected String[] getNewNames() {
		String[] result = new String[fElements.size()];
		Iterator iter = fElements.values().iterator();
		for (int i = 0; i < fElements.size(); i++)
			result[i] = iter.next().toString();
		return result;
	}

	protected void addBundleManifestChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		addBundleManifestChange(PDEProject.getManifest(fProject), result, pm);
	}

	protected void addBundleManifestChange(IFile file, CompositeChange result, IProgressMonitor pm) throws CoreException {
		if (file.exists()) {
			Change change = BundleManifestChange.createRenameChange(file, fElements.keySet().toArray(), getNewNames(), pm);
			if (change != null)
				result.add(change);
		}
	}

	protected void addBuildPropertiesChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		IFile file = PDEProject.getBuildProperties(fProject);
		if (file.exists()) {
			Change change = BuildPropertiesChange.createRenameChange(file, fElements.keySet().toArray(), getNewNames(), pm);
			if (change != null)
				result.add(change);
		}
	}

	protected boolean updateManifest() {
		return containsElement(true);
	}

	protected boolean updateBuildProperties() {
		return containsElement(false);
	}

	protected boolean containsElement(boolean javaElement) {
		Object[] objs = fElements.keySet().toArray();
		for (int i = 0; i < objs.length; i++)
			if (objs[i] instanceof IJavaElement == javaElement)
				return true;
		return false;
	}
}
