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
package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;


public class NewWizard extends Wizard implements INewWizard {
	private org.eclipse.ui.IWorkbench workbench;
	private org.eclipse.jface.viewers.IStructuredSelection selection;
	private static final String KEY_WTITLE = "NewWizard.wtitle";

public NewWizard() {
	super();
	setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
}
public org.eclipse.jface.viewers.IStructuredSelection getSelection() {
	return selection;
}
public IWorkbench getWorkbench() {
	return workbench;
}
public void init(IWorkbench workbench, IStructuredSelection selection) {
	this.workbench = workbench;
	this.selection = selection;
}
public boolean performFinish() {
	return true;
}
}
