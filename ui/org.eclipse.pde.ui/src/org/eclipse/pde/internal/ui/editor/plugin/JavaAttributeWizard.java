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
package org.eclipse.pde.internal.ui.editor.plugin;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class JavaAttributeWizard extends Wizard {

	private static String STORE_SECTION = "JavaAttributeWizard"; //$NON-NLS-1$

	private String fClassName;
	private IProject fProject;
	private ISchemaAttribute fAttInfo;
	private IPluginModelBase fModel;
	protected NewTypeWizardPage fMainPage;

	public JavaAttributeWizard(JavaAttributeValue value) {
		this(value.getProject(), value.getModel(), value.getAttributeInfo(), value.getClassName());
	}

	public JavaAttributeWizard(IProject project, IPluginModelBase model, ISchemaAttribute attInfo, String className) {
		fClassName = className;
		fModel = model;
		fProject = project;
		fAttInfo = attInfo;
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setWindowTitle(PDEUIMessages.JavaAttributeWizard_wtitle);
		setNeedsProgressMonitor(true);
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null)
			setting = master.addNewSection(STORE_SECTION);
		return setting;
	}

	@Override
	public void addPages() {
		fMainPage = new JavaAttributeWizardPage(fProject, fModel, fAttInfo, fClassName);
		addPage(fMainPage);
		((JavaAttributeWizardPage) fMainPage).init();
	}

	@Override
	public boolean performFinish() {
		IRunnableWithProgress op = new WorkspaceModifyOperation() {
			@Override
			protected void execute(IProgressMonitor monitor) throws CoreException, InterruptedException {
				fMainPage.createType(monitor);
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().runInUI(PDEPlugin.getActiveWorkbenchWindow(), op, PDEPlugin.getWorkspace().getRoot());
			IResource resource = fMainPage.getModifiedResource();
			if (resource != null) {
				selectAndReveal(resource);
				if (fProject.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject jProject = JavaCore.create(fProject);
					IJavaElement jElement = jProject.findElement(resource.getProjectRelativePath().removeFirstSegments(1));
					if (jElement != null)
						JavaUI.openInEditor(jElement);
				} else if (resource instanceof IFile) {
					IWorkbenchPage page = PDEPlugin.getActivePage();
					IDE.openEditor(page, (IFile) resource, true);
				}
			}
		} catch (InvocationTargetException | InterruptedException | CoreException e) {
			PDEPlugin.logException(e);
		}
		return true;
	}

	protected void selectAndReveal(IResource newResource) {
		BasicNewResourceWizard.selectAndReveal(newResource, PDEPlugin.getActiveWorkbenchWindow());
	}

	public String getQualifiedName() {
		if (fMainPage.getCreatedType() == null)
			return null;
		return fMainPage.getCreatedType().getFullyQualifiedName('$');
	}

	public String getQualifiedNameWithArgs() {
		String name = getQualifiedName();
		if (name == null)
			return null;
		if (fMainPage instanceof JavaAttributeWizardPage) {
			String classArgs = ((JavaAttributeWizardPage) fMainPage).getClassArgs();
			if (classArgs != null && classArgs.length() > 0)
				return name + ':' + classArgs;
		}
		return name;
	}
}
