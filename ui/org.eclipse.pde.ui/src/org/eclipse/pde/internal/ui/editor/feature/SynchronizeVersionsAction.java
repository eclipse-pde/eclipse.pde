/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.PlatformUI;

public class SynchronizeVersionsAction extends Action {
	private FeatureEditor activeEditor;

	public SynchronizeVersionsAction() {
		setText(PDEUIMessages.Actions_synchronizeVersions_label);
	}

	private void ensureContentSaved() {
		if (activeEditor.isDirty()) {
			try {
				IRunnableWithProgress op = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						activeEditor.doSave(monitor);
					}
				};
				PlatformUI.getWorkbench().getProgressService().runInUI(PDEPlugin.getActiveWorkbenchWindow(), op, PDEPlugin.getWorkspace().getRoot());
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} catch (InterruptedException e) {
			}
		}
	}

	public void run() {
		ensureContentSaved();
		SynchronizeVersionsWizard wizard = new SynchronizeVersionsWizard(activeEditor);
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.open();
	}

	public void setActiveEditor(FeatureEditor editor) {
		this.activeEditor = editor;
		IModel model = (IModel) editor.getAggregateModel();
		setEnabled(model.isEditable());
	}
}
