package org.eclipse.pde.internal.feature;
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

public class BuildFeatureJarAction implements IObjectActionDelegate {
	private IWorkbenchPart targetPart;
	private IFile featureFile;

public IFile getFeatureFile() {
	return featureFile;
}
public void run(IAction action) {
	if (featureFile==null) return;
	if (featureFile.exists()==false) return;
	FeatureJarWizard wizard = new FeatureJarWizard(featureFile);
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
			if (file.getName().toLowerCase().equals("feature.xml") == false)
				file = null;
		}
	}
	this.featureFile = file;
}
public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	this.targetPart = targetPart;
}
public void setComponentFile(IFile featureFile) {
	this.featureFile = featureFile;
}
}
