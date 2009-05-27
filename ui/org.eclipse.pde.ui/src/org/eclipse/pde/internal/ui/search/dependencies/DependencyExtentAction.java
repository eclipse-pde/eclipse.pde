/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

	private IProject fProject;

	private String fImportID;

	public DependencyExtentAction(IProject project, String importID) {
		fProject = project;
		fImportID = importID;
		setText(PDEUIMessages.DependencyExtentAction_label);
	}

	public void run() {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInBackground(new DependencyExtentQuery(fProject, fImportID));
	}

}
