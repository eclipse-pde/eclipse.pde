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
package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.lang.reflect.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.exports.*;
import org.eclipse.ui.*;

public class PluginExportAction extends Action {
	private PDEFormEditor fEditor;
	
	public PluginExportAction(PDEFormEditor editor) {
		fEditor = editor;
	}
	
	private void ensureContentSaved() {
		if (fEditor.isDirty()) {
			ProgressMonitorDialog monitor = new ProgressMonitorDialog(PDEPlugin
					.getActiveWorkbenchShell());
			try {
				monitor.run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						fEditor.doSave(monitor);
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
		PluginExportWizard wizard = new PluginExportWizard();
		IStructuredSelection selection;
		IResource resource = fEditor.getAggregateModel().getUnderlyingResource();
		if (resource != null)
			selection = new StructuredSelection(resource);
		else
			selection = new StructuredSelection();
		wizard.init(PlatformUI.getWorkbench(), selection);
		WizardDialog wd = new ResizableWizardDialog(PDEPlugin
				.getActiveWorkbenchShell(), wizard);
		wd.create();
		//wd.getShell().setSize(450, 600);
		wd.open();
	}
	
}