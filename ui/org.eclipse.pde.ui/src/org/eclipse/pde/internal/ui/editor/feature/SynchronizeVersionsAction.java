package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;

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
		ProgressMonitorDialog monitor =
			new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
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
	boolean result =
		PDECore.getDefault().getWorkspaceModelManager().getAllEditableModelsUnused(
			modelClass);
	if (!result) {
		MessageDialog.openError(
			PDEPlugin.getActiveWorkbenchShell(),
			PDEPlugin.getResourceString(DIALOG_TITLE),
			PDEPlugin.getResourceString(DIALOG_MESSAGE));
	}
	return result;
}
public void run() {
	ensureContentSaved();
	if (!ensureEditorsClosed()) return;
	SynchronizeVersionsWizard wizard = new SynchronizeVersionsWizard(activeEditor);
	WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
	dialog.open();
}
public void setActiveEditor(FeatureEditor editor) {
	this.activeEditor = editor;
}
}
