/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;

public class ConvertAutomaticManifestsWizard extends RefactoringWizard {

	public ConvertAutomaticManifestsWizard(List<IProject> projects) {
		super(new PDERefactor(new ConvertAutomaticManifestProcessor(projects)), WIZARD_BASED_USER_INTERFACE);
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.ConvertAutomaticManifestWizardPage_title);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_ORGANIZE_MANIFESTS);
	}

	@Override
	protected void addUserInputPages() {
		addPage(new ConvertAutomaticManifestsWizardSettingsPage(
				(ConvertAutomaticManifestProcessor) ((PDERefactor) getRefactoring()).getProcessor()));
	}


}
