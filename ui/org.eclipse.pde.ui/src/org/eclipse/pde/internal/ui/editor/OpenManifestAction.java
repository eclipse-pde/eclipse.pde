/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt (alex_blewitt@yahoo.com) - contributed a patch for:
 *       o Add an 'Open Manifest' to projects to open the manifest editor
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=133692)
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;

public class OpenManifestAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;

	public OpenManifestAction() {
		super();
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) fSelection;
			Iterator it = ssel.iterator();
			final ArrayList projects = new ArrayList();
			while (it.hasNext()) {
				Object element = it.next();
				IProject proj = null;
				if (element instanceof IFile)
					proj = ((IFile) element).getProject();
				else if (element instanceof IProject)
					proj = (IProject) element;
				else if (element instanceof IAdaptable)
					proj = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				if (proj != null && WorkspaceModelManager.isPluginProject(proj))
					projects.add(proj);
			}
			if (projects.size() > 0) {
				BusyIndicator.showWhile(PDEPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
					public void run() {
						Iterator it = projects.iterator();
						while (it.hasNext()) {
							IProject project = (IProject) it.next();
							IFile file = project.getFile(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
							if (file == null || !file.exists())
								file = project.getFile(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
							if (file == null || !file.exists())
								file = project.getFile(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
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
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}
}
