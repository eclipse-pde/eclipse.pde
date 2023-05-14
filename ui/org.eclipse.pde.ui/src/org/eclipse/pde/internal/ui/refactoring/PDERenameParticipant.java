/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.pde.internal.core.project.PDEProject;

public abstract class PDERenameParticipant extends RenameParticipant implements ISharableParticipant {

	protected IProject fProject;
	protected HashMap<Object, String> fElements;

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public void addElement(Object element, RefactoringArguments arguments) {
		String newName = ((RenameArguments) arguments).getNewName();
		if (element instanceof IResource) {
			IPath projectPath = ((IResource) element).getProjectRelativePath();
			newName = projectPath.removeLastSegments(1).append(newName).toString();
		}
		fElements.put(element, newName);
	}

	@Override
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
		Iterator<String> iter = fElements.values().iterator();
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
		for (Object obj : objs)
			if (obj instanceof IJavaElement == javaElement)
				return true;
		return false;
	}
}
