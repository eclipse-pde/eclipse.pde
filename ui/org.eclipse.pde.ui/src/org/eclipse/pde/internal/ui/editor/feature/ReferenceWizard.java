package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;

public class ReferenceWizard extends Wizard {
	private IFeatureModel model;
	private ReferenceWizardPage page;

	public ReferenceWizard(
		IFeatureModel model,
		ReferenceWizardPage page) {
		this.model = model;
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		this.page = page;
	}

	public void addPages() {
		addPage(page);
	}

	public boolean performFinish() {
		return page.finish();
	}

}