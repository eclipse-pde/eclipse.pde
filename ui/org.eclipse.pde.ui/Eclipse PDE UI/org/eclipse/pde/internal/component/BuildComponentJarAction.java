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

public class BuildComponentJarAction implements IObjectActionDelegate {
	private IWorkbenchPart targetPart;
	private IFile componentFile;

public org.eclipse.core.resources.IFile getComponentFile() {
	return componentFile;
}
public void run(IAction action) {
	if (componentFile==null) return;
	if (componentFile.exists()==false) return;
	ComponentJarWizard wizard = new ComponentJarWizard(componentFile);
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
			if (file.getName().toLowerCase().equals("install.xml") == false)
				file = null;
		}
	}
	this.componentFile = file;
}
public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	this.targetPart = targetPart;
}
public void setComponentFile(org.eclipse.core.resources.IFile newComponentFile) {
	componentFile = newComponentFile;
}
}
