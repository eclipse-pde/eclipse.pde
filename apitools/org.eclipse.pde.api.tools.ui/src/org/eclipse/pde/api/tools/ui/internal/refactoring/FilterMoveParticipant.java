/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.osgi.util.NLS;

/**
 * Handles a type being moved from one location to another and updates the
 * associated
 * {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter}
 * s as needed
 *
 * @since 1.0.1
 */
public class FilterMoveParticipant extends MoveParticipant {

	private IJavaElement element = null;

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return null;
	}

	@Override
	public Change createPreChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		switch (this.element.getElementType()) {
			case IJavaElement.TYPE: {
				return RefactoringUtils.createDeleteFilterChanges((IType) this.element);
			}
			case IJavaElement.PACKAGE_FRAGMENT: {
				return RefactoringUtils.createDeleteFilterChanges((IPackageFragment) this.element);
			}
			default:
				break;
		}
		return null;
	}

	@Override
	public String getName() {
		return NLS.bind(RefactoringMessages.FilterDeleteParticipant_remove_unused_filters_for_0, this.element.getElementName());
	}

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IJavaElement) {
			this.element = (IJavaElement) element;
			return true;
		}
		return false;
	}
}
