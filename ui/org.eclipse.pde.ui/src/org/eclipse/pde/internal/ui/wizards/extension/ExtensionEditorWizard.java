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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.ElementList;

public class ExtensionEditorWizard extends Wizard {
	public static final String PLUGIN_POINT = "newExtension"; //$NON-NLS-1$
	public static final String STATUS_MESSAGE = "ExtensionEditorWizard.statusMessage"; //$NON-NLS-1$
	private static final String KEY_WTITLE = "ExtensionEditorWizard.wtitle"; //$NON-NLS-1$

	private ExtensionEditorSelectionPage pointPage;
	private IPluginModelBase model;
	private IProject project;
	private IStructuredSelection selection;
	private ElementList wizards;
	
	public ExtensionEditorWizard(IProject project, IPluginModelBase model, IStructuredSelection selection) {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEX_WIZ);
		this.model = model;
		this.project = project;
		this.selection = selection;
		setForcePreviousAndNextButtons(true);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		loadWizardCollection();
	}
	public void addPages() {
		pointPage =
			new ExtensionEditorSelectionPage(wizards);
		pointPage.init(project, model.getPluginBase(), selection);
		addPage(pointPage);
	}
	private void loadWizardCollection() {
		NewExtensionRegistryReader reader = new NewExtensionRegistryReader(true);
		wizards = reader.readRegistry(
				PDEPlugin.getPluginId(),
				PLUGIN_POINT,
				true);
	}

	public boolean performFinish() {
		return true;
	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
}