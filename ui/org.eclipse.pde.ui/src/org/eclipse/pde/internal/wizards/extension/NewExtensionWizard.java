package org.eclipse.pde.internal.wizards.extension;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.*;

public class NewExtensionWizard extends NewWizard {
	public static final String PLUGIN_POINT = "newExtension";
	public static final String STATUS_MESSAGE = "NewExtensionWizard.statusMessage";

	private NewExtensionMainPage mainPage;
	private IPluginModelBase model;
	private IProject project;
	public NewExtensionWizard(IProject project, IPluginModelBase model) {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEX_WIZ);
		this.model = model;
		this.project = project;
		setForcePreviousAndNextButtons(true);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	public void addPages() {
		mainPage =
			new NewExtensionMainPage(
				project,
				model,
				getAvailableExtensionCategories(),
				PDEPlugin.getResourceString(STATUS_MESSAGE));
		addPage(mainPage);
	}
	public WizardCollectionElement getAvailableExtensionCategories() {
		NewExtensionRegistryReader reader = new NewExtensionRegistryReader();
		return (WizardCollectionElement) reader.readRegistry(
			PDEPlugin.getPluginId(),
			PLUGIN_POINT,
			false);
	}
	public boolean performFinish() {
		return true;
	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
}