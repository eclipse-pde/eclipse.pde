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

	@Override
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
