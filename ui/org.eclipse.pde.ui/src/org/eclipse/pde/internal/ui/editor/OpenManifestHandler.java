/*******************************************************************************
 *  Copyright (c) 2006, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt (alex_blewitt@yahoo.com) - contributed a patch for:
 *       o Add an 'Open Manifest' to projects to open the manifest editor
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=133692)
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.HashSet;
import java.util.Iterator;
import org.eclipse.core.commands.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Command handler that opens the manifest file for the plug-in in the
 * manifest editor.
 */
public class OpenManifestHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			Iterator it = ssel.iterator();
			final HashSet projects = new HashSet();
			while (it.hasNext()) {
				Object element = it.next();
				IProject proj = null;
				if (element instanceof IFile)
					proj = ((IFile) element).getProject();
				if ((proj == null) && (element instanceof IProject))
					proj = (IProject) element;
				if ((proj == null) && (element instanceof IAdaptable)) {
					IResource resource = (IResource) ((IAdaptable) element).getAdapter(IResource.class);
					if (resource != null) {
						proj = resource.getProject();
					}
					if (proj == null) {
						IWorkbenchAdapter workbenchAdapter = (IWorkbenchAdapter) ((IAdaptable) element).getAdapter(IWorkbenchAdapter.class);
						if (workbenchAdapter != null) {
							Object o = workbenchAdapter.getParent(element);
							if (o instanceof IAdaptable) {
								resource = (IResource) ((IAdaptable) o).getAdapter(IResource.class);
								if (resource != null) {
									proj = resource.getProject();
								}
							}
						}
					}
				}

				if (proj != null && WorkspaceModelManager.isPluginProject(proj))
					projects.add(proj);
			}
			if (projects.size() > 0) {
				BusyIndicator.showWhile(PDEPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
					public void run() {
						Iterator it = projects.iterator();
						while (it.hasNext()) {
							IProject project = (IProject) it.next();
							IFile file = PDEProject.getManifest(project);
							if (file == null || !file.exists())
								file = PDEProject.getPluginXml(project);
							if (file == null || !file.exists())
								file = PDEProject.getFragmentXml(project);
							if (file == null || !file.exists())
								MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.OpenManifestsAction_title, NLS.bind(PDEUIMessages.OpenManifestsAction_cannotFind, project.getName()));
							else
								try {
									IDE.openEditor(PDEPlugin.getActivePage(), file, IPDEUIConstants.MANIFEST_EDITOR_ID);
								} catch (PartInitException e) {
									MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.OpenManifestsAction_title, NLS.bind(PDEUIMessages.OpenManifestsAction_cannotOpen, project.getName()));
								}
						}
					}
				});
			} else
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.OpenManifestsAction_title, PDEUIMessages.OpenManifestAction_noManifest);
		}
		return null;
	}
}
