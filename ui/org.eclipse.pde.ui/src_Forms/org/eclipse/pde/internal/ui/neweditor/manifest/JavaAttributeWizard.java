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
package org.eclipse.pde.internal.ui.neweditor.manifest;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.manifest.JavaAttributeValue;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
public class JavaAttributeWizard extends Wizard {
	private String className;
	private IProject project;
	private ISchemaAttribute attInfo;
	private IPluginModelBase model;
	private JavaAttributeWizardPage mainPage;

	private static String STORE_SECTION = "JavaAttributeWizard";
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
		setWindowTitle(PDEPlugin
				.getResourceString("JavaAttributeWizard.wtitle"));
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
		mainPage = new JavaAttributeWizardPage(project, model, attInfo, className);
		addPage(mainPage);
		mainPage.init();
	}

	public boolean performFinish() {
		if (mainPage.getPackageText() != null && mainPage.getPackageText().length() >0)
			className = mainPage.getPackageText() + "." + mainPage.getTypeName();
		else
			className = mainPage.getTypeName();
		IWorkspaceRunnable op = new IWorkspaceRunnable(){
			public void run(IProgressMonitor monitor){
				try {
					mainPage.createType(monitor);
					IResource resource = mainPage.getModifiedResource();
					if (resource!=null){
						selectAndReveal(resource);
						if (project.hasNature(JavaCore.NATURE_ID)){
							IJavaProject jProject = JavaCore.create(project);
							IJavaElement jElement = jProject.findElement(resource.getProjectRelativePath().removeFirstSegments(1));
							if (jElement != null)
								JavaUI.openInEditor(jElement);
						} else {
							if (resource instanceof IFile){
								IWorkbenchPage page = PDEPlugin.getActivePage();
								IDE.openEditor(page, (IFile)resource, true);
							}
						}
					}
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} catch (InterruptedException e) {
					PDEPlugin.logException(e);
				}
			}
		};
		try {
			getContainer().run(false, true, new WorkbenchRunnableAdapter(op, getSchedulingRule()));
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
		}
		return true;
	}
	protected void selectAndReveal(IResource newResource) {
		BasicNewResourceWizard.selectAndReveal(newResource, PDEPlugin.getActiveWorkbenchWindow());
	}
    protected ISchedulingRule getSchedulingRule() {
    	return mainPage.getModifiedResource();
    }

	public String getClassName() {
		return className;
	}
}
