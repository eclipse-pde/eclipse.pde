/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;

import org.eclipse.core.resources.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.*;

public class NewExtensionWizard extends NewWizard {
	public static final String PLUGIN_POINT = "newExtension"; //$NON-NLS-1$
	public static final String STATUS_MESSAGE = "NewExtensionWizard.statusMessage"; //$NON-NLS-1$
	private static final String KEY_WTITLE = "NewExtensionWizard.wtitle"; //$NON-NLS-1$

	private PointSelectionPage pointPage;
	private IPluginModelBase model;
	private IProject project;
	private ManifestEditor editor;
	private WizardCollectionElement wizardCollection;
	
	public NewExtensionWizard(IProject project, IPluginModelBase model, ManifestEditor editor) {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEX_WIZ);
		this.model = model;
		this.project = project;
		this.editor = editor;
		setForcePreviousAndNextButtons(true);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		loadWizardCollection();
	}
	public void addPages() {
		pointPage =
			new PointSelectionPage(project, model.getPluginBase(), wizardCollection, getTemplates(), this);
		addPage(pointPage);
	}
	private void loadWizardCollection() {
		NewExtensionRegistryReader reader = new NewExtensionRegistryReader();
		wizardCollection = (WizardCollectionElement) reader.readRegistry(
				PDEPlugin.getPluginId(),
				PLUGIN_POINT,
				false);
	}
	
	public WizardCollectionElement getTemplates() {
		WizardCollectionElement templateCollection = new WizardCollectionElement("", "", null); //$NON-NLS-1$ //$NON-NLS-2$
		collectTemplates(wizardCollection.getChildren(), templateCollection);
		return templateCollection;
	}
	
	private void collectTemplates(Object [] children, WizardCollectionElement list) {
		for  (int i = 0; i<children.length; i++){
			if (children[i] instanceof WizardCollectionElement) {
				WizardCollectionElement element = (WizardCollectionElement)children[i];
				collectTemplates(element.getChildren(), list);
				collectTemplates(element.getWizards().getChildren(), list);
			}
			else if (children[i] instanceof WizardElement) {
				WizardElement wizard = (WizardElement)children[i];
				if (wizard.isTemplate())
					list.getWizards().add(wizard);
			}
		}
	}
	public boolean performFinish() {
		if (pointPage.canFinish())
			return pointPage.finish();
		return true;
	}
	
	public ManifestEditor getEditor() {
		return editor;
	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
}
