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
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.wizards.NewWizard;

public class NewExtensionPointWizard extends NewWizard {
	private NewExtensionPointMainPage mainPage;
	private final IPluginModelBase model;
	private final IProject project;
	private final IPluginExtensionPoint point;
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

	@Override
	public void addPages() {
		mainPage = new NewExtensionPointMainPage(project, model, point);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		if (editor != null)
			editor.ensurePluginContextPresence();
		return mainPage.finish();
	}
}
