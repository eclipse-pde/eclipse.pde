/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.search.dependencies;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.search.ui.NewSearchUI;

public class DependencyExtentAction extends Action {

	private final IProject fProject;

	private final String fImportID;

	public DependencyExtentAction(IProject project, String importID) {
		fProject = project;
		fImportID = importID;
		setText(PDEUIMessages.DependencyExtentAction_label);
	}

	@Override
	public void run() {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInBackground(new DependencyExtentQuery(fProject, fImportID));
	}

}
