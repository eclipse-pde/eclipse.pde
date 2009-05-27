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
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.wizards.NewWizard;

public class NewExtensionPointWizard extends NewWizard {
	private NewExtensionPointMainPage mainPage;
	private IPluginModelBase model;
	private IProject project;
	private IPluginExtensionPoint point;
	private ManifestEditor editor;

	public NewExtensionPointWizard(IProject project, IPluginModelBase model, ManifestEditor editor) {
		this(project, model, (IPluginExtensionPoint) null);
		this.editor = editor;
	}

	public NewExtensionPointWizard(IProject project, IPluginModelBase model, IPluginExtensionPoint point) {
		initialize();
		this.project = project;
		this.model = model;
		this.point = point;
	}

	public void initialize() {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXP_WIZ);
		setWindowTitle(PDEUIMessages.NewExtensionPointWizard_wtitle);
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		mainPage = new NewExtensionPointMainPage(project, model, point);
		addPage(mainPage);
	}

	public boolean performFinish() {
		if (editor != null)
			editor.ensurePluginContextPresence();
		return mainPage.finish();
	}
}
