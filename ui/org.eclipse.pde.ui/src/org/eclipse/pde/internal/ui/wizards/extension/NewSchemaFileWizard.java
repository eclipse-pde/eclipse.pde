package org.eclipse.pde.internal.ui.wizards.extension;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.ui.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jdt.core.IJavaProject;

public class NewSchemaFileWizard extends Wizard implements INewWizard {
	private NewSchemaFileMainPage mainPage;
	private IContainer container;
	public static final String KEY_WTITLE = "NewSchemaFileWizard.wtitle";

	public NewSchemaFileWizard() {
		setDialogSettings(getSettingsSection());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXP_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		setNeedsProgressMonitor(true);
	}
	public void addPages() {
		mainPage = new NewSchemaFileMainPage(container);
		addPage(mainPage);
	}

	private IDialogSettings getSettingsSection() {
		IDialogSettings root = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = root.getSection("newExtensionPointWizard");
		if (section == null)
			section = root.addNewSection("newExtensionPointWizard");
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
