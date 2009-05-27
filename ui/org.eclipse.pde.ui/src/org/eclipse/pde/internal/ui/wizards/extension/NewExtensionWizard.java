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
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.wizards.*;

public class NewExtensionWizard extends NewWizard {
	public static final String PLUGIN_POINT = "newExtension"; //$NON-NLS-1$
	private PointSelectionPage fPointPage;
	private IPluginModelBase fModel;
	private IProject fProject;
	private ManifestEditor fEditor;
	private WizardCollectionElement fWizardCollection;

	public NewExtensionWizard(IProject project, IPluginModelBase model, ManifestEditor editor) {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEX_WIZ);
		fModel = model;
		fProject = project;
		fEditor = editor;
		setForcePreviousAndNextButtons(true);
		setWindowTitle(PDEUIMessages.NewExtensionWizard_wtitle);
		loadWizardCollection();
	}

	public void addPages() {
		fPointPage = new PointSelectionPage(fProject, fModel, fWizardCollection, getTemplates(), this);
		addPage(fPointPage);
	}

	private void loadWizardCollection() {
		NewExtensionRegistryReader reader = new NewExtensionRegistryReader();
		fWizardCollection = (WizardCollectionElement) reader.readRegistry(PDEPlugin.getPluginId(), PLUGIN_POINT, false);
	}

	public WizardCollectionElement getTemplates() {
		WizardCollectionElement templateCollection = new WizardCollectionElement("", "", null); //$NON-NLS-1$ //$NON-NLS-2$
		collectTemplates(fWizardCollection.getChildren(), templateCollection);
		return templateCollection;
	}

	private void collectTemplates(Object[] children, WizardCollectionElement list) {
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof WizardCollectionElement) {
				WizardCollectionElement element = (WizardCollectionElement) children[i];
				collectTemplates(element.getChildren(), list);
				collectTemplates(element.getWizards().getChildren(), list);
			} else if (children[i] instanceof WizardElement) {
				WizardElement wizard = (WizardElement) children[i];
				if (wizard.isTemplate())
					list.getWizards().add(wizard);
			}
		}
	}

	public boolean performFinish() {
		fPointPage.checkModel();
		if (fPointPage.canFinish())
			return fPointPage.finish();
		return true;
	}

	public ManifestEditor getEditor() {
		return fEditor;
	}

}
