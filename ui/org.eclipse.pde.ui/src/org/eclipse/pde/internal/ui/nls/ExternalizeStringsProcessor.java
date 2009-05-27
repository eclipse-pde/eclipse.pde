/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.*;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.ltk.core.refactoring.participants.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ExternalizeStringsProcessor extends RefactoringProcessor {

	Object[] fChangeFiles = null;

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		if (fChangeFiles == null)
			status.addFatalError(PDEUIMessages.ExternalizeStringsProcessor_errorMessage);
		return status;
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return null;
	}

	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange change = new CompositeChange(""); //$NON-NLS-1$
		change.markAsSynthetic();
		ExternalizeStringsOperation op = new ExternalizeStringsOperation(fChangeFiles, change);
		try {
			op.run(pm);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
		return change;
	}

	public Object[] getElements() {
		return fChangeFiles;
	}

	public String getIdentifier() {
		return getClass().getName();
	}

	public String getProcessorName() {
		return PDEUIMessages.ExternalizeStringsWizard_title;
	}

	public boolean isApplicable() throws CoreException {
		return true;
	}

	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	public void setChangeFiles(Object[] changeFiles) {
		fChangeFiles = changeFiles;
	}

}
