/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;


import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.SearchUI;

public class DependencyExtentAction extends Action {
	
	private static final String KEY_COMPUTE = "DependencyExtentAction.label";
	
	DependencyExtentSearchOperation op;
	
	public DependencyExtentAction(IPluginImport object) {
		op = new DependencyExtentSearchOperation(object);
		setText(PDEPlugin.getResourceString(KEY_COMPUTE));
	}
		
	
	public void run() {
		try {
			SearchUI.activateSearchResultView();
			PDEPlugin.getWorkspace().run(
				op,
				null,
				IWorkspace.AVOID_UPDATE,
				new NullProgressMonitor());
		} catch (CoreException e) {
		}
	}
	
	
	
}
