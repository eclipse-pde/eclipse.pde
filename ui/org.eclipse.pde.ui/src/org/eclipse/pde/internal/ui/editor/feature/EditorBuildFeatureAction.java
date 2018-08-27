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
package org.eclipse.pde.internal.ui.editor.feature;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.ResizableWizardDialog;
import org.eclipse.pde.internal.ui.wizards.exports.FeatureExportWizard;
import org.eclipse.ui.PlatformUI;

public class EditorBuildFeatureAction extends Action {
	private FeatureEditor activeEditor;
	private IFile featureFile;

	public EditorBuildFeatureAction() {
		setText(PDEUIMessages.FeatureEditor_BuildAction_label);
	}

	private void ensureContentSaved() {
		if (activeEditor.isDirty()) {
			try {
				IRunnableWithProgress op = monitor -> activeEditor.doSave(monitor);
				PlatformUI.getWorkbench().getProgressService().runInUI(PDEPlugin.getActiveWorkbenchWindow(), op, PDEPlugin.getWorkspace().getRoot());

			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public void run() {
		ensureContentSaved();
		FeatureExportWizard wizard = new FeatureExportWizard();
		IStructuredSelection selection;
		if (featureFile != null)
			selection = new StructuredSelection(featureFile);
		else
			selection = new StructuredSelection();
		wizard.init(PlatformUI.getWorkbench(), selection);
		WizardDialog wd = new ResizableWizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		wd.create();
		wd.open();
	}

	public void setActiveEditor(FeatureEditor editor) {
		this.activeEditor = editor;
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
		if (model != null) {
			featureFile = (IFile) model.getUnderlyingResource();
			setEnabled(model.isEditable());
		}
	}
}
