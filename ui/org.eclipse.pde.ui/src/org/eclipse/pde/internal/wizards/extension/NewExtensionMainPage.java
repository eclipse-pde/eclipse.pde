package org.eclipse.pde.internal.wizards.extension;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.wizards.templates.ITemplateSection;

public class NewExtensionMainPage extends WizardTreeSelectionPage {
	public static final String KEY_TITLE = "NewExtensionWizard.title";
	public static final String KEY_DESC = "NewExtensionWizard.desc";
	private IPluginModelBase model;
	private IProject project;

	public NewExtensionMainPage(
		IProject project,
		IPluginModelBase model,
		WizardCollectionElement wizardElements,
		String message) {
		super(wizardElements, "Base", message);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
		this.project = project;
		this.model = model;
	}
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			public IBasePluginWizard createWizard() throws CoreException {
				IExtensionWizard wizard = createWizard(wizardElement);
				wizard.init(project, model);
				return wizard;
			}
			protected IExtensionWizard createWizard(WizardElement element)
				throws CoreException {
				if (element.isTemplate()) {
					ITemplateSection section =
						(ITemplateSection) element.createExecutableExtension();
					return new NewExtensionTemplateWizard(section);
				} else {
					return (IExtensionWizard) element.createExecutableExtension();
				}
			}
		};
	}

}