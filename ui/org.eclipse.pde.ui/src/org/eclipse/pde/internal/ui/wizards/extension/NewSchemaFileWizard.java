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
package org.eclipse.pde.internal.ui.wizards.extension;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;

public class NewSchemaFileWizard extends Wizard implements INewWizard {
	private NewSchemaFileMainPage mainPage;
	private IContainer container;
	private IPluginExtensionPoint point;
	private boolean isPluginIdFinal;
	public static final String KEY_WTITLE = "NewSchemaFileWizard.wtitle"; //$NON-NLS-1$

	public NewSchemaFileWizard() {
		this(null, null, false);
	}
	public NewSchemaFileWizard(IProject project, IPluginExtensionPoint point, boolean isFinalPluginId){
		initialize();
		this.container = project;
		this.point = point;
		this.isPluginIdFinal = isFinalPluginId;
	}
	public void initialize(){
		setDialogSettings(getSettingsSection());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_EXT_POINT_SCHEMA_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		setNeedsProgressMonitor(true);
	}
	public void addPages() {
		mainPage = new NewSchemaFileMainPage(container, point, isPluginIdFinal);
		addPage(mainPage);
	}

	private IDialogSettings getSettingsSection() {
		IDialogSettings root = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = root.getSection("newExtensionPointWizard"); //$NON-NLS-1$
		if (section == null)
			section = root.addNewSection("newExtensionPointWizard"); //$NON-NLS-1$
		return section;
	}
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		Object sel = selection.getFirstElement();
		if (sel instanceof IJavaProject) {
			container = ((IJavaProject) sel).getProject();
		} else if (sel instanceof IContainer)
			container = (IContainer) sel;
	}

	public boolean performFinish() {
		return mainPage.finish();
	}
}
