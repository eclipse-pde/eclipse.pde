/*******************************************************************************
 *  Copyright (c) 2026 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

public abstract class PDEDeleteParticipant extends DeleteParticipant implements ISharableParticipant {

	protected IProject fProject;
	protected HashMap<Object, String> fElements;

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public void addElement(Object element, RefactoringArguments arguments) {
		if (element instanceof IType type) {
			fElements.put(element, type.getFullyQualifiedName());
		} else if (element instanceof IPackageFragment pkg) {
			fElements.put(element, pkg.getElementName());
		}
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange result = new CompositeChange(getName());
		addChange(result, pm);
		return (result.getChildren().length == 0) ? null : result;
	}

	/**
	 * @throws CoreException may be thrown by overrides
	 */
	protected abstract void addChange(CompositeChange result, IProgressMonitor pm) throws CoreException;

}