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
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.NewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class NewCategoryDefinitionWizard extends NewWizard implements IExecutableExtension {

	IPath fInitialPath = null;
	IPath fFilePath = null;
	private IConfigurationElement fConfig;
	private CategoryDefinitionWizardPage fPage;

	@Override
	public void setInitializationData(IConfigurationElement config, String property, Object data) throws CoreException {
		this.fConfig = config;
	}

	@Override
	public void addPages() {
		fPage = new CategoryDefinitionWizardPage("profile", getSelection()); //$NON-NLS-1$
		addPage(fPage);
	}

	@Override
	public boolean performFinish() {
		System.out.println(fConfig);

		try {
			BasicNewProjectResourceWizard.updatePerspective(fConfig);
			final IPath location = fPage.getContainerFullPath();
			final String fileName = fPage.getFileName();
			IRunnableWithProgress op = new NewCategoryCreationOperation(getShell().getDisplay(), location, fileName);
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(PDEUIMessages.NewCategoryDefinitionWizard_title);
		setNeedsProgressMonitor(true);
	}

	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
	}

	public void setInitialPath(IPath path) {
		fInitialPath = path;
	}

	public IPath getFilePath() {
		return fFilePath;
	}

}
