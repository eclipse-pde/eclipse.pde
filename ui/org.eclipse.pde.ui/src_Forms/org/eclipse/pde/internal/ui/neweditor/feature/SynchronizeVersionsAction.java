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
package org.eclipse.pde.internal.ui.neweditor.feature;
import java.lang.reflect.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
public class SynchronizeVersionsAction extends Action {
	public static final String LABEL = "Actions.synchronizeVersions.label";
	public static final String DIALOG_TITLE = "FeatureEditor.modelsInUse.title";
	public static final String DIALOG_MESSAGE = "FeatureEditor.modelsInUse.message";
	private FeatureEditor activeEditor;
	public SynchronizeVersionsAction() {
		setText(PDEPlugin.getResourceString(LABEL));
	}
	private void ensureContentSaved() {
		if (activeEditor.isDirty()) {
			ProgressMonitorDialog monitor = new ProgressMonitorDialog(PDEPlugin
					.getActiveWorkbenchShell());
			try {
				monitor.run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						activeEditor.doSave(monitor);
					}
				});
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