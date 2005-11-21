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

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

public abstract class PDEMoveParticipant extends MoveParticipant implements ISharableParticipant {

	protected IProject fProject;
	protected ArrayList fElements;

	public RefactoringStatus checkConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}
	
	public void addElement(Object element, RefactoringArguments arguments) {
		fElements.add(element);
	}

	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		CompositeChange result = new CompositeChange(getName());
		addBundleManifestChange(result, pm);
		if (isInterestingForExtensions()) {
			addChange(result, "plugin.xml", pm); //$NON-NLS-1$
			addChange(result, "fragment.xml", pm); //$NON-NLS-1$
		}
		return (result.getChildren().length == 0) ? null : result;
	}
	
	protected abstract boolean isInterestingForExtensions();
	
	protected void addChange(CompositeChange result, String filename, IProgressMonitor pm)
		throws CoreException {		
	}
	
	protected void addBundleManifestChange(CompositeChange result, IProgressMonitor pm)
		throws CoreException {
	}

}
