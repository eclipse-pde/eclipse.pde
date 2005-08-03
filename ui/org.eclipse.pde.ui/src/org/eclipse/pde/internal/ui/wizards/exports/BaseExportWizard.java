/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.launchConfigurations.AntLaunchShortcut;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;

public abstract class BaseExportWizard
	extends Wizard
	implements IExportWizard, IPreferenceConstants {

	protected IStructuredSelection fSelection;
	protected BaseExportWizardPage fPage1;

	/**
	 * The constructor.
	 */
	public BaseExportWizard() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		IDialogSettings masterSettings =
			PDEPlugin.getDefault().getDialogSettings();
		setNeedsProgressMonitor(true);
		setDialogSettings(getSettingsSection(masterSettings));
		setWindowTitle(PDEUIMessages.BaseExportWizard_wtitle); 
	}

	public void addPages() {
		fPage1 = createPage1();
		addPage(fPage1);
 	}

	protected abstract BaseExportWizardPage createPage1();
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public IStructuredSelection getSelection() {
		return fSelection;
	}

	public IDialogSettings getSettingsSection(IDialogSettings master) {
		String name = getSettingsSectionName();
		IDialogSettings settings = master.getSection(name);
		if (settings == null) {
			settings = master.addNewSection(name);
		}
		return settings;
	}
	
	protected abstract String getSettingsSectionName();

	/**
	 * @see Wizard#init
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		fSelection = selection;
	}
	
	public boolean canFinish() {
		IWizardPage nextPage = fPage1.getNextPage();
		return fPage1.isPageComplete() && (nextPage == null || nextPage.isPageComplete());
	}

	/**
	 * @see Wizard#performFinish
	 */
	public boolean performFinish() {
		saveSettings();
		if (!PlatformUI.getWorkbench().saveAllEditors(true))
			return false;
		
		if (fPage1.doGenerateAntFile())
			generateAntBuildFile(fPage1.getAntBuildFileName());
		
		if (!performPreliminaryChecks())
			return false;
		
		if (!fPage1.doExportToDirectory()) {
			File zipFile = new File(fPage1.getDestination(), fPage1.getFileName());
			if (zipFile.exists()) {
				if (!MessageDialog.openQuestion(getContainer().getShell(),
						PDEUIMessages.BaseExportWizard_confirmReplace_title,  
						NLS.bind(PDEUIMessages.BaseExportWizard_confirmReplace_desc, zipFile.getAbsolutePath())))
					return false;
				zipFile.delete();
			}
		}
		
		scheduleExportJob();
		return true;
	}
	
	private void saveSettings() {
		IWizardPage[] pages = getPages();
		for (int i = 0; i < pages.length; i++) {
			((IExportWizardPage)pages[i]).saveSettings();
		}
	}
	
	protected boolean performPreliminaryChecks() {
		return true;
	}
	
	private void generateAntBuildFile(String filename) {
		String parent = new Path(filename).removeLastSegments(1).toOSString();
		String buildFilename = new Path(filename).lastSegment();
		if (!buildFilename.endsWith(".xml")) //$NON-NLS-1$
			buildFilename += ".xml"; //$NON-NLS-1$
		File dir = new File(new File(parent).getAbsolutePath());
		if (!dir.exists())
			dir.mkdirs();

		try {
			Document task = generateAntTask();
			if (task != null) {
				File buildFile = new File(dir, buildFilename);
				XMLPrintHandler.writeFile(task, buildFile);
				generateAntTask();
				setDefaultValues(dir, buildFilename);
			}
		} catch (IOException e) {
		}
	}
	
	private void setDefaultValues(File dir, String buildFilename) {
		try {
			IContainer container = PDEPlugin.getWorkspace().getRoot().getContainerForLocation(new Path(dir.toString()));
			if (container != null && container.exists()) {
				container.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
				IFile file = container.getFile(new Path(buildFilename));
				List configs = AntLaunchShortcut.findExistingLaunchConfigurations(file);
				ILaunchConfigurationWorkingCopy launchCopy;
				if (configs.size() == 0) {
					ILaunchConfiguration config = AntLaunchShortcut.createDefaultLaunchConfiguration(file);
					launchCopy = config.getWorkingCopy();
				} else {
					launchCopy = ((ILaunchConfiguration) configs.get(0)).getWorkingCopy();
				}
				if (launchCopy != null) {
					launchCopy.setAttribute(
							IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME,
							(String) null);
					launchCopy.setAttribute(
							IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE,
							(String) null);
					launchCopy.setAttribute(
							IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
							(String) null);
					launchCopy.setAttribute(
							IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
							(String) null);
					launchCopy.setAttribute(
							IAntUIConstants.ATTR_DEFAULT_VM_INSTALL,
							(String) null);
					launchCopy.doSave();				
				}
			}
		} catch (CoreException e) {
		}		
	}
		
	protected abstract Document generateAntTask();
	
	protected abstract void scheduleExportJob();
	
	protected String getExportOperation() {
		return fPage1.doExportToDirectory() ? "directory" : "zip";  //$NON-NLS-1$ //$NON-NLS-2$
	}

}
