package org.eclipse.pde.internal.editor.manifest;
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
import org.eclipse.pde.internal.*;
import org.eclipse.pde.model.plugin.*;

public class NewDependencyWizard extends Wizard {
	private IPluginModelBase modelBase;
	private NewDependencyWizardPage mainPage;

public NewDependencyWizard(IPluginModelBase modelBase) {
	this.modelBase = modelBase;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
}

public void addPages() {
	mainPage = new NewDependencyWizardPage(modelBase);
	addPage(mainPage);
}

public boolean performFinish() {
	return mainPage.finish();
}

}
