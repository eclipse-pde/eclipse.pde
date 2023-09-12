/*******************************************************************************
.
. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.category;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ISetSelectionTarget;

public class NewCategoryCreationOperation extends WorkspaceModifyOperation {
	private final Display fDisplay;
	private final IPath fPath;
	private final String fFileName;

	public NewCategoryCreationOperation(Display display, IPath path, String fileName) {
		fDisplay = display;
		fPath = path;
		fFileName = fileName;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {

		monitor.beginTask(PDEUIMessages.NewCategoryDefinitionWizard_creatingManifest, 2);

		IFile file = createSiteManifest();
		monitor.worked(1);

		openFile(file);
		monitor.worked(1);

	}

	private IFile createSiteManifest() {
		IPath fFilePath = fPath.append(fFileName);
		IFile categoryFile = PDECore.getWorkspace().getRoot().getFile(fFilePath);

		if (categoryFile.exists())
			return categoryFile;

		WorkspaceSiteModel model = new WorkspaceSiteModel(categoryFile);
		model.getSite();
		// Save the model
		model.save();
		model.dispose();
		// Set the default editor
		IDE.setDefaultEditor(categoryFile, IPDEUIConstants.CATEGORY_EDITOR_ID);
		return categoryFile;
	}

	private void openFile(final IFile file) {
		fDisplay.asyncExec(() -> {
			IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
			if (ww == null) {
				return;
			}
			IWorkbenchPage page = ww.getActivePage();
			if (page == null || !file.exists())
				return;
			IWorkbenchPart focusPart = page.getActivePart();
			if (focusPart instanceof ISetSelectionTarget) {
				ISelection selection = new StructuredSelection(file);
				((ISetSelectionTarget) focusPart).selectReveal(selection);
			}
			try {
				page.openEditor(new FileEditorInput(file), IPDEUIConstants.CATEGORY_EDITOR_ID);
			} catch (PartInitException e) {
			}
		});
	}

}
