package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.base.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jdt.core.*;

public class PluginCodeGeneratorWizard extends Wizard implements IPluginContentWizard {
	private DefaultCodeGenerationPage defaultPage;
	private IProjectProvider projectProvider;
	private boolean fragment;
	private IPluginStructureData structureData;

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
}
public boolean performFinish() {
	return defaultPage.finish();
}
}
