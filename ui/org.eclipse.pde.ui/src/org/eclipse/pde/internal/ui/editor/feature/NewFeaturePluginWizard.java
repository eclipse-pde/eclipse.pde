package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.swt.events.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.model.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;

public class NewFeaturePluginWizard extends Wizard {
	private IFeatureModel model;
	private NewFeaturePluginWizardPage mainPage;

public NewFeaturePluginWizard(IFeatureModel model) {
	this.model = model;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
}

public void addPages() {
	mainPage = new NewFeaturePluginWizardPage(model);
	addPage(mainPage);
}

public boolean performFinish() {
	return mainPage.finish();
}

}
