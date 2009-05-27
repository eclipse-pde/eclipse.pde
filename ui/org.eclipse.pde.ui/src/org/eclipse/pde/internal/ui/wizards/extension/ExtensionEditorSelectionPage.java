/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

/**
 *
 */
public class ExtensionEditorSelectionPage extends WizardListSelectionPage {
	private IProject fProject;
	private IPluginBase fPluginBase;
	private IStructuredSelection fSelection;

	/**
	 * @param categories
	 * @param baseCategory
	 * @param message
	 */
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

	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
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
