package org.eclipse.pde.internal.ui.wizards.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.internal.ui.*;

public class PluginCodeGeneratorWizard extends Wizard implements IPluginContentWizard {
	private DefaultCodeGenerationPage defaultPage;
	private IProjectProvider projectProvider;
	private boolean fragment;
	private IPluginStructureData structureData;
	private static final String KEY_WTITLE = "PluginCodeGeneratorWizard.title";
	private static final String KEY_WFTITLE = "PluginCodeGeneratorWizard.ftitle";

public PluginCodeGeneratorWizard() {
	super();
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_DEFCON_WIZ);
	setNeedsProgressMonitor(true);
}
public void addPages() {
	defaultPage =
		new DefaultCodeGenerationPage(projectProvider, structureData, fragment);
	addPage(defaultPage);
}
public void init(IProjectProvider  projectProvider, IPluginStructureData structureData, boolean fragment) {
	this.projectProvider = projectProvider;
	this.structureData = structureData;
	this.fragment = fragment;
	setWindowTitle(PDEPlugin.getResourceString(fragment?KEY_WFTITLE:KEY_WTITLE));
}
public boolean performFinish() {
	return defaultPage.finish();
}
}
