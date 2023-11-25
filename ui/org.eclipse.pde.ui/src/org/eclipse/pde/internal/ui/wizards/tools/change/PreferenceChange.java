/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools.change;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceChange extends Change {

	private IEclipsePreferences node;

	public PreferenceChange(IEclipsePreferences node) {
		this.node = node;
	}

	@Override
	public String getName() {
		return NLS.bind(PDEUIMessages.ProjectUpdateChange_set_pde_preference, PDEProject.BUNDLE_ROOT_PATH);
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {

	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		node.put(PDEProject.BUNDLE_ROOT_PATH, ""); //$NON-NLS-1$
		try {
			node.flush();
		} catch (BackingStoreException e) {
		}
		return null;
	}

	@Override
	public Object getModifiedElement() {
		return node;
	}
}
