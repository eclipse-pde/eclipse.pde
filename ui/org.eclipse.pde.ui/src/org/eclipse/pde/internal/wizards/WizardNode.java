package org.eclipse.pde.internal.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.base.*;
import org.eclipse.jface.dialogs.*;

public abstract class WizardNode implements IWizardNode {
	private IWizard wizard;
	private BaseWizardSelectionPage parentWizardPage;
	public static final String KEY_CREATION_ERROR_TEXT="Errors.CreationError.NoWizard";
	public static final String KEY_CREATION_ERROR="Errors.CreationError";
	protected WizardElement wizardElement;

public WizardNode(BaseWizardSelectionPage parentPage, WizardElement element) {
	parentWizardPage = parentPage;
	wizardElement = element;
}
protected abstract IBasePluginWizard createWizard() throws CoreException;
public void dispose() {
	if (wizard != null) {
		wizard.dispose();
		wizard = null;
	}
}
public WizardElement getElement() {
	return wizardElement;
}
public Point getExtent() {
	return new Point(-1, -1);
}
public IWizard getWizard() {
	if (wizard != null)
		return wizard; // we've already created it

	IBasePluginWizard pluginWizard;
	try {
		pluginWizard = createWizard(); // create instance of target wizard
	} catch (CoreException e) {
		parentWizardPage.setDescriptionText("");
		parentWizardPage.setErrorMessage(PDEPlugin.getResourceString(KEY_CREATION_ERROR_TEXT));
		MessageDialog.openError(
			parentWizardPage.getWizard().getContainer().getShell(), 
			PDEPlugin.getResourceString(KEY_CREATION_ERROR), 
			PDEPlugin.getResourceString(KEY_CREATION_ERROR_TEXT)); 
		return null;
	}
	wizard = (IWizard)pluginWizard;
	//wizard.setUseContainerState(false);
	return wizard;
}
public boolean isContentCreated() {
	return wizard != null;
}
}
