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
package org.eclipse.pde.internal.ui.editor.plugin;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.wizards.ResizableWizardDialog;
import org.eclipse.pde.internal.ui.wizards.exports.PluginExportWizard;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 *
 */
public class PluginExportAction extends Action {
	private PDEFormEditor fEditor;

	public PluginExportAction(PDEFormEditor editor) {
		fEditor = editor;
	}

	public PluginExportAction() {
	}

	private void ensureContentSaved() {
		if (fEditor.isDirty()) {
			try {
				IRunnableWithProgress op = monitor -> fEditor.doSave(monitor);
				PlatformUI.getWorkbench().getProgressService().runInUI(PDEPlugin.getActiveWorkbenchWindow(), op, PDEPlugin.getWorkspace().getRoot());
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public void run() {
		if (fEditor != null)
			ensureContentSaved();
		IStructuredSelection selection;
		IResource resource = null;
		if (fEditor != null) {
			IModel model = (IModel) fEditor.getAggregateModel();
			resource = model == null ? null : model.getUnderlyingResource();
		}
		String customWizard = null;
		if (resource != null) {
			selection = new StructuredSelection(resource);
			IProject project = resource.getProject();
			if (project != null) {
				// a project can override the default export wizard
				customWizard = PDEProject.getExportWizard(project);
			}
		} else {
			selection = new StructuredSelection();
		}
		IWorkbenchWizard wizard = null;
		if (customWizard != null) {
			IWizardDescriptor descriptor = PlatformUI.getWorkbench().getExportWizardRegistry().findWizard(customWizard);
			if (descriptor != null) {
				try {
					wizard = descriptor.createWizard();
				} catch (CoreException e) {
					PDEPlugin.log(e);
					notifyResult(false);
					return;
				}
			}
		}
		if (wizard == null) {
			wizard = new PluginExportWizard();
		}
		wizard.init(PlatformUI.getWorkbench(), selection);
		WizardDialog wd = new ResizableWizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		wd.create();
		//wd.getShell().setSize(450, 600);
		int result = wd.open();
		notifyResult(result == Window.OK);
	}
}
