/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;


import org.eclipse.core.resources.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.search.ui.*;

public class DependencyExtentAction extends Action {
	
	private static final String KEY_COMPUTE = "DependencyExtentAction.label"; //$NON-NLS-1$
	
	private IProject fProject;

	private String fImportID;
	
	public DependencyExtentAction(IProject project, String importID) {
		fProject = project;
		fImportID = importID;
		setText(PDEPlugin.getResourceString(KEY_COMPUTE));
	}
		
	public void run() {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInBackground(new DependencyExtentQuery(fProject, fImportID));
	}	
	
}
