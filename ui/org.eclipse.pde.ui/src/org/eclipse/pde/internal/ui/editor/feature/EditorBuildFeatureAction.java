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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.ResizableWizardDialog;
import org.eclipse.pde.internal.ui.wizards.exports.FeatureExportWizard;
import org.eclipse.ui.PlatformUI;
public class EditorBuildFeatureAction extends Action {
	public static final String LABEL = "FeatureEditor.BuildAction.label"; //$NON-NLS-1$
	private FeatureEditor activeEditor;
	private IFile featureFile;
	public EditorBuildFeatureAction() {
		setText(PDEPlugin.getResourceString(LABEL));
	}
	private void ensureContentSaved() {
		if (activeEditor.isDirty()) {
			try {
				PlatformUI.getWorkbench().getProgressService().run(false, false, new IRunnableWithProgress() {
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
	public void run() {
		ensureContentSaved();
		FeatureExportWizard wizard = new FeatureExportWizard();
		IStructuredSelection selection;
		if (featureFile != null)
			selection = new StructuredSelection(featureFile);
		else
			selection = new StructuredSelection();
		wizard.init(PlatformUI.getWorkbench(), selection);
		WizardDialog wd = new ResizableWizardDialog(PDEPlugin
				.getActiveWorkbenchShell(), wizard);
		wd.create();
		wd.open();
	}
	public void setActiveEditor(FeatureEditor editor) {
		this.activeEditor = editor;
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
		featureFile = (IFile) model.getUnderlyingResource();
		setEnabled(model.isEditable());
	}
}