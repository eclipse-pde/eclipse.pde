package org.eclipse.pde.internal.wizards.extension;

import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.base.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.*;


public class NewExtensionMainPage extends WizardTreeSelectionPage {
	public static final String KEY_TITLE = "NewExtensionWizard.title";
	public static final String KEY_DESC = "NewExtensionWizard.desc";
	private IPluginModelBase model;
	private IProject project;

public NewExtensionMainPage(IProject project, IPluginModelBase model, WizardCollectionElement wizardElements, String message) {
	super(wizardElements, "Base", message);
	setTitle(PDEPlugin.getResourceString(KEY_TITLE));
	setDescription(PDEPlugin.getResourceString(KEY_DESC));
	this.project = project;
	this.model = model;
}
protected IWizardNode createWizardNode(WizardElement element) {
	return new WizardNode(this, element) {
		public IBasePluginWizard createWizard() throws CoreException {
			IExtensionWizard wizard=(IExtensionWizard)wizardElement.createExecutableExtension();
			wizard.init(project, model);
			return wizard;
		}
	};
}
}
