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

import java.io.*;
import java.util.*;

import org.eclipse.ant.internal.ui.launchConfigurations.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public abstract class BaseExportWizard
	extends Wizard
	implements IExportWizard, IPreferenceConstants {

	private IStructuredSelection fSelection;
	protected BaseExportWizardPage fPage1;
	protected AdvancedPluginExportPage fPage2;
	private static final String STORE_SECTION = "PluginExportWizard"; //$NON-NLS-1$

	/**
	 * The constructor.
	 */
	public BaseExportWizard() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		IDialogSettings masterSettings =
			PDEPlugin.getDefault().getDialogSettings();
		setNeedsProgressMonitor(true);
		setDialogSettings(getSettingsSection(masterSettings));
	}

	public void addPages() {
		fPage1 = createPage1();
		fPage2 = createPage2();
		addPage(fPage1);
		addPage(fPage2);
	}

	protected abstract BaseExportWizardPage createPage1();
	
	protected abstract AdvancedPluginExportPage createPage2();
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public IStructuredSelection getSelection() {
		return fSelection;
	}

	public IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings settings = master.getSection(STORE_SECTION);
		if (settings == null) {
			settings = master.addNewSection(STORE_SECTION);
		}
		return settings;
	}

	/**
	 * @see Wizard#init
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.fSelection = selection;
	}
	
	public boolean canFinish() {
		IWizardPage nextPage = fPage1.getNextPage();
		return fPage1.isPageComplete() && (nextPage == null || nextPage.isPageComplete());
	}

	/**
	 * @see Wizard#performFinish
	 */
	public boolean performFinish() {
		fPage1.saveSettings();
		fPage2.saveSettings();
		if (fPage1.doGenerateAntFile()) {
			generateAntBuildFile(fPage1.getAntBuildFileName());
		}
		if (!fPage1.doExportToDirectory()) {
			File zipFile = new File(fPage1.getDestination(), fPage1.getFileName());
			if (zipFile.exists()) {
				if (!MessageDialog.openQuestion(getContainer().getShell(),
						PDEPlugin.getResourceString("BaseExportWizard.confirmReplace.title"),  //$NON-NLS-1$
						PDEPlugin.getFormattedMessage("BaseExportWizard.confirmReplace.desc", //$NON-NLS-1$
								zipFile.getAbsolutePath())))
					return false;
				zipFile.delete();
			}
		}
		scheduleExportJob();
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
			File buildFile = new File(dir, buildFilename);
			PrintWriter writer = new PrintWriter(new FileWriter(buildFile));
			generateAntTask(writer);
			writer.close();
			setDefaultValues(dir, buildFilename);				
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
					launchCopy.doSave();				
				}
			}
		} catch (CoreException e) {
		}		
	}
		
	protected abstract void generateAntTask(PrintWriter writer);
	
	protected abstract void scheduleExportJob();
	
	protected String getExportOperation() {
		return fPage1.doExportToDirectory() ? "directory" : "zip";  //$NON-NLS-1$ //$NON-NLS-2$
	}

}
