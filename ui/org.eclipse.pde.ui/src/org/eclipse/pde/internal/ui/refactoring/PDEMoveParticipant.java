/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashMap;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.ltk.core.refactoring.participants.*;
import org.eclipse.pde.internal.core.project.PDEProject;

public abstract class PDEMoveParticipant extends MoveParticipant implements ISharableParticipant {

	protected IProject fProject;
	protected HashMap<Object, String> fElements;

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public void addElement(Object element, RefactoringArguments arguments) {
		Object destination = ((MoveArguments) arguments).getDestination();
		fElements.put(element, getNewName(destination, element));
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange result = new CompositeChange(getName());
		addChange(result, pm);
		if (isInterestingForExtensions()) {
			addChange(result, PDEProject.getPluginXml(fProject), pm);
			addChange(result, PDEProject.getFragmentXml(fProject), pm);
		}
		return (result.getChildren().length == 0) ? null : result;
	}

	protected abstract boolean isInterestingForExtensions();

	/**
	 * @param result
	 * @param file
	 * @param pm
	 * @throws CoreException
	 */
	protected void addChange(CompositeChange result, IFile file, IProgressMonitor pm) throws CoreException {
	}

	/**
	 * @param result
	 * @param pm
	 * @throws CoreException
	 */
	protected void addChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
	}

	protected String getNewName(Object destination, Object element) {
		return element.toString();
	}

	protected String[] getNewNames() {
		String[] result = new String[fElements.size()];
		Iterator<String> iter = fElements.values().iterator();
		for (int i = 0; i < fElements.size(); i++)
			result[i] = iter.next().toString();
		return result;
	}

}
