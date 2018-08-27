/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.templates.NewExtensionTemplateWizard;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.pde.ui.IExtensionWizard;
import org.eclipse.pde.ui.templates.ITemplateSection;

/**
 *
 */
public class ExtensionTreeSelectionPage extends WizardTreeSelectionPage {
	private IProject fProject;
	private IPluginBase fPluginBase;

	/**
	 * @param categories
	 * @param baseCategory
	 * @param message
	 */
	public ExtensionTreeSelectionPage(WizardCollectionElement categories, String baseCategory, String message) {
		super(categories, baseCategory, message);
	}

	public void init(IProject project, IPluginBase pluginBase) {
		this.fProject = project;
		this.fPluginBase = pluginBase;
	}

	@Override
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			@Override
			public IBasePluginWizard createWizard() throws CoreException {
				IExtensionWizard wizard = createWizard(wizardElement);
				wizard.init(fProject, fPluginBase.getPluginModel());
				return wizard;
			}

			protected IExtensionWizard createWizard(WizardElement element) throws CoreException {
				if (element.isTemplate()) {
					IConfigurationElement template = element.getTemplateElement();
					if (template == null)
						return null;
					ITemplateSection section = (ITemplateSection) template.createExecutableExtension("class"); //$NON-NLS-1$
					return new NewExtensionTemplateWizard(section);
				}
				return (IExtensionWizard) element.createExecutableExtension();
			}
		};
	}

	public ISelectionProvider getSelectionProvider() {
		return wizardSelectionViewer;
	}
}
