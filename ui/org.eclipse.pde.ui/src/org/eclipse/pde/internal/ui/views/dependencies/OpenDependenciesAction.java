/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.core.commands.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.ui.handlers.HandlerUtil;

public class OpenDependenciesAction extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			openDependencies(ssel.getFirstElement());
		}
		return null;
	}

	private void openDependencies(Object el) {
		if (el instanceof IFile) {
			el = ((IFile) el).getProject();
		}
		if (el instanceof IJavaProject) {
			el = ((IJavaProject) el).getProject();
		}
		if (el instanceof IProject) {
			el = PluginRegistry.findModel((IProject) el);
		}
		if (el instanceof IPluginObject) {
			el = ((IPluginObject) el).getModel();
		}
		if (el instanceof IPluginModelBase) {
			new OpenPluginDependenciesAction((IPluginModelBase) el).run();
		}
	}
}
