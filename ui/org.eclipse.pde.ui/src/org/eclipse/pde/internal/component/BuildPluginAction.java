package org.eclipse.pde.internal.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.*;

public class BuildPluginAction implements IObjectActionDelegate {
	private IWorkbenchPart targetPart;
	private IFile pluginBaseFile;
	private boolean fragment;

public IFile getPluginBaseFile() {
	return pluginBaseFile;
}
public void run(IAction action) {
	if (pluginBaseFile==null) return;
	if (pluginBaseFile.exists()==false) return;
	BuildPluginWizard wizard = new BuildPluginWizard(pluginBaseFile, fragment);
	WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
	dialog.setMinimumPageSize(400, 300);
	dialog.create();
	dialog.open();
}

public void selectionChanged(IAction action, ISelection selection) {
	IFile file = null;

	if (selection instanceof IStructuredSelection) {
		Object obj = ((IStructuredSelection) selection).getFirstElement();
		if (obj != null && obj instanceof IFile) {
			file = (IFile) obj;
			String name = file.getName().toLowerCase();
			if (name.equals("plugin.xml")) {
				fragment = false;
			}
			else if (name.equals("fragment.xml")) {
				fragment = true;
			}
			else file = null;
		}
	}
	this.pluginBaseFile = file;
}

public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	this.targetPart = targetPart;
}
public void setPluginBaseFile(IFile pluginBaseFile) {
	this.pluginBaseFile = pluginBaseFile;
}
}
