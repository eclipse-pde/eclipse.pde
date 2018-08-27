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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.pde.ui.IExtensionEditorWizard;

public class ExtensionEditorSelectionPage extends WizardListSelectionPage {
	private IProject fProject;
	private IPluginBase fPluginBase;
	private IStructuredSelection fSelection;

	public ExtensionEditorSelectionPage(ElementList wizards) {
		super(wizards, PDEUIMessages.ExtensionEditorSelectionPage_message);
		setTitle(PDEUIMessages.ExtensionEditorSelectionPage_title);
		setDescription(PDEUIMessages.ExtensionEditorSelectionPage_desc);
	}

	public void init(IProject project, IPluginBase pluginBase, IStructuredSelection selection) {
		this.fProject = project;
		this.fPluginBase = pluginBase;
		this.fSelection = selection;
	}

	@Override
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			@Override
			public IBasePluginWizard createWizard() throws CoreException {
				IExtensionEditorWizard wizard = createWizard(wizardElement);
				wizard.init(fProject, fPluginBase.getPluginModel(), fSelection);
				return wizard;
			}

			protected IExtensionEditorWizard createWizard(WizardElement element) throws CoreException {
				return (IExtensionEditorWizard) element.createExecutableExtension();
			}
		};
	}
}
