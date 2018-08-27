/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class NewSchemaFileWizard extends Wizard implements INewWizard {
	private NewSchemaFileMainPage mainPage;
	private IContainer container;
	private IPluginExtensionPoint point;
	private boolean isPluginIdFinal;

	public NewSchemaFileWizard() {
		this(null, null, false);
	}

	public NewSchemaFileWizard(IProject project, IPluginExtensionPoint point, boolean isFinalPluginId) {
		initialize();
		this.container = project;
		this.point = point;
		this.isPluginIdFinal = isFinalPluginId;
	}

	public void initialize() {
		setDialogSettings(getSettingsSection());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_EXT_POINT_SCHEMA_WIZ);
		setWindowTitle(PDEUIMessages.NewSchemaFileWizard_wtitle);
		setNeedsProgressMonitor(true);
	}

	@Override
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

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		Object sel = selection.getFirstElement();
		if (sel instanceof IJavaProject) {
			container = ((IJavaProject) sel).getProject();
		} else if (sel instanceof IContainer)
			container = (IContainer) sel;
	}

	@Override
	public boolean performFinish() {
		return mainPage.finish();
	}
}
