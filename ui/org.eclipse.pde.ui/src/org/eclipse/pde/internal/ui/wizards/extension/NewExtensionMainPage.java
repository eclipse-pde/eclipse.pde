/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;

import org.eclipse.core.resources.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.*;

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
	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(parent);
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.ADD_EXTENSIONS_MAIN);
		
	}

}
