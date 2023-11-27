/*******************************************************************************
 *  Copyright (c) 2023, 2024 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.List;
import java.util.Objects;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.pde.internal.core.natures.BndProject;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command handler to run the convert to automatic manifests operation.
 *
 */
public class ConvertAutomaticManifestAction extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (PlatformUI.getWorkbench().saveAllEditors(true)) {
			// only do our work when all work is committed to files...
			if (HandlerUtil.getCurrentSelection(event) instanceof IStructuredSelection selection) {
				List<IProject> projects = selection.stream().map(ConvertAutomaticManifestAction::toProject)
						.filter(Objects::nonNull)
						.filter(PluginProject::isJavaProject)
						.filter(proj -> !BndProject.isBndProject(proj)).toList();
				if (projects.isEmpty()) {
					MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(),
							PDEUIMessages.ConvertAutomaticManifestWizardPage_title,
							PDEUIMessages.OrganizeManifestsWizardPage_errorMsg);
					return null;
				}
				RefactoringWizardOpenOperation refactoringOperation = new RefactoringWizardOpenOperation(new ConvertAutomaticManifestsWizard(projects));
				try {
					refactoringOperation.run(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.ConvertAutomaticManifestWizardPage_title);
				} catch (final InterruptedException e) {
					// ignore...
				}
			}
		}
		return null;
	}

	private static IProject toProject(Object object) {
		if (object instanceof IJavaProject java) {
			return java.getProject();
		}
		if (object instanceof IResource resource) {
			return resource.getProject();
		}
		return null;
	}

}
