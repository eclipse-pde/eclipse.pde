/*******************************************************************************
 *  Copyright (c) 2006, 2016 IBM Corporation and others.
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
 *     Alex Blewitt (alex_blewitt@yahoo.com) - contributed a patch for:
 *       o Add an 'Open Manifest' to projects to open the manifest editor
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=133692)
 *     Brian de Alwis - open manifest for the currently active editor
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.HashSet;
import java.util.Iterator;
import org.eclipse.core.commands.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Command handler that opens the manifest file for the plug-in in the
 * manifest editor.
 */
public class OpenManifestHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final HashSet<IProject> projects = new HashSet<>();
		if (HandlerUtil.getActivePart(event) instanceof IEditorPart) {
			IEditorInput input = ((IEditorPart) HandlerUtil.getActivePart(event)).getEditorInput();
			IProject proj = getProject(input);
			if (proj != null && WorkspaceModelManager.isPluginProject(proj)) {
				projects.add(proj);
			}
		}
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			Iterator<?> it = ssel.iterator();
			while (it.hasNext()) {
				Object element = it.next();
				IProject proj = null;
				if (element instanceof IFile)
					proj = ((IFile) element).getProject();
				if ((proj == null) && (element instanceof IProject))
					proj = (IProject) element;
				if ((proj == null) && (element instanceof IAdaptable)) {
					IResource resource = ((IAdaptable) element).getAdapter(IResource.class);
					if (resource != null) {
						proj = resource.getProject();
					}
					if (proj == null) {
						IWorkbenchAdapter workbenchAdapter = ((IAdaptable) element).getAdapter(IWorkbenchAdapter.class);
						if (workbenchAdapter != null) {
							Object o = workbenchAdapter.getParent(element);
							if (o instanceof IAdaptable) {
								resource = ((IAdaptable) o).getAdapter(IResource.class);
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
		}
		if (!projects.isEmpty()) {
			BusyIndicator.showWhile(PDEPlugin.getActiveWorkbenchShell().getDisplay(), () -> {
				Iterator<IProject> it = projects.iterator();
				while (it.hasNext()) {
					IProject project = it.next();
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
			});
		} else
			MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.OpenManifestsAction_title, PDEUIMessages.OpenManifestAction_noManifest);
		return null;
	}

	private IProject getProject(IEditorInput input) {
		IAdapterManager adapterManager = Platform.getAdapterManager();
		Object o;
		if ((o = input.getAdapter(IResource.class)) != null || (o = adapterManager.getAdapter(input, IResource.class)) != null) {
			return ((IFile) o).getProject();
		}
		if ((o = input.getAdapter(IProject.class)) != null || (o = adapterManager.getAdapter(input, IProject.class)) != null) {
			return (IProject) o;
		}
		return null;
	}
}
