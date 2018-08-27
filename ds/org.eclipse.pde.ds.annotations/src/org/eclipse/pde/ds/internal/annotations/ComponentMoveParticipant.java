/*******************************************************************************
 * Copyright (c) 2015, 2016 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

public class ComponentMoveParticipant extends MoveParticipant implements ISharableParticipant, ComponentRefactoringParticipant {

	private final ComponentRefactoringHelper helper = new ComponentRefactoringHelper(this);

	@Override
	protected boolean initialize(Object element) {
		return helper.initialize(element);
	}

	@Override
	public String getName() {
		return Messages.ComponentMoveParticipant_name;
	}

	public void addElement(Object element, RefactoringArguments arguments) {
		helper.addElement(element, arguments);
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		return helper.checkConditions(pm, context);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return helper.createChange(pm);
	}

	public String getComponentNameRoot(IJavaElement element, RefactoringArguments args) {
		IType type = (IType) element;
		String compName = type.getFullyQualifiedName();
		Object destination = ((MoveArguments) args).getDestination();
		if (destination instanceof IPackageFragment) {
			compName = String.format("%s.%s", ((IPackageFragment) destination).getElementName(), type.getElementName()); //$NON-NLS-1$
		} else if (destination instanceof IType) {
			compName = ((IType) destination).getType(type.getElementName()).getFullyQualifiedName();
		}

		return compName;
	}
}
