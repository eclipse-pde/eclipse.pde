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
package org.eclipse.pde.internal.ui.editor.feature;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.PlatformUI;
public class SynchronizeVersionsAction extends Action {
	public static final String LABEL = "Actions.synchronizeVersions.label"; //$NON-NLS-1$
	public static final String DIALOG_TITLE = "FeatureEditor.modelsInUse.title"; //$NON-NLS-1$
	public static final String DIALOG_MESSAGE = "FeatureEditor.modelsInUse.message"; //$NON-NLS-1$
	private FeatureEditor activeEditor;
	public SynchronizeVersionsAction() {
		setText(PDEPlugin.getResourceString(LABEL));
	}
	private void ensureContentSaved() {
		if (activeEditor.isDirty()) {
			try {
				IRunnableWithProgress op = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						activeEditor.doSave(monitor);
					}
				};
				PlatformUI.getWorkbench().getProgressService().runInUI(
						PDEPlugin.getActiveWorkbenchWindow(), op,
						PDEPlugin.getWorkspace().getRoot());
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} catch (InterruptedException e) {
			}
		}
	}
	private boolean ensureEditorsClosed() {
		Class modelClass = IPluginModelBase.class;
		boolean result = PDECore.getDefault().getWorkspaceModelManager()
				.getAllEditableModelsUnused(modelClass);
		if (!result) {
			MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString(DIALOG_TITLE), PDEPlugin
							.getResourceString(DIALOG_MESSAGE));
		}
		return result;
	}
	public void run() {
		ensureContentSaved();
		if (!ensureEditorsClosed())
			return;
		SynchronizeVersionsWizard wizard = new SynchronizeVersionsWizard(
				activeEditor);
		WizardDialog dialog = new WizardDialog(PDEPlugin
				.getActiveWorkbenchShell(), wizard);
		dialog.open();
	}
	public void setActiveEditor(FeatureEditor editor) {
		this.activeEditor = editor;
		IModel model = (IModel) editor.getAggregateModel();
		setEnabled(model.isEditable());
	}
}