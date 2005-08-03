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
package org.eclipse.pde.internal.ui.editor.plugin;
import java.lang.reflect.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.wizards.newresource.*;
public class JavaAttributeWizard extends Wizard {
	private String className, classArgs;
	private IProject project;
	private ISchemaAttribute attInfo;
	private IPluginModelBase model;
	private JavaAttributeWizardPage mainPage;
	private static String STORE_SECTION = "JavaAttributeWizard"; //$NON-NLS-1$
	public JavaAttributeWizard(JavaAttributeValue value) {
		this(value.getProject(), value.getModel(), value.getAttributeInfo(),
				value.getClassName());
	}
	public JavaAttributeWizard(IProject project, IPluginModelBase model,
			ISchemaAttribute attInfo, String className) {
		this.className = className;
		this.model = model;
		this.project = project;
		this.attInfo = attInfo;
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
		IDialogSettings masterSettings = PDEPlugin.getDefault()
				.getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setWindowTitle(PDEUIMessages.JavaAttributeWizard_wtitle); 
		setNeedsProgressMonitor(true);
	}
	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}
	public void addPages() {
		mainPage = new JavaAttributeWizardPage(project, model, attInfo,
				className);
		addPage(mainPage);
		mainPage.init();
	}
	public boolean performFinish() {
		if (mainPage.getPackageText() != null
				&& mainPage.getPackageText().length() > 0)
			className = mainPage.getPackageText() + "." //$NON-NLS-1$
					+ mainPage.getTypeName();
		else
			className = mainPage.getTypeName();
		classArgs = mainPage.getClassArgs();
		IRunnableWithProgress op = new WorkspaceModifyOperation(){
			protected void execute(IProgressMonitor monitor)
			throws CoreException, InvocationTargetException,
			InterruptedException {
				mainPage.createType(monitor);
				IResource resource = mainPage.getModifiedResource();
				if (resource != null) {
					selectAndReveal(resource);
					if (project.hasNature(JavaCore.NATURE_ID)) {
						IJavaProject jProject = JavaCore.create(project);
						IJavaElement jElement = jProject.findElement(resource
								.getProjectRelativePath().removeFirstSegments(1));
						if (jElement != null)
							JavaUI.openInEditor(jElement);
					} else if (resource instanceof IFile) {
						IWorkbenchPage page = PDEPlugin.getActivePage();
						IDE.openEditor(page, (IFile) resource, true);
					}
				}
			}
			
		};
		try{
		getContainer().run(false, true, op);
		} catch (InvocationTargetException e){
			PDEPlugin.logException(e);
		} catch (InterruptedException e){
			PDEPlugin.logException(e);
		}
		return true;
	}
	protected void selectAndReveal(IResource newResource) {
		BasicNewResourceWizard.selectAndReveal(newResource, PDEPlugin
				.getActiveWorkbenchWindow());
	}
	protected ISchedulingRule getSchedulingRule() {
		return mainPage.getModifiedResource();
	}
	public String getClassName() {
		return className;
	}
	public String getClassNameWithArgs() {
		if (classArgs != null && classArgs.length() > 0)
			return className + ":" + classArgs; //$NON-NLS-1$
		return getClassName();
	}
}
