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
import org.eclipse.pde.internal.ui.*;

public class NewExtensionWizard extends NewWizard {
	public static final String PLUGIN_POINT = "newExtension";
	public static final String STATUS_MESSAGE = "NewExtensionWizard.statusMessage";
	private static final String KEY_WTITLE = "NewExtensionWizard.wtitle";

	private PointSelectionPage pointPage;
	private IPluginModelBase model;
	private IProject project;
	public NewExtensionWizard(IProject project, IPluginModelBase model) {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEX_WIZ);
		this.model = model;
		this.project = project;
		setForcePreviousAndNextButtons(true);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	public void addPages() {
		pointPage =
			new PointSelectionPage(project, model.getPluginBase(), getAvailableExtensionWizards(), this);
		addPage(pointPage);
	}
	public WizardCollectionElement getAvailableExtensionWizards() {
		NewExtensionRegistryReader reader = new NewExtensionRegistryReader();
		WizardCollectionElement element = (WizardCollectionElement) reader.readRegistry(
			PDEPlugin.getPluginId(),
			PLUGIN_POINT,
			false);
		Object[] children = element.getChildren();  
		for  (int i = 0; i<children.length; i++){
			if (children[i] instanceof WizardCollectionElement)
				if (((WizardCollectionElement)children[i]).getId().equals("templates")) 
					return (WizardCollectionElement)children[i]; 
			
		}
		
		return element;
		
	}
	public boolean performFinish() {
		if (pointPage.canFinish())
			return pointPage.finish();
		return true;
	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
}
